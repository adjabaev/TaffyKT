package be.arby.taffy.tree.traits

import be.arby.taffy.tree.NodeId

/**
 * Extends [LayoutPartialTree] with getters for the styles required for CSS Grid layout
 */
interface LayoutGridContainer : LayoutPartialTree {
    /**
     * Get the container's styles
     */
    fun getBlockContainerStyle(nodeId: NodeId): GridContainerStyle

    /**
     * Get the child's styles
     */
    fun getBlockChildStyle(childNodeId: NodeId): GridItemStyle
}
