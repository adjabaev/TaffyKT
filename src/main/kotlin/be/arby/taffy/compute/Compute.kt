package be.arby.taffy.compute

import be.arby.taffy.compute.flexbox.FlexboxAlgorithm
import be.arby.taffy.compute.grid.CssGridAlgorithm
import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Size
import be.arby.taffy.geometry.extensions.intoOptions
import be.arby.taffy.lang.Option
import be.arby.taffy.layout.*
import be.arby.taffy.node.Node
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.Display
import be.arby.taffy.style.Overflow
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.tree.layout.RunMode
import be.arby.taffy.utils.toInt
import org.apache.commons.math3.util.Precision

class Compute {
    companion object {
        @JvmStatic
        fun computeLayout(tree: LayoutTree, root: Node, availableSpace: Size<AvailableSpace>) {
            // Recursively compute node layout
            val sizeAndBaselines = GenericAlgorithm.performLayout(
                tree,
                root,
                Size.none(),
                availableSpace.intoOptions(),
                availableSpace,
                SizingMode.INHERENT_SIZE
            )

            val layout = Layout(order = 0, size = sizeAndBaselines.size, location = Point.ZERO)
            tree.layout(root, layout)

            // Recursively round the layout's of this node and all children
            roundLayout(tree, root)
        }

        /**
         * Updates the stored layout of the provided `node` and its children
         */
        @JvmStatic
        fun computeNodeLayout(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            runMode: RunMode,
            sizingMode: SizingMode
        ): SizeAndBaselines {
            // First we check if we have a cached result for the given input
            val cacheRunMode = if (tree.isChildless(node)) {
                RunMode.PERFORM_LAYOUT
            } else {
                runMode
            }

            val avs: Size<AvailableSpace> = Size(availableSpace.width, availableSpace.height)
            val style = node.data.style
            if (style.overflowX == Overflow.SCROLL) {
                avs.width = AvailableSpace.MaxContent
            }
            if (style.overflowY == Overflow.SCROLL) {
                avs.height = AvailableSpace.MaxContent
            }

            val cfc = computeFromCache(tree, node, knownDimensions, avs, cacheRunMode, sizingMode);
            if (cfc.isSome()) {
                return cfc.unwrap()
            }

            val computedSizeAndBaselines = if (tree.isChildless(node)) {
                when (runMode) {
                    RunMode.PERFORM_LAYOUT -> {
                        LeafAlgorithm.performLayout(tree, node, knownDimensions, parentSize, avs, sizingMode)
                    }
                    RunMode.COMPUTE_SIZE -> {
                        val size = LeafAlgorithm.measureSize(
                            tree,
                            node,
                            knownDimensions,
                            parentSize,
                            avs,
                            sizingMode
                        )
                        SizeAndBaselines(size, firstBaselines = Point.NONE)
                    }
                }
            } else {
                when (tree.style(node).display) {
                    Display.FLEX -> {
                        when (runMode) {
                            RunMode.PERFORM_LAYOUT -> {
                                FlexboxAlgorithm.performLayout(
                                    tree,
                                    node,
                                    knownDimensions,
                                    parentSize,
                                    avs,
                                    sizingMode
                                )
                            }
                            RunMode.COMPUTE_SIZE -> {
                                val size = FlexboxAlgorithm.measureSize(
                                    tree,
                                    node,
                                    knownDimensions,
                                    parentSize,
                                    avs,
                                    sizingMode
                                )
                                SizeAndBaselines(size, firstBaselines = Point.NONE)
                            }
                        }
                    }
                    Display.GRID -> {
                        when (runMode) {
                            RunMode.PERFORM_LAYOUT -> {
                                CssGridAlgorithm.performLayout(
                                    tree,
                                    node,
                                    knownDimensions,
                                    parentSize,
                                    avs,
                                    sizingMode
                                )
                            }
                            RunMode.COMPUTE_SIZE -> {
                                val size = CssGridAlgorithm.measureSize(
                                    tree,
                                    node,
                                    knownDimensions,
                                    parentSize,
                                    avs,
                                    sizingMode
                                )
                                SizeAndBaselines(size, firstBaselines = Point.NONE)
                            }
                        }
                    }
                    Display.NONE -> {
                        when (runMode) {
                            RunMode.PERFORM_LAYOUT -> {
                                HiddenAlgorithm.performLayout(
                                    tree,
                                    node,
                                    knownDimensions,
                                    parentSize,
                                    avs,
                                    sizingMode
                                )
                            }
                            RunMode.COMPUTE_SIZE -> {
                                val size = HiddenAlgorithm.measureSize(
                                    tree,
                                    node,
                                    knownDimensions,
                                    parentSize,
                                    avs,
                                    sizingMode
                                )
                                SizeAndBaselines(size, firstBaselines = Point.NONE)
                            }
                        }
                    }
                }
            }

            // Cache result
            val cacheSlot = computeCacheSlot(knownDimensions, avs)
            tree.cache(
                node, cacheSlot, Cache(
                    knownDimensions = knownDimensions,
                    availableSpace = avs,
                    runMode = cacheRunMode,
                    cachedSizeAndBaselines = computedSizeAndBaselines
                )
            )

            return computedSizeAndBaselines
        }

        private fun computeCacheSlot(
            knownDimensions: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>
        ): Int {
            val hasKnownWidth = knownDimensions.width.isSome()
            val hasKnownHeight = knownDimensions.height.isSome()

            // Slot 0: Both knownDimensions were set
            if (hasKnownWidth && hasKnownHeight) {
                return 0;
            }

            // Slot 1: width but not height known_dimension was set and the other dimension was either a MaxContent or
            // Definite available space constraint
            // Slot 2: width but not height known_dimension was set and the other dimension was a MinContent constraint
            if (hasKnownWidth && !hasKnownHeight) {
                return 1 + (availableSpace.height is AvailableSpace.MinContent).toInt()
            }

            // Slot 3: height but not width known_dimension was set and the other dimension was either a MaxContent or Definite available space constraint
            // Slot 4: height but not width known_dimension was set and the other dimension was a MinContent constraint
            if (!hasKnownWidth && hasKnownHeight) {
                return 3 + (availableSpace.width is AvailableSpace.MinContent).toInt()
            }

            // Slot 5: Neither known_dimensions were set, and we are sizing under a MaxContent or Definite available space constraint
            // Slot 6: Neither known_dimensions were set, and we are sizing under a MinContent constraint
            return 5 + (availableSpace.width is AvailableSpace.MinContent).toInt()
        }

        private fun computeFromCache(
            tree: LayoutTree, node: Node, knownDimensions: Size<Option<Float>>, availableSpace: Size<AvailableSpace>,
            runMode: RunMode, sizingMode: SizingMode
        ): Option<SizeAndBaselines> {
            for (idx in 0 until 7) {
                val entry = tree.cache(node, idx)
                if (entry.isSome()) {
                    val cache = entry.unwrap()
                    // Cached ComputeSize results are not valid if we are running in PerformLayout mode
                    if (cache.runMode == RunMode.COMPUTE_SIZE && runMode == RunMode.PERFORM_LAYOUT) {
                        return Option.None;
                    }

                    val cachedSize = cache.cachedSizeAndBaselines.size

                    if ((knownDimensions.width == cache.knownDimensions.width || knownDimensions.width == Option.Some(
                            cachedSize.width
                        ))
                        && (knownDimensions.height == cache.knownDimensions.height
                                || knownDimensions.height == Option.Some(cachedSize.height))
                        && (knownDimensions.width.isSome()
                                || cache.availableSpace.width.isRoughlyEqual(availableSpace.width)
                                || (sizingMode == SizingMode.CONTENT_SIZE
                                && availableSpace.width.isDefinite()
                                && availableSpace.width.unwrap() >= cachedSize.width))
                        && (knownDimensions.height.isSome()
                                || cache.availableSpace.height.isRoughlyEqual(availableSpace.height)
                                || (sizingMode == SizingMode.CONTENT_SIZE
                                && availableSpace.height.isDefinite()
                                && availableSpace.height.unwrap() >= cachedSize.height))
                    ) {
                        return Option.Some(cache.cachedSizeAndBaselines)
                    }
                }
            }

            return Option.None
        }

        fun performHiddenLayout(tree: LayoutTree, node: Node) {
            /// Recursive function to apply hidden layout to all descendents
            fun performHiddenLayoutInner(tree: LayoutTree, node: Node, order: Int) {
                tree.layout(node, Layout.withOrder(order))
                for (order in 0 until tree.childCount(node)) {
                    performHiddenLayoutInner(tree, tree.child(node, order), order)
                }
            }

            for (order in 0 until tree.childCount(node)) {
                performHiddenLayoutInner(tree, tree.child(node, order), order)
            }
        }

        private fun roundLayout(tree: LayoutTree, root: Node) {
            val layout = tree.layout(root);

            layout.location.x = Precision.round(layout.location.x, 0)
            layout.location.y = Precision.round(layout.location.y, 0)

            layout.size.width = Precision.round(layout.size.width, 0)
            layout.size.height = Precision.round(layout.size.height, 0)

            // Satisfy the borrow checker here by re-indexing to shorten the lifetime to the loop scope
            for (x in 0 until tree.childCount(root)) {
                val child = tree.child(root, x);
                roundLayout(tree, child);
            }
        }
    }
}
