package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.collections.len
import be.arby.taffy.lang.collections.position
import be.arby.taffy.lang.tuples.T2

/**
 * Takes an axis, and a list of grid items sorted firstly by whether they cross a flex track
 * in the specified axis (items that don't cross a flex track first) and then by the number
 * of tracks they cross in specified axis (ascending order).
 */
data class ItemBatcher(
    /**
     * The axis in which the ItemBatcher is operating. Used when querying properties from items.
     */
    val axis: AbstractAxis,
    /**
     * The starting index of the current batch
     */
    var indexOffset: Int,
    /**
     * The span of the items in the current batch
     */
    var currentSpan: Int,
    /**
     * Whether the current batch of items cross a flexible track
     */
    var currentIsFlex: Boolean
) {

    /**
     * This is basically a manual version of Iterator::next which passes `items`
     * in as a parameter on each iteration to work around borrow checker rules
     */
    fun next(items: List<GridItem>): Option<T2<List<GridItem>, Boolean>> {
        if (currentIsFlex || indexOffset >= items.len()) {
            return Option.None
        }

        val item = items[indexOffset]
        currentSpan = item.span(axis)
        currentIsFlex = item.crossesFlexibleTrack(axis)

        val nextIndexOffset = if (currentIsFlex) {
            items.len()
        } else {
            items
                .position { item: GridItem ->
                item.crossesFlexibleTrack(axis) || item.span(axis) > currentSpan
            }
            .unwrapOr(items.len())
        }

        val batchRange = indexOffset until nextIndexOffset
        indexOffset = nextIndexOffset

        val batch = items.slice(batchRange)
        return Option.Some(T2(batch, currentIsFlex))
    }

    companion object {
        /**
         * Create a new ItemBatcher for the specified axis
         */
        fun new(axis: AbstractAxis): ItemBatcher {
            return ItemBatcher(indexOffset = 0, axis = axis, currentSpan = 1, currentIsFlex = false)
        }
    }
}
