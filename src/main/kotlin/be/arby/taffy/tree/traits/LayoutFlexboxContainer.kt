package be.arby.taffy.tree.traits

import be.arby.taffy.style.flex.FlexboxContainerStyle
import be.arby.taffy.style.flex.FlexboxItemStyle
import be.arby.taffy.tree.NodeId

/**
 * Extends [LayoutPartialTree] with getters for the styles required for Flexbox layout
 */
interface LayoutFlexboxContainer : LayoutPartialTree {
    /**
     * Get the container's styles
     */
    fun getFlexboxContainerStyle(nodeId: NodeId): FlexboxContainerStyle

    /**
     * Get a reference to the node's final layout
     */
    fun getFlexboxChildStyle(childNodeId: NodeId): FlexboxItemStyle
}
