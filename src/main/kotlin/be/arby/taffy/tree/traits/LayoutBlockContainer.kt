package be.arby.taffy.tree.traits

import be.arby.taffy.style.block.BlockContainerStyle
import be.arby.taffy.style.block.BlockItemStyle

/**
 * Extends [LayoutPartialTree] with getters for the styles required for CSS Block layout
 */
interface LayoutBlockContainer : LayoutPartialTree {
    /**
     * Get the container's styles
     */
    fun getBlockContainerStyle(nodeId: Int): BlockContainerStyle

    /**
     * Get the child's styles
     */
    fun getBlockChildStyle(childNodeId: Int): BlockItemStyle
}
