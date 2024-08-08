package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.lang.Option
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.utils.position

class ItemBatcher(
    var axis: AbstractAxis,
    var indexOffset: Int,
    var currentSpan: Int,
    var currentIsFlex: Boolean
) {

    fun next(items: List<GridItem>): Option<Pair<List<GridItem>, Boolean>> {
        if (currentIsFlex || indexOffset >= items.size) {
            return Option.None
        }

        val item = items[indexOffset]
        currentSpan = item.span(axis)
        currentIsFlex = item.crossesFlexibleTrack(axis)

        val nextIndexOffset = if (currentIsFlex) {
            items.size
        } else {
            items
                .position { it.crossesFlexibleTrack(axis) || it.span(axis) > currentSpan }
                .unwrapOr(items.size)
        }

        val batchRange = indexOffset until nextIndexOffset
        indexOffset = nextIndexOffset

        val batch = items.slice(batchRange)
        return Option.Some(Pair(batch, currentIsFlex))
    }

    companion object {
        fun new(axis: AbstractAxis): ItemBatcher {
            return ItemBatcher(indexOffset = 0, axis = axis, currentSpan = 1, currentIsFlex = false)
        }
    }
}
