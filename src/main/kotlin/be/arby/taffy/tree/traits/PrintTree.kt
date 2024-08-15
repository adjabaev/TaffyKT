package be.arby.taffy.tree.traits

import be.arby.taffy.tree.layout.Layout

/**
 * Trait used by the `print_tree` method which prints a debug representation
 *
 * As indicated by it's dependence on `TraverseTree`, it required full recursive access to the tree.
 */
interface PrintTree : TraverseTree {
    /**
     * Get a debug label for the node (typically the type of node: flexbox, grid, text, image, etc)
     */
    fun getUnroundedLayout(nodeId: Int):Layout

    /**
     * Get a reference to the node's final layout
     */
    fun setFinalLayout(nodeId: Int): Layout
}
