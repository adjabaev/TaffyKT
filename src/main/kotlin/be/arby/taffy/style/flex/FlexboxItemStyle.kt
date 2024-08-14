package be.arby.taffy.style.flex

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
 * The set of styles required for a Flexbox item (child of a Flexbox container)
 */
interface FlexboxItemStyle: CoreStyle {
    /**
     * Sets the initial main axis size of the item
     */
    fun flexBasis(): Dimension

    /**
     * The relative rate at which this item grows when it is expanding to fill space
     */
    fun flexGrow(): Float

    /**
     * The relative rate at which this item shrinks when it is contracting to fit into space
     */
    fun flexShrink(): Float

    /**
     * How this node should be aligned in the cross/block axis
     * Falls back to the parents [AlignItems] if not set
     */
    fun alignSelf(): Option<AlignSelf>
}
