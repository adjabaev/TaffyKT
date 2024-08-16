package be.arby.taffy.compute.grid

import be.arby.taffy.compute.common.applyAlignmentFallback
import be.arby.taffy.compute.common.computeAlignmentOffset
import be.arby.taffy.compute.common.computeContentSizeContribution
import be.arby.taffy.compute.grid.types.GridTrack
import be.arby.taffy.geom.*
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.collections.enumerate
import be.arby.taffy.lang.collections.skip
import be.arby.taffy.lang.collections.stepBy
import be.arby.taffy.lang.f32Max
import be.arby.taffy.lang.tuples.T2
import be.arby.taffy.lang.tuples.T3
import be.arby.taffy.style.BoxSizing
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.Position
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.tree.layout.SizingMode
import be.arby.taffy.tree.traits.LayoutGridContainer
import be.arby.taffy.util.*

/**
 * Align the grid tracks within the grid according to the align-content (rows) or
 * justify-content (columns) property. This only does anything if the size of the
 * grid is not equal to the size of the grid container in the axis being aligned.
 */
fun alignTracks(
    gridContainerContentBoxSize: Float,
    padding: Line<Float>,
    border: Line<Float>,
    tracks: MutableList<GridTrack>,
    trackAlignmentStyle: AlignContent,
) {
    val usedSize: Float = tracks.map { track -> track.baseSize }.sum()
    val freeSpace = gridContainerContentBoxSize - usedSize
    val origin = padding.start + border.start

    // Count the number of non-collapsed tracks (not counting gutters)
    val numTracks = tracks
        .asSequence()
        .drop(1)
        .filterIndexed { index, _ -> index % 2 == 0 }
        .filter { track -> !track.isCollapsed }
        .count()

    // Grid layout treats gaps as full tracks rather than applying them at alignment so we
    // simply pass zero here. Grid layout is never reversed.
    val gap = 0f
    val layoutIsReversed = false
    val isSafe = false; // TODO: Implement safe alignment
    val trackAlignment = applyAlignmentFallback(freeSpace, numTracks, trackAlignmentStyle, isSafe)

    // Compute offsets
    var totalOffset = origin
    tracks.enumerate().forEach { (i, track) ->
        // Odd tracks are gutters (but slices are zero-indexed, so odd tracks have even indices)
        val isGutter = i % 2 == 0

        // The first non-gutter track is index 1
        val isFirst = i == 1

        val offset = if (isGutter) {
            0f
        } else {
            computeAlignmentOffset(freeSpace, numTracks, gap, trackAlignment, layoutIsReversed, isFirst)
        }

        track.offset = totalOffset + offset
        totalOffset += offset + track.baseSize
    }
}

