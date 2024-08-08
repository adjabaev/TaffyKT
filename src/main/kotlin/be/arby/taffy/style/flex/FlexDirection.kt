package be.arby.taffy.style.flex

import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.lang.Default

/**
 * The direction of the flexbox layout main axis.
 *
 * There are always two perpendicular layout axes: main (or primary) and cross (or secondary).
 * Adding items will cause them to be positioned adjacent to each other along the main axis.
 * By varying this value throughout your tree, you can create complex axis-aligned layouts.
 *
 * Items are always aligned relative to the cross axis, and justified relative to the main axis.
 *
 * The default behavior is [`FlexDirection::Row`].
 *
 * [Specification](https://www.w3.org/TR/css-flexbox-1/#flex-direction-property)
 */
enum class FlexDirection {
    /**
     * Defines +x as the main axis
     *
     * Items will be added from left to right in a row.
     */
    ROW,

    /**
     * Defines +y as the main axis
     *
     * Items will be added from top to bottom in a column.
     */
    COLUMN,

    /**
     * Defines -x as the main axis
     *
     * Items will be added from right to left in a row.
     */
    ROW_REVERSE,

    /**
     * Defines -y as the main axis
     *
     * Items will be added from bottom to top in a column.
     */
    COLUMN_REVERSE;

    /**
     * Is the direction [`FlexDirection::Row`] or [`FlexDirection::RowReverse`]?
     */
    fun isRow(): Boolean {
        return this == ROW || this == ROW_REVERSE
    }

    /**
     * Is the direction [`FlexDirection::Column`] or [`FlexDirection::ColumnReverse`]?
     */
    fun isColumn(): Boolean {
        return this == COLUMN || this == COLUMN_REVERSE
    }

    /**
     * Is the direction [`FlexDirection::RowReverse`] or [`FlexDirection::ColumnReverse`]?
     */
    fun isReverse(): Boolean {
        return this == ROW_REVERSE || this == COLUMN_REVERSE
    }

    /**
     * The `AbsoluteAxis` that corresponds to the main axis
     */
    fun mainAxis(): AbsoluteAxis {
        return when (this) {
            ROW, ROW_REVERSE -> AbsoluteAxis.HORIZONTAL
            COLUMN, COLUMN_REVERSE -> AbsoluteAxis.VERTICAL
        }
    }

    /**
     * The `AbsoluteAxis` that corresponds to the cross axis
     */
    fun crossAxis(): AbsoluteAxis {
        return when (this) {
            ROW, ROW_REVERSE -> AbsoluteAxis.VERTICAL
            COLUMN, COLUMN_REVERSE -> AbsoluteAxis.HORIZONTAL
        }
    }

    companion object: Default<FlexDirection> {
        override fun default(): FlexDirection {
            return ROW
        }
    }
}
