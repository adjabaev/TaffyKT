package be.arby.taffy.style.grid

import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.lang.Default

/**
 * Controls whether grid items are placed row-wise or column-wise. And whether the sparse or dense packing algorithm is used.
 *
 * The "dense" packing algorithm attempts to fill in holes earlier in the grid, if smaller items come up later. This may cause items to appear out-of-order, when doing so would fill in holes left by larger items.
 *
 * Defaults to [`GridAutoFlow::Row`]
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-flow)
 */
enum class GridAutoFlow {
    /**
     * Items are placed by filling each row in turn, adding new rows as necessary
     */
    ROW,

    /**
     * Items are placed by filling each column in turn, adding new columns as necessary.
     */
    COLUMN,

    /**
     * Combines `Row` with the dense packing algorithm.
     */
    ROW_DENSE,

    /**
     * Combines `Column` with the dense packing algorithm.
     */
    COLUMN_DENSE;

    /**
     * Whether grid auto placement uses the sparse placement algorithm or the dense placement algorithm
     * See: <https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-flow#values>
     */
    fun isDense(): Boolean {
        return this == ROW_DENSE || this == COLUMN_DENSE
    }

    /**
     * Whether grid auto placement fills areas row-wise or column-wise
     * See: <https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-flow#values>
     */
    fun primaryAxis(): AbsoluteAxis {
        return when (this) {
            ROW, ROW_DENSE -> AbsoluteAxis.HORIZONTAL
            COLUMN, COLUMN_DENSE -> AbsoluteAxis.VERTICAL
        }
    }

    companion object: Default<GridAutoFlow> {
        override fun default(): GridAutoFlow {
            return ROW
        }
    }
}
