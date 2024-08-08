package be.arby.taffy.tree

/**
 * A type representing the id of a single node in a tree of nodes
 *
 * Internally it is a wrapper around a u64 and a `NodeId` can be converted to and from
 * and u64 if needed.
 */
data class NodeId(val id: Long) {
    companion object {
        /**
         * Create a new NodeId from a u64 value
         */
        fun new(value: Long): NodeId {
            return NodeId(value)
        }
    }
}
