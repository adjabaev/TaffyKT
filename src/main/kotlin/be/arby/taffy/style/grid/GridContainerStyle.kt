package be.arby.taffy.style.grid

import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.CoreStyle
import be.arby.taffy.style.Style
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.JustifyContent
import be.arby.taffy.style.dimension.LengthPercentage

/**
 * The set of styles required for a CSS Grid container
 */
interface GridContainerStyle : CoreStyle {
    /**
     * Defines the track sizing functions (heights) of the grid rows
     */
    fun gridTemplateRows(): MutableList<TrackSizingFunction>

    /**
     *  Defines the track sizing functions (widths) of the grid columns
     */
    fun gridTemplateColumns(): MutableList<TrackSizingFunction>

    /**
     *  Defines the size of implicitly created rows
     */
    fun gridAutoRows(): MutableList<NonRepeatedTrackSizingFunction>

    /**
     *  Defines the size of implicitly created rows
     */
    fun gridAutoColumns(): MutableList<NonRepeatedTrackSizingFunction>

    /**
     * Controls how items get placed into the grid for auto-placed items
     */
    fun gridAutoFlow(): GridAutoFlow

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
     * How should contained within this item be aligned in the main/inline axis
     */
    fun justifyContent(): Option<JustifyContent>

    /**
     * How this node's children aligned in the cross/block axis?
     */
    fun alignItems(): Option<AlignItems>

    /**
     * How this node's children should be aligned in the inline axis
     */
    fun justifyItems(): Option<AlignItems>

    /**
     * Get a grid item's row or column placement depending on the axis passed
     */
    fun gridTemplateTracks(axis: AbsoluteAxis): MutableList<TrackSizingFunction> {
        return when (axis) {
            AbsoluteAxis.HORIZONTAL -> gridTemplateColumns()
            AbsoluteAxis.VERTICAL -> gridTemplateRows()
        }
    }

    /**
     * Get a grid container's align-content or justify-content alignment depending on the axis passed
     */
    fun gridAlignContent(axis: AbstractAxis): AlignContent {
        return when (axis) {
            AbstractAxis.INLINE -> justifyContent().unwrapOr(AlignContent.STRETCH)
            AbstractAxis.BLOCK -> alignContent().unwrapOr(AlignContent.STRETCH)
        }
    }
}
