package be.arby.taffy.compute

import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Size
import be.arby.taffy.geometry.extensions.*
import be.arby.taffy.lang.*
import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.layout.SizeAndBaselines
import be.arby.taffy.layout.SizingMode
import be.arby.taffy.node.Node
import be.arby.taffy.resolve.maybeResolveStS
import be.arby.taffy.resolve.resolveOrZeroOtRlp
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.util.maybeClamp
import be.arby.taffy.utils.f32Max
import be.arby.taffy.utils.tuples.Quadruple

class Leaf {
    companion object {
        @JvmStatic
        fun compute(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            sizingMode: SizingMode
        ): SizeAndBaselines {
            val style = tree.style(node)

            // Resolve node's preferred/min/max sizes (width/heights) against the available space (percentages resolve to pixel values)
            // For ContentSize mode, we pretend that the node has no size styles as these should be ignored.
            val (nodeSize, nodeMinSize, nodeMaxSize, aspectRatio) = when (sizingMode) {
                SizingMode.CONTENT_SIZE -> {
                    val nodeSize = knownDimensions
                    val nodeMinSize = Size.none()
                    val nodeMaxSize = Size.none()
                    Quadruple(nodeSize, nodeMinSize, nodeMaxSize, Option.None)
                }

                SizingMode.INHERENT_SIZE -> {
                    val aspectRatio = style.aspectRatio;
                    val styleSize = style.size.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)
                    val styleMinSize = style.minSize.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)
                    val styleMaxSize = style.maxSize.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)
                    val nodeSize = knownDimensions.or(styleSize)
                    Quadruple(nodeSize, styleMinSize, styleMaxSize, aspectRatio)
                }
            }

            // Return early if both width and height are known
            if (nodeSize.width.isSome() && nodeSize.height.isSome()) {
                val size = Size(nodeSize.width.unwrap(), nodeSize.height.unwrap()).maybeClamp(nodeMinSize, nodeMaxSize)
                return SizeAndBaselines(size, firstBaselines = Point.NONE)
            }

            if (tree.needsMeasure(node)) {
                // Compute available space
                val availableSpace =
                    Size(width = availableSpace.width.maybeSet(nodeSize.width).maybeSet(nodeMaxSize.width)
                        .mapDefiniteValue { size -> size.maybeClamp(nodeMinSize.width, nodeMaxSize.width) },
                        height = availableSpace.height.maybeSet(nodeSize.height).maybeSet(nodeMaxSize.height)
                            .mapDefiniteValue { size -> size.maybeClamp(nodeMinSize.height, nodeMaxSize.height) })

                // Measure node
                val ms = tree.measureNode(node, knownDimensions, availableSpace)
                val measuredSize = Size(
                    width = ms.width,
                    height = f32Max(ms.height, aspectRatio.map { ratio -> ms.width / ratio }.unwrapOr(0.0f)),
                )

                val size = nodeSize.unwrapOr(measuredSize).maybeClamp(nodeMinSize, nodeMaxSize)
                return SizeAndBaselines(size, firstBaselines = Point.NONE)
            }

            // Note: both horizontal and vertical percentage padding/borders are resolved against the container's inline size (i.e. width).
            // This is not a bug, but is how CSS is specified (see: https://developer.mozilla.org/en-US/docs/Web/CSS/padding#values)
            val padding = style.padding.resolveOrZeroOtRlp(parentSize.width)
            val border = style.border.resolveOrZeroOtRlp(parentSize.width)

            val size = Size(
                width = nodeSize.width
                    // .unwrap_or(0.0) + padding.horizontal_axis_sum() + border.horizontal_axis_sum(), // content-box
                    .unwrapOr(0.0f + padding.horizontalAxisSum() + border.horizontalAxisSum()) // border-box
                    .maybeClamp(nodeMinSize.width, nodeMaxSize.width),
                height = nodeSize.height
                    // .unwrap_or(0.0) + padding.vertical_axis_sum() + border.vertical_axis_sum(), // content-box
                    .unwrapOr(0.0f + padding.verticalAxisSum() + border.verticalAxisSum()) // border-box
                    .maybeClamp(nodeMinSize.height, nodeMaxSize.height),
            )

            return SizeAndBaselines(size, firstBaselines = Point.NONE)
        }
    }
}
