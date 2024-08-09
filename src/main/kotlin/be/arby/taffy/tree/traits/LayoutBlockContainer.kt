package be.arby.taffy.tree.traits

import be.arby.taffy.style.block.BlockContainerStyle
import be.arby.taffy.style.block.BlockItemStyle
import be.arby.taffy.tree.NodeId

/**
 * Extends [LayoutPartialTree] with getters for the styles required for CSS Block layout
 */
interface LayoutBlockContainer : LayoutPartialTree {
    /**
     * Get the container's styles
     */
    fun getBlockContainerStyle(nodeId: NodeId): BlockContainerStyle

    /**
     * Get the child's styles
     */
    fun getBlockChildStyle(childNodeId: NodeId): BlockItemStyle
}
