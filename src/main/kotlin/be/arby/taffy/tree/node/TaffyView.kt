package be.arby.taffy.tree.node

import be.arby.taffy.compute.block.computeBlockLayout
import be.arby.taffy.compute.computeCachedLayout
import be.arby.taffy.compute.computeHiddenLayout
import be.arby.taffy.compute.computeLeafLayout
import be.arby.taffy.compute.flexbox.computeFlexboxLayout
import be.arby.taffy.compute.grid.computeGridLayout
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.then
import be.arby.taffy.style.CoreStyle
import be.arby.taffy.style.Display
import be.arby.taffy.style.Style
import be.arby.taffy.style.block.BlockContainerStyle
import be.arby.taffy.style.block.BlockItemStyle
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.flex.FlexboxContainerStyle
import be.arby.taffy.style.flex.FlexboxItemStyle
import be.arby.taffy.style.grid.GridContainerStyle
import be.arby.taffy.style.grid.GridItemStyle
import be.arby.taffy.tree.cache.Cache
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.tree.layout.LayoutInput
import be.arby.taffy.tree.layout.LayoutOutput
import be.arby.taffy.tree.layout.RunMode
import be.arby.taffy.tree.traits.*

/**
 * View over the Taffy tree that holds the tree itself along with a reference to the context
 * and implements LayoutTree. This allows the context to be stored outside of the TaffyTree struct
 * which makes the lifetimes of the context much more flexible.
 */
data class TaffyView<
        NodeContext,
        MeasureFunction : (Size<Option<Float>>, Size<AvailableSpace>, Int, Option<NodeContext>, Style) -> Size<Float>>(
    /**
     * A reference to the TaffyTree
     */
    val taffy: TaffyTree<NodeContext>,
    /**
     * The context provided for passing to measure functions if layout is run over this struct
     */
    val measureFunction: MeasureFunction
) : TraversePartialTree, TraverseTree, LayoutPartialTree, LayoutBlockContainer, LayoutFlexboxContainer,
    LayoutGridContainer, RoundTree {

    override fun childIds(parentNodeId: Int): MutableList<Int> {
        return taffy.childIds(parentNodeId)
    }

    override fun childCount(parentNodeId: Int): Int {
        return taffy.childCount(parentNodeId)
    }

    override fun getChildId(parentNodeId: Int, childIndex: Int): Int {
        return taffy.getChildId(parentNodeId, childIndex)
    }

    override fun getCoreContainerStyle(nodeId: Int): CoreStyle {
        return taffy.nodes[nodeId]?.style ?: throw TaffyError.InvalidInputNode(nodeId)
    }

    override fun getCache(nodeId: Int): Cache {
        return taffy.nodes[nodeId]?.cache ?: throw TaffyError.InvalidInputNode(nodeId)
    }

    override fun setUnroundedLayout(nodeId: Int, layout: Layout) {
        taffy.nodes[nodeId]?.unroundedLayout = layout
    }

    override fun computeChildLayout(nodeId: Int, inputs: LayoutInput): LayoutOutput {
        // If RunMode is PerformHiddenLayout then this indicates that an ancestor node is `Display::None`
        // and thus that we should lay out this node using hidden layout regardless of it's own display style.
        if (inputs.runMode == RunMode.PERFORM_HIDDEN_LAYOUT) {
            return computeHiddenLayout(this, nodeId)
        }

        // We run the following wrapped in "compute_cached_layout", which will check the cache for an entry matching the node and inputs and:
        //   - Return that entry if exists
        //   - Else call the passed closure (below) to compute the result
        //
        // If there was no cache match and a new result needs to be computed then that result will be added to the cache

        return computeCachedLayout(this, nodeId, inputs) { tree, node, inputs ->
            val displayMode = tree.taffy.nodes[node]!!.style.display
            val hasChildren = tree.childCount(node) > 0

            // Dispatch to a layout algorithm based on the node's display style and whether the node has children or not.
            if (displayMode == Display.NONE) {
                computeHiddenLayout(tree, node)
            } else if (displayMode == Display.BLOCK && hasChildren) {
                computeBlockLayout(tree, node, inputs)
            } else if (displayMode == Display.FLEX && hasChildren) {
                computeFlexboxLayout(tree, node, inputs)
            } else if (displayMode == Display.GRID && hasChildren) {
                computeGridLayout(tree, node, inputs)
            } else {
                val style = tree.taffy.nodes[node]!!.style
                val hasContext = tree.taffy.nodes[node]!!.hasContext
                val nodeContext = hasContext.then { tree.taffy.nodeContextData[node] ?: throw TaffyError.InvalidInputNode(node) }
                val measureFunction: (Size<Option<Float>>, Size<AvailableSpace>) -> Size<Float> = { knownDimensions, availableSpace ->
                    (tree.measureFunction)(knownDimensions, availableSpace, node, nodeContext, style)
                }
                computeLeafLayout(inputs, style, measureFunction)
            }
        }
    }

    override fun getBlockContainerStyle(nodeId: Int): BlockContainerStyle {
        return getCoreContainerStyle(nodeId) as BlockContainerStyle
    }

    override fun getBlockChildStyle(childNodeId: Int): BlockItemStyle {
        return getCoreContainerStyle(childNodeId) as BlockItemStyle
    }

    override fun getFlexboxContainerStyle(nodeId: Int): FlexboxContainerStyle {
        return getCoreContainerStyle(nodeId) as FlexboxContainerStyle
    }

    override fun getFlexboxChildStyle(childNodeId: Int): FlexboxItemStyle {
        return getCoreContainerStyle(childNodeId) as FlexboxItemStyle
    }

    override fun getGridContainerStyle(nodeId: Int): GridContainerStyle {
        return getCoreContainerStyle(nodeId) as GridContainerStyle
    }

    override fun getGridChildStyle(childNodeId: Int): GridItemStyle {
        return getCoreContainerStyle(childNodeId) as GridItemStyle
    }

    override fun getUnroundedLayout(nodeId: Int): Layout {
        return taffy.nodes[nodeId]?.unroundedLayout ?: throw TaffyError.InvalidInputNode(nodeId)
    }

    override fun setFinalLayout(nodeId: Int, layout: Layout) {
        taffy.nodes[nodeId]?.finalLayout = layout
    }
}
