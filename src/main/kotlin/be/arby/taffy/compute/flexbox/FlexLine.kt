package be.arby.taffy.compute.flexbox

/**
 * A line of [FlexItem] used for intermediate computation
 */
data class FlexLine(
    /**
     * The slice of items to iterate over during computation of this line
     */
    var items: List<FlexItem>,

    /**
     * The dimensions of the cross-axis
     */
    var crossSize: Float,
    /**
     * The relative offset of the cross-axis
     */
    var offsetCross: Float
)
