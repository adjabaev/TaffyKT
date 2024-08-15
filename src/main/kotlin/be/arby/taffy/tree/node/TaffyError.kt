package be.arby.taffy.tree.node

sealed class TaffyError : Exception() {
    /**
     * The parent node does not have a child at `child_index`. It only has `child_count` children
     */
    data class ChildIndexOutOfBounds(
        /**
         * The parent node whose child was being looked up
         */
        val parent: Int,
        /**
         * The index that was looked up
         */
        val childIndex: Int,
        /**
         * The total number of children the parent has
         */
        val childCount: Int
    ) : TaffyError()

    /**
     * The parent node was not found in the [`TaffyTree`](crate::TaffyTree) instance.
     */
    data class InvalidParentNode(val parent: Int) : TaffyError()

    /**
     * The child node was not found in the [`TaffyTree`](crate::TaffyTree) instance.
     */
    data class InvalidChildNode(val parent: Int) : TaffyError()

    /**
     * The supplied node was not found in the [`TaffyTree`](crate::TaffyTree) instance.
     */
    data class InvalidInputNode(val parent: Int) : TaffyError()
}
