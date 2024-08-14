package be.arby.taffy.style.flex

import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.CoreStyle
import be.arby.taffy.style.Style
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.JustifyContent
import be.arby.taffy.style.dimension.LengthPercentage

/**
 * The set of styles required for a Flexbox container
 */
interface FlexboxContainerStyle: CoreStyle {
    /**
     * Which direction does the main axis flow in?
     */
    fun flexDirection(): FlexDirection

    /**
     * Should elements wrap, or stay in a single line?
     */
    fun flexWrap(): FlexWrap

    /**
     * How large should the gaps between items in a grid or flex container be?
     */
    fun gap(): Size<LengthPercentage>

    /// Alignment properties

    /**
     * How should content contained within this item be aligned in the cross/block axis
     */
    fun alignContent(): Option<AlignContent>

    /**
     * How this node's children aligned in the cross/block axis?
     */
    fun alignItems(): Option<AlignItems>

    /**
     * How this node's children should be aligned in the inline axis
     */
    fun justifyContent(): Option<JustifyContent>
}