/// Align and size a grid item into it's final position
fun alignAndPositionItem(
    tree: LayoutGridContainer,
    node: Int,
    order: Int,
    gridArea: Rect<Float>,
    containerAlignmentStyles: InBothAbsAxis<Option<AlignItems>>,
    baselineShim: Float,
): T3<Size<Float>, Float, Float> {
    val gridAreaSize = Size(width = gridArea.right - gridArea.left, height = gridArea.bottom - gridArea.top)

    val style = tree.getGridChildStyle(node)

    val overflow = style.overflow()
    val scrollbarWidth = style.scrollbarWidth()
    val aspectRatio = style.aspectRatio()
    val justifySelf = style.justifySelf()
    val alignSelf = style.alignSelf()

    val position = style.position()
    val insetHorizontal =
        style.inset().horizontalComponents().map { size -> size.resolveToOption(gridAreaSize.width) }
    val insetVertical = style.inset().verticalComponents().map { size -> size.resolveToOption(gridAreaSize.height) }
    val padding = style.padding().map { p -> p.resolveOrZero(Option.Some(gridAreaSize.width)) }
    val border = style.border().map { p -> p.resolveOrZero(Option.Some(gridAreaSize.width)) }
    val paddingBorderSize = (padding + border).sumAxes()

    val boxSizingAdjustment =
        if (style.boxSizing() == BoxSizing.CONTENT_BOX) paddingBorderSize else Size.ZERO.clone()

    val inherentSize = style
        .size()
        .maybeResolve(gridAreaSize)
        .maybeApplyAspectRatio(aspectRatio)
        .maybeAdd(boxSizingAdjustment)
    val minSize = style
        .minSize()
        .maybeResolve(gridAreaSize)
        .maybeAdd(boxSizingAdjustment)
        .or(paddingBorderSize.map { v -> Option.Some(v) })
        .maybeMax(paddingBorderSize)
        .maybeApplyAspectRatio(aspectRatio)
    val maxSize = style
        .maxSize()
        .maybeResolve(gridAreaSize)
        .maybeApplyAspectRatio(aspectRatio)
        .maybeAdd(boxSizingAdjustment)

    // Resolve default alignment styles if they are set on neither the parent or the node itself
    // Note: if the child has a preferred aspect ratio but neither width or height are set, then the width is stretched
    // and the then height is calculated from the width according the aspect ratio
    // See: https://www.w3.org/TR/css-grid-1/#grid-item-sizing
    val alignmentStyles = InBothAbsAxis(
        horizontal = justifySelf.or(containerAlignmentStyles.horizontal).unwrapOrElse {
            if (inherentSize.width.isSome()) {
                AlignSelf.START
            } else {
                AlignSelf.STRETCH
            }
        },
        vertical = alignSelf.or(containerAlignmentStyles.vertical).unwrapOrElse {
            if (inherentSize.height.isSome() || aspectRatio.isSome()) {
                AlignSelf.START
            } else {
                AlignSelf.STRETCH
            }
        }
    )

    // Note: This is not a bug. It is part of the CSS spec that both horizontal and vertical margins
    // resolve against the WIDTH of the grid area.
    val margin = style.margin().map { margin -> margin.resolveToOption(gridAreaSize.width) }

    val gridAreaMinusItemMarginsSize = Size(
        width = gridAreaSize.width.maybeSub(margin.left).maybeSub(margin.right),
        height = gridAreaSize.height.maybeSub(margin.top).maybeSub(margin.bottom) - baselineShim,
    )

    // If node is absolutely positioned and width is not set explicitly, then deduce it
    // from left, right and container_content_box if both are set.
    val wdt = inherentSize.width.orElse {
        // Apply width derived from both the left and right properties of an absolutely
        // positioned element being set
        if (position == Position.ABSOLUTE) {
            val (left, right) = insetHorizontal.t2()
            if (left.isSome() && right.isSome()) {
                return@orElse Option.Some(f32Max(gridAreaMinusItemMarginsSize.width - left.unwrap() - right.unwrap(), 0f))
            }
        }

        // Apply width based on stretch alignment if:
        //  - Alignment style is "stretch"
        //  - The node is not absolutely positioned
        //  - The node does not have auto margins in this axis.
        if (margin.left.isSome()
            && margin.right.isSome()
            && alignmentStyles.horizontal == AlignSelf.STRETCH
            && position != Position.ABSOLUTE
        ) {
            return@orElse Option.Some(gridAreaMinusItemMarginsSize.width)
        }

        Option.None
    }

    // Reapply aspect ratio after stretch and absolute position width adjustments
    val (width, height) = Size(wdt, height = inherentSize.height).maybeApplyAspectRatio(aspectRatio)

    val hgt = height.orElse {
        if (position == Position.ABSOLUTE) {
            val (top, bottom) = insetHorizontal.t2()
            if (top.isSome() && bottom.isSome()) {
                return@orElse Option.Some(f32Max(gridAreaMinusItemMarginsSize.height - top.unwrap() - bottom.unwrap(), 0f))
            }
        }

        // Apply height based on stretch alignment if:
        //  - Alignment style is "stretch"
        //  - The node is not absolutely positioned
        //  - The node does not have auto margins in this axis.
        if (margin.top.isSome()
            && margin.bottom.isSome()
            && alignmentStyles.vertical == AlignSelf.STRETCH
            && position != Position.ABSOLUTE
        ) {
            return@orElse Option.Some(gridAreaMinusItemMarginsSize.height)
        }

        Option.None
    }
    // Reapply aspect ratio after stretch and absolute position height adjustments
    val (width2, height2) = Size(width, hgt).maybeApplyAspectRatio(aspectRatio)

    // Clamp size by min and max width/height
    val (width3, height3) = Size(width2, height2).maybeClamp(minSize, maxSize)

    // Layout node
    val layoutOutput = tree.performChildLayout(
        node,
        Size(width3, height3),
        gridAreaSize.map { v -> Option.Some(v) },
        gridAreaMinusItemMarginsSize.map { v -> AvailableSpace.Definite(v) },
        SizingMode.INHERENT_SIZE,
        Line.FALSE
    )

    // Resolve final size
    val (width4, height4) = Size(width3, height3).unwrapOr(layoutOutput.size).maybeClamp(minSize, maxSize)

    val (x, xMargin) = alignItemWithinArea(
        Line(start = gridArea.left, end = gridArea.right),
        justifySelf.unwrapOr(alignmentStyles.horizontal),
        width4,
        position,
        insetHorizontal,
        margin.horizontalComponents(),
        0f
    )
    val (y, yMargin) = alignItemWithinArea(
        Line(start = gridArea.top, end = gridArea.bottom),
        alignSelf.unwrapOr(alignmentStyles.vertical),
        height4,
        position,
        insetVertical,
        margin.verticalComponents(),
        baselineShim
    )

    val scrollbarSize = Size(
        width = if (overflow.y == Overflow.SCROLL) scrollbarWidth else 0f,
        height = if (overflow.x == Overflow.SCROLL) scrollbarWidth else 0f,
    )

    val resolvedMargin = Rect(left = xMargin.start, right = xMargin.end, top = yMargin.start, bottom = yMargin.end)

    tree.setUnroundedLayout(
        node,
        Layout(
            order,
            location = Point(x, y),
            size = Size(width4, height4),
            contentSize = layoutOutput.contentSize,
            scrollbarSize,
            padding,
            border,
            margin = resolvedMargin
        )
    )

    val contribution =
        computeContentSizeContribution(Point(x, y), Size(width4, height4), layoutOutput.contentSize, overflow)

    return T3(contribution, y, height4)
}

