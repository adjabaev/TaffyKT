package be.arby.taffy.style.grid

import be.arby.taffy.compute.grid.types.GridLine
import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.geom.Line
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.CoreStyle
import be.arby.taffy.style.Style
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.alignment.JustifyContent
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage

/**
 * The set of styles required for a CSS Grid item (child of a CSS Grid container)
 */
interface GridItemStyle: CoreStyle {
    /**
     * Defines which row in the grid the item should start and end at
     */
    fun gridRow(): Line<GridPlacement> {
        return Style.DEFAULT.gridRow
    }
    /**
     * Defines which column in the grid the item should start and end at
     */
    fun gridColumn(): Line<GridPlacement> {
        return Style.DEFAULT.gridColumn
    }

    /**
     * How this node should be aligned in the cross/block axis
     * Falls back to the parents [`AlignItems`] if not set
     */
    fun alignSelf(): Option<AlignSelf> {
        return Style.DEFAULT.alignSelf
    }

    /**
     * How this node should be aligned in the inline axis
     * Falls back to the parents [`super::JustifyItems`] if not set
     */
    fun justifySelf(): Option<AlignSelf> {
        return Style.DEFAULT.justifySelf
    }

    /**
     * Get a grid item's row or column placement depending on the axis passed
     */
    fun gridPlacement(axis: AbsoluteAxis): Line<GridPlacement> {
        return when (axis) {
            AbsoluteAxis.HORIZONTAL -> gridColumn()
            AbsoluteAxis.VERTICAL -> gridRow()
        }
    }
}
