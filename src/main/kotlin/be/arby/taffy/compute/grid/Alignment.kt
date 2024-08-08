package be.arby.taffy.compute.grid

import be.arby.taffy.compute.GenericAlgorithm
import be.arby.taffy.compute.common.Alignment
import be.arby.taffy.compute.grid.types.GridTrack
import be.arby.taffy.geometry.Line
import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.geometry.extensions.maybeApplyAspectRatio
import be.arby.taffy.geometry.extensions.sum
import be.arby.taffy.geometry.extensions.unwrapOr
import be.arby.taffy.lang.Option
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.layout.SizingMode
import be.arby.taffy.geom.InBothAbsAxis
import be.arby.taffy.lang.*
import be.arby.taffy.node.Node
import be.arby.taffy.resolve.maybeResolveStSd
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.Position
import be.arby.taffy.util.maybeSub
import be.arby.taffy.utils.f32Max
import be.arby.taffy.utils.f32Min
import be.arby.taffy.utils.toInt
import java.util.*

class Alignment {

    companion object {
        fun alignTracks(
            gridContainerContentBoxSize: Float,
            padding: Line<Float>,
            border: Line<Float>,
            tracks: ArrayList<GridTrack>,
            trackAlignmentStyle: AlignContent
        ) {
            val usedSize = tracks.map { track -> track.baseSize }.sum()
            val sizeDiff = gridContainerContentBoxSize - usedSize
            val freeSpace = f32Max(sizeDiff, 0f)
            val overflow = f32Min(sizeDiff, 0f)

            val origin = padding.start + border.start + when (trackAlignmentStyle) {
                AlignContent.START -> 0f
                AlignContent.FLEX_START -> 0f
                AlignContent.END -> overflow
                AlignContent.FLEX_END -> overflow
                AlignContent.CENTER -> overflow / 2f
                AlignContent.STRETCH -> 0f
                AlignContent.SPACE_BETWEEN -> 0f
                AlignContent.SPACE_EVENLY -> 0f
                AlignContent.SPACE_AROUND -> 0f
            }

            val numTracks = tracks.toList().drop(1).filterIndexed { index, _ -> index % 2 == 0 }
                .count { track -> !track.isCollapsed }

            val gap = 0f
            val layoutIsReversed = false

            var totalOffset = origin
            tracks.forEachIndexed { i, track ->
                // Odd tracks are gutters (but slices are zero-indexed, so odd tracks have even indicies)
                val isGutter = i % 2 == 0

                // The first non-gutter track is index 1
                val isFirst = i == 1

                val offset = if (isGutter) {
                    0f
                } else {
                    Alignment.computeAlignmentOffset(
                        freeSpace,
                        numTracks,
                        gap,
                        trackAlignmentStyle,
                        layoutIsReversed,
                        isFirst
                    )
                }

                track.offset = totalOffset + offset
                totalOffset += offset + track.baseSize
            }
        }

        fun alignAndPositionItem(
            tree: LayoutTree,
            node: Node,
            order: Int,
            gridArea: Rect<Float>,
            containerAlignmentStyles: InBothAbsAxis<Option<AlignItems>>,
            baselineShim: Float
        ) {
            val gridAreaSize = Size(width = gridArea.right - gridArea.left, height = gridArea.bottom - gridArea.top)

            val style = tree.style(node)
            val aspectRatio = style.aspectRatio
            val justifySelf = style.justifySelf
            val alignSelf = style.alignSelf

            val position = style.position
            val insetHorizontal =
                style.inset.horizontalComponents().map { size -> size.resolveToOption(gridAreaSize.width) }
            val insetVertical =
                style.inset.verticalComponents().map { size -> size.resolveToOption(gridAreaSize.height) }
            val inherentSize = style.size.maybeResolveStSd(gridAreaSize).maybeApplyAspectRatio(aspectRatio)
            val minSize = style.minSize.maybeResolveStSd(gridAreaSize).maybeApplyAspectRatio(aspectRatio)
            val maxSize = style.maxSize.maybeResolveStSd(gridAreaSize).maybeApplyAspectRatio(aspectRatio)

            // Resolve default alignment styles if they are set on neither the parent or the node itself
            // Note: if the child has a preferred aspect ratio but neither width or height are set, then the width is stretched
            // and the then height is calculated from the width according the aspect ratio
            // See: https://www.w3.org/TR/css-grid-1/#grid-item-sizing
            val alignmentStyles = InBothAbsAxis(
                horizontal = containerAlignmentStyles.horizontal.orElse { justifySelf }.unwrapOr(
                    if (inherentSize.width.isSome()) {
                        AlignSelf.START
                    } else {
                        AlignSelf.STRETCH
                    }
                ),
                vertical = containerAlignmentStyles.vertical.orElse { alignSelf }.unwrapOr(
                    if (inherentSize.height.isSome() || aspectRatio.isSome()) {
                        AlignSelf.START
                    } else {
                        AlignSelf.STRETCH
                    }
                )
            )

            // Note: This is not a bug. It is part of the CSS spec that both horizontal and vertical margins
            // resolve against the WIDTH of the grid area.
            val margin = style.margin.map { margin -> margin.resolveToOption(gridAreaSize.width) }
            val gridAreaMinusItemMarginsSize = Size(
                width = gridAreaSize.width.maybeSub(margin.left).maybeSub(margin.right),
                height = gridAreaSize.height.maybeSub(margin.top).maybeSub(margin.bottom) - baselineShim
            )

            // Apply width derived from both the left and right properties of an absolutely
            // positioned element being set


            var iStart = insetHorizontal.start
            var iEnd = insetHorizontal.end
            val altWidth = if (position == Position.ABSOLUTE && iStart.isSome() && iEnd.isSome()) {
                Option.Some(f32Max(gridAreaMinusItemMarginsSize.width - iStart.unwrap() - iEnd.unwrap(), 0f))
            } else if (margin.left.isSome() && margin.right.isSome() && alignmentStyles.horizontal == AlignSelf.STRETCH
                && position != Position.ABSOLUTE
            ) {
                // Apply width based on stretch alignment if:
                //  - Alignment style is "stretch"
                //  - The node is not absolutely positioned
                //  - The node does not have auto margins in this axis.
                Option.Some(gridAreaMinusItemMarginsSize.width)
            } else {
                Option.None
            }

            // If node is absolutely positioned and width is not set explicitly, then deduce it
            // from left, right and container_content_box if both are set.
            var w: Option<Float> = inherentSize.width.orElse { altWidth }

            // Reapply aspect ratio after stretch and absolute position width adjustments
            val sz2 = Size(w, height = inherentSize.height).maybeApplyAspectRatio(aspectRatio)
            w = sz2.width
            var h: Option<Float> = sz2.height

            iStart = insetVertical.start
            iEnd = insetVertical.end
            val altHeight = if (position == Position.ABSOLUTE && iStart.isSome() && iEnd.isSome()) {
                Option.Some(f32Max(gridAreaMinusItemMarginsSize.height - iStart.unwrap() - iEnd.unwrap(), 0f))
            } else if (margin.top.isSome() && margin.bottom.isSome() && alignmentStyles.vertical == AlignSelf.STRETCH
                && position != Position.ABSOLUTE
            ) {
                // Apply height based on stretch alignment if:
                //  - Alignment style is "stretch"
                //  - The node is not absolutely positioned
                //  - The node does not have auto margins in this axis.
                Option.Some(gridAreaMinusItemMarginsSize.height)
            } else {
                Option.None
            }

            h = h.orElse { altHeight }

            // Reapply aspect ratio after stretch and absolute position adjustments
            var sz = Size(w, h).maybeApplyAspectRatio(aspectRatio)
            w = sz.width
            h = sz.height

            // Clamp size by min and max width/height
            sz = Size(w, h).maybeClamp(minSize, maxSize)
            w = sz.width
            h = sz.height

            // Layout node
            val measuredSizeAndBaselines = GenericAlgorithm.performLayout(
                tree,
                node,
                Size(w, h),
                gridAreaSize.map { v -> Option.Some(v) },
                gridAreaMinusItemMarginsSize.map { f -> AvailableSpace.Definite(f) },
                SizingMode.INHERENT_SIZE
            )

            // Resolve final size
            val sf = Size(w, h).unwrapOr(measuredSizeAndBaselines.size).maybeClamp(minSize, maxSize)
            val width = sf.width
            val height = sf.height

            val x = alignItemWithinArea(
                Line(start = gridArea.left, end = gridArea.right),
                justifySelf.unwrapOr(alignmentStyles.horizontal),
                width,
                position,
                insetHorizontal,
                margin.horizontalComponents(),
                0.0f
            )
            val y = alignItemWithinArea(
                Line(start = gridArea.top, end = gridArea.bottom),
                alignSelf.unwrapOr(alignmentStyles.vertical),
                height,
                position,
                insetVertical,
                margin.verticalComponents(),
                baselineShim
            )

            tree.layout(node, Layout(order, size = Size(width, height), location = Point(x, y)))
        }

        fun alignItemWithinArea(
            gridArea: Line<Float>,
            alignmentStyle: AlignSelf,
            resolvedSize: Float,
            position: Position,
            inset: Line<Option<Float>>,
            margin: Line<Option<Float>>,
            baselineShim: Float
        ): Float {
            // Calculate grid area dimension in the axis
            val nonAutoMargin = Line(start = margin.start.unwrapOr(0.0f) + baselineShim, end = margin.end.unwrapOr(0.0f))
            val gridAreaSize = f32Max(gridArea.end - gridArea.start, 0.0f)
            val freeSpace = f32Max(gridAreaSize - resolvedSize - nonAutoMargin.sum(), 0.0f)

            // Expand auto margins to fill available space
            val autoMarginCount = margin.start.isNone().toInt() + margin.end.isNone().toInt()
            val autoMarginSize = if (autoMarginCount > 0) {
                freeSpace / autoMarginCount.toFloat()
            } else {
                0.0f
            }
            val resolvedMargin = Line(
                start = margin.start.unwrapOr(autoMarginSize) + baselineShim,
                end = margin.end.unwrapOr(autoMarginSize)
            )

            // Compute offset in the axis
            val alignmentBasedOffset = when (alignmentStyle) {
                AlignSelf.START, AlignSelf.FLEX_START -> resolvedMargin.start
                AlignSelf.END, AlignSelf.FLEX_END -> gridAreaSize - resolvedSize - resolvedMargin.end
                AlignSelf.CENTER -> (gridAreaSize - resolvedSize + resolvedMargin.start - resolvedMargin.end) / 2.0f
                // TODO: Add support for baseline alignment. For now we treat it as "start".
                AlignSelf.BASELINE -> resolvedMargin.start
                AlignSelf.STRETCH -> resolvedMargin.start
            }

            val offsetWithinArea = if (position == Position.ABSOLUTE) {
                val start = inset.start
                val end = inset.end
                if (start.isSome()) {
                    start.unwrap() + nonAutoMargin.start
                } else if (end.isSome()) {
                    gridAreaSize - end.unwrap() - resolvedSize - nonAutoMargin.end
                } else {
                    alignmentBasedOffset
                }
            } else {
                alignmentBasedOffset
            }

            var start = gridArea.start + offsetWithinArea
            if (position == Position.RELATIVE) {
                start += inset.start.orElse { inset.end.map { pos -> -pos } }.unwrapOr(0.0f)
            }

            return start
        }
    }
}
