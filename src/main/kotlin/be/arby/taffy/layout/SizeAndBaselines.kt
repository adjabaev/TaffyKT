package be.arby.taffy.layout

import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option

/**
 * A baseline is the line on which text sits. Your node likely has a baseline if it is a text node, or contains
 * children that may be text nodes. See https://www.w3.org/TR/css-writing-modes-3/#intro-baselines for details.
 * If your node does not have a baseline (or you are unsure how to compute it), then simply return Point.NONE
 * for the first_baselines field
 */
data class SizeAndBaselines(
    /**
     * The size of the node
     */
    val size: Size<Float>,
    /**
     * The first baseline of the node in each dimension, if any
     */
    val firstBaselines: Point<Option<Float>>
)
