package be.arby.taffy.tree.traits

import be.arby.taffy.style.grid.GridContainerStyle
import be.arby.taffy.style.grid.GridItemStyle

/**
 * Extends [LayoutPartialTree] with getters for the styles required for CSS Grid layout
 */
interface LayoutGridContainer : LayoutPartialTree {
    /**
     * Get the container's styles
     */
    fun getGridContainerStyle(nodeId: Int): GridContainerStyle

    /**
     * Get the child's styles
     */
    fun getGridChildStyle(childNodeId: Int): GridItemStyle
}
