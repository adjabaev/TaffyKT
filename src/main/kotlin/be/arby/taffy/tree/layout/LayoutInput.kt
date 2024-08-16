package be.arby.taffy.tree.layout

import be.arby.taffy.geom.Line
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.AvailableSpace

/**
 * A struct containing the inputs constraints/hints for laying out a node, which are passed in by the parent
 */
data class LayoutInput(
    /**
     * Whether we only need to know the Node's size, or whe
     */
    val runMode: RunMode,
    /**
     * Whether a Node's style sizes should be taken into account or ignored
     */
    val sizingMode: SizingMode,
    /**
     * Which axis we need the size of
     */
    val axis: RequestedAxis,

    /**
     * Known dimensions represent dimensions (width/height) which should be taken as fixed when performing layout.
     * For example, if known_dimensions.width is set to Some(WIDTH) then this means something like:
     *
     *    "What would the height of this node be, assuming the width is WIDTH"
     *
     * Layout functions will be called with both known_dimensions set for final layout. Where the meaning is:
     *
     *   "The exact size of this node is WIDTHxHEIGHT. Please lay out your children"
     *
     */
    val knownDimensions: Size<Option<Float>>,
    /**
     * Parent size dimensions are intended to be used for percentage resolution.
     */
    val parentSize: Size<Option<Float>>,
    /**
     * Available space represents an amount of space to layout into, and is used as a soft constraint
     * for the purpose of wrapping.
     */
    val availableSpace: Size<AvailableSpace>,
    /**
     * Specific to CSS Block layout. Used for correctly computing margin collapsing. You probably want to set this to `Line::FALSE`.
     */
    val verticalMarginsAreCollapsible: Line<Boolean>
) {
    companion object {
        val HIDDEN = LayoutInput(
            // The important property for hidden layout
            runMode = RunMode.PERFORM_HIDDEN_LAYOUT,
            // The rest will be ignored
            knownDimensions = Size.NONE.clone(),
            parentSize = Size.NONE.clone(),
            availableSpace = Size.MAX_CONTENT,
            sizingMode = SizingMode.INHERENT_SIZE,
            axis = RequestedAxis.BOTH,
            verticalMarginsAreCollapsible = Line.FALSE
        )
    }
}
