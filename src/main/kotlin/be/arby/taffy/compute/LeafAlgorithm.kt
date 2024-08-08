package be.arby.taffy.compute

import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.layout.SizeAndBaselines
import be.arby.taffy.layout.SizingMode
import be.arby.taffy.node.Node
import be.arby.taffy.style.dimension.AvailableSpace

/**
 * The public interface to Taffy's leaf node algorithm implementation
 */
class LeafAlgorithm : LayoutAlgorithm {
    companion object {
        const val NAME: String = "LEAF"

        /**
         * Compute the size of the node given the specified constraints
         */
        fun measureSize(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            sizingMode: SizingMode
        ): Size<Float> {
            return Leaf.compute(
                tree,
                node,
                knownDimensions,
                parentSize,
                availableSpace,
                sizingMode
            ).size
        }

        /**
         * Perform a full layout on the node given the specified constraints
         */
        fun performLayout(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            sizingMode: SizingMode
        ): SizeAndBaselines {
            return Leaf.compute(
                tree,
                node,
                knownDimensions,
                parentSize,
                availableSpace,
                sizingMode
            )
        }
    }
}
