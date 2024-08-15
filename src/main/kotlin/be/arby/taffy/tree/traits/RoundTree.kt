package be.arby.taffy.tree.traits

import be.arby.taffy.tree.layout.Layout

/**
 * Trait used by the `round_layout` method which takes a tree of unrounded float-valued layouts and performs
 * rounding to snap the values to the pixel grid.
 *
 * As indicated by it's dependence on `TraverseTree`, it required full recursive access to the tree.
 */
interface RoundTree : TraverseTree {
    /**
     * Get the node's unrounded layout
     */
    fun getUnroundedLayout(nodeId: Int):Layout

    /**
     * Get a reference to the node's final layout
     */
    fun setFinalLayout(nodeId: Int, layout: Layout)
}
