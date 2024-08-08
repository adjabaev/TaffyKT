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
    fun flexDirection(): FlexDirection {
        return Style.DEFAULT.flexDirection
    }

    /**
     * Should elements wrap, or stay in a single line?
     */
    fun flexWrap(): FlexWrap {
        return Style.DEFAULT.flexWrap
    }

    /**
     * How large should the gaps between items in a grid or flex container be?
     */
    fun gap(): Size<LengthPercentage> {
        return Style.DEFAULT.gap
    }

    /// Alignment properties

    /**
     * How should content contained within this item be aligned in the cross/block axis
     */
    fun alignContent(): Option<AlignContent> {
        return Style.DEFAULT.alignContent
    }

    /**
     * How this node's children aligned in the cross/block axis?
     */
    fun alignItems(): Option<AlignItems> {
        return Style.DEFAULT.alignItems
    }

    /**
     * How this node's children should be aligned in the inline axis
     */
    fun justifyContent(): Option<JustifyContent> {
        return Style.DEFAULT.justifyContent
    }
}