/// Align and size a grid item along a single axis
fun alignItemWithinArea(
    gridArea: Line<Float>,
    alignmentStyle: AlignSelf,
    resolvedSize: Float,
    position: Position,
    inset: Line<Option<Float>>,
    margin: Line<Option<Float>>,
    baselineShim: Float,
): T2<Float, Line<Float>> {
    // Calculate grid area dimension in the axis
    val nonAutoMargin = Line(start = margin.start.unwrapOr(0f) + baselineShim, end = margin.end.unwrapOr(0f))
    val gridAreaSize = f32Max(gridArea.end - gridArea.start, 0f)
    val freeSpace = f32Max(gridAreaSize - resolvedSize - nonAutoMargin.sum(), 0f)

    // Expand auto margins to fill available space
    val autoMarginCount = margin.start.isNone().toInt() + margin.end.isNone().toInt()
    val autoMarginSize = if (autoMarginCount > 0) freeSpace / autoMarginCount.toFloat() else 0f
    val resolvedMargin = Line(
        start = margin.start.unwrapOr(autoMarginSize) + baselineShim,
        end = margin.end.unwrapOr(autoMarginSize),
    )

    // Compute offset in the axis
    val alignmentBasedOffset = when (alignmentStyle) {
        AlignSelf.START, AlignSelf.FLEX_START -> resolvedMargin.start
        AlignSelf.END, AlignSelf.FLEX_END -> gridAreaSize - resolvedSize - resolvedMargin.end
        AlignSelf.CENTER -> (gridAreaSize - resolvedSize + resolvedMargin.start - resolvedMargin.end) / 2f
        // TODO: Add support for baseline alignment. For now we treat it as "start".
        AlignSelf.BASELINE -> resolvedMargin.start
        AlignSelf.STRETCH -> resolvedMargin.start
    }

    val offsetWithinArea = if (position == Position.ABSOLUTE) {
        if (inset.start.isSome()) {
            inset.start.unwrap() + nonAutoMargin.start
        } else if (inset.end.isSome()) {
            gridAreaSize - inset.end.unwrap() - resolvedSize - nonAutoMargin.end
        } else {
            alignmentBasedOffset
        }
    } else {
        alignmentBasedOffset
    }

    var start = gridArea.start + offsetWithinArea
    if (position == Position.RELATIVE) {
        start += inset.start.or(inset.end.map { pos -> -pos }).unwrapOr(0f)
    }

    return T2(start, resolvedMargin)
}
