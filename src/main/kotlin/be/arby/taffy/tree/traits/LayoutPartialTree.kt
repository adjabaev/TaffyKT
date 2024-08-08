package be.arby.taffy.tree.traits

import be.arby.taffy.style.CoreStyle
import be.arby.taffy.tree.NodeId
import be.arby.taffy.tree.cache.Cache
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.tree.layout.LayoutInput
import be.arby.taffy.tree.layout.LayoutOutput

/**
 * Any type that implements [`LayoutPartialTree`] can be laid out using [Taffy's algorithms](crate::compute)
 *
 * Note that this trait extends [`TraversePartialTree`] (not [`TraverseTree`]). Taffy's algorithm implementations have been designed such that they can be used for a laying out a single
 */
interface LayoutPartialTree: TraversePartialTree {

    /**
     * Get core style
     */
    fun getCoreContainerStyle(nodeId: NodeId): CoreStyle

    /**
     * Set the node's unrounded layout
     */
    fun setUnroundedLayout(nodeId: NodeId, layout: Layout)

    /**
     * Get a mutable reference to the [`Cache`] for this node.
     */
    fun getCache(nodeId: NodeId): Cache

    /**
     * Compute the specified node's size or full layout given the specified constraints
     */
    fun computeChildLayout(nodeId: NodeId, inputs: LayoutInput): LayoutOutput
}
