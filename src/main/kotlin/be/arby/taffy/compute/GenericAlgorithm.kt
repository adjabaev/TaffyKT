package be.arby.taffy.compute

import be.arby.taffy.compute.Compute.Companion.computeNodeLayout
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.tree.layout.RunMode
import be.arby.taffy.layout.SizeAndBaselines
import be.arby.taffy.layout.SizingMode
import be.arby.taffy.node.Node
import be.arby.taffy.style.dimension.AvailableSpace

class GenericAlgorithm : LayoutAlgorithm {
    companion object {
        const val NAME: String = "GENERIC"

        /**
         * Compute the size of the node given the specified constraints
         */
        @JvmStatic
        fun measureSize(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            sizingMode: SizingMode
        ): Size<Float> {
            return computeNodeLayout(
                tree,
                node,
                knownDimensions,
                parentSize,
                availableSpace,
                RunMode.COMPUTE_SIZE,
                sizingMode
            ).size
        }

        /**
         * Perform a full layout on the node given the specified constraints
         */
        @JvmStatic
        fun performLayout(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            sizingMode: SizingMode
        ): SizeAndBaselines {
            return computeNodeLayout(
                tree,
                node,
                knownDimensions,
                parentSize,
                availableSpace,
                RunMode.PERFORM_LAYOUT,
                sizingMode,
            )
        }
    }
}
