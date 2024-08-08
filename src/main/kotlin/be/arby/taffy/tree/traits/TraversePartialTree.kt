package be.arby.taffy.tree.traits

import be.arby.taffy.tree.NodeId

/**
 * This trait is Taffy's abstraction for downward tree traversal.
 * However, this trait does *not* require access to any node's other than a single container node's immediate children unless you also intend to implement `TraverseTree`.
 */
interface TraversePartialTree {
    /**
     * Get the list of children IDs for the given node
     */
    fun childIds(parentNodeId: NodeId): List<NodeId>

    /**
     * Get the number of children for the given node
     */
    fun childCount(parentNodeId: NodeId): Int

    /**
     * Get a specific child of a node, where the index represents the nth child
     */
    fun getChildId(parentNodeId: NodeId, childIndex: Int): NodeId
}
