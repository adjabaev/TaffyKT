package be.arby.taffy.geom;

/**
 * Container that holds an item in each absolute axis without specifying
 * what kind of item it is.
 */
data class InBothAbsAxis<T>(
    /**
     * The item in the horizontal axis
     */
    var horizontal: T,
    /**
     * The item in the vertical axis
     */
    var vertical: T
) {
    /**
     * Get the contained item based on the AbsoluteAxis passed
     */
    fun get(axis: AbsoluteAxis): T {
        return when (axis) {
            AbsoluteAxis.HORIZONTAL -> horizontal
            AbsoluteAxis.VERTICAL -> vertical
        }
    }
}
