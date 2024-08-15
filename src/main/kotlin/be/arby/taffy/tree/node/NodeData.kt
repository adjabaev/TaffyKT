package be.arby.taffy.tree.node

import be.arby.taffy.style.Style
import be.arby.taffy.tree.cache.Cache
import be.arby.taffy.tree.layout.Layout

/**
 * Layout information for a given [`Node`](crate::node::Node)
 *
 * Stored in a [`TaffyTree`].
 */
data class NodeData(
    /**
     * The layout strategy used by this node
     */
    var style: Style,
    /**
     * The always unrounded results of the layout computation. We must store this separately from the rounded
     * layout to avoid errors from rounding already-rounded values. See <https://github.com/DioxusLabs/taffy/issues/501>.
     */
    var unroundedLayout: Layout,
    /**
     * The final results of the layout computation.
     * These may be rounded or unrounded depending on what the `use_rounding` config setting is set to.
     */
    var finalLayout: Layout,
    /**
     * Whether the node has context data associated with it or not
     */
    var hasContext: Boolean,
    /**
     * The cached results of the layout computation
     */
    val cache: Cache
) {
    /**
     * Marks a node and all of its parents (recursively) as dirty
     *
     * This clears any cached data and signals that the data must be recomputed.
     */
    fun markDirty() {
        cache.clear()
    }

    companion object {
        /**
         * Create the data for a new node
         */
        fun new (style: Style): NodeData {
            return NodeData(
                style = style,
                cache = Cache.new(),
                unroundedLayout = Layout.new(),
                finalLayout = Layout.new(),
                hasContext = false
            )
        }
    }
}
