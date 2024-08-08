package be.arby.taffy.compute.flexbox

import be.arby.taffy.compute.LayoutAlgorithm
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.tree.layout.RunMode
import be.arby.taffy.layout.SizeAndBaselines
import be.arby.taffy.layout.SizingMode
import be.arby.taffy.node.Node
import be.arby.taffy.style.dimension.AvailableSpace

/**
 * The public interface to Taffy's Flexbox algorithm implementation
 */
class FlexboxAlgorithm : LayoutAlgorithm {
    companion object {
        const val NAME: String = "FLEXBOX"

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
            return FlexBox.compute(
                tree,
                node,
                knownDimensions,
                parentSize,
                availableSpace,
                RunMode.COMPUTE_SIZE
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
            return FlexBox.compute(
                tree,
                node,
                knownDimensions,
                parentSize,
                availableSpace,
                RunMode.PERFORM_LAYOUT
            )
        }
    }
}
