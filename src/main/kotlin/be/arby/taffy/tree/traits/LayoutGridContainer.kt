package be.arby.taffy.tree.traits

import be.arby.taffy.style.grid.GridContainerStyle
import be.arby.taffy.style.grid.GridItemStyle
import be.arby.taffy.tree.NodeId

/**
 * Extends [LayoutPartialTree] with getters for the styles required for CSS Grid layout
 */
interface LayoutGridContainer : LayoutPartialTree {
    /**
     * Get the container's styles
     */
    fun getGridContainerStyle(nodeId: NodeId): GridContainerStyle

    /**
     * Get the child's styles
     */
    fun getGridChildStyle(childNodeId: NodeId): GridItemStyle
}
