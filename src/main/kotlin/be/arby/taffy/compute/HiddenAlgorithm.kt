package be.arby.taffy.compute

import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.layout.SizeAndBaselines
import be.arby.taffy.layout.SizingMode
import be.arby.taffy.node.Node
import be.arby.taffy.style.dimension.AvailableSpace
import java.util.*

class HiddenAlgorithm : LayoutAlgorithm {
    companion object {
        const val NAME: String = "NONE"

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
            Compute.performHiddenLayout(
                tree,
                node
            )
            return Size.zeroF()
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
            Compute.performHiddenLayout(
                tree,
                node
            )
            return SizeAndBaselines(size = Size.zeroF(), firstBaselines = Point.NONE)
        }
    }
}
