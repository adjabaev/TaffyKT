package be.arby.taffy.tree.layout

import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option


/**
 * A struct containing the result of laying a single node, which is returned up to the parent node
 *
 * A baseline is the line on which text sits. Your node likely has a baseline if it is a text node, or contains
 * children that may be text nodes. See <https://www.w3.org/TR/css-writing-modes-3/#intro-baselines> for details.
 * If your node does not have a baseline (or you are unsure how to compute it), then simply return `Point::NONE`
 * for the first_baselines field
 */
data class LayoutOutput(
    /**
     * The size of the node
     */
    val size: Size<Float>,
    /**
     * The size of the content within the node
     */
    val contentSize: Size<Float>,
    /**
     * The first baseline of the node in each dimension, if any
     */
    val firstBaselines: Point<Option<Float>>,
    /**
     * Top margin that can be collapsed with. This is used for CSS block layout and can be set to
     * `CollapsibleMarginSet::ZERO` for other layout modes that don't support margin collapsing
     */
    val topMargin: CollapsibleMarginSet,
    /**
     * Bottom margin that can be collapsed with. This is used for CSS block layout and can be set to
     * `CollapsibleMarginSet::ZERO` for other layout modes that don't support margin collapsing
     */
    val bottomMargin: CollapsibleMarginSet,
    /**
     * Whether margins can be collapsed through this node. This is used for CSS block layout and can
     * be set to `false` for other layout modes that don't support margin collapsing
     */
    val marginsCanCollapseThrough: Boolean
) {
    companion object {
        /**
         * An all-zero `LayoutOutput` for hidden nodes
         */
        val HIDDEN = LayoutOutput(
            size = Size.ZERO,
            contentSize = Size.ZERO,
            firstBaselines = Point.NONE,
            topMargin = CollapsibleMarginSet.ZERO,
            bottomMargin = CollapsibleMarginSet.ZERO,
            marginsCanCollapseThrough = false
        )

        /**
         * A blank layout output
         */
        val DEFAULT = HIDDEN

        /**
         * Constructor to create a `LayoutOutput` from just the size and baselines
         */
        fun fromSizesAndBaselines(
            size: Size<Float>,
            contentSize: Size<Float>,
            firstBaselines: Point<Option<Float>>,
        ): LayoutOutput {
            return LayoutOutput(
                size,
                contentSize,
                firstBaselines,
                topMargin = CollapsibleMarginSet.ZERO,
                bottomMargin = CollapsibleMarginSet.ZERO,
                marginsCanCollapseThrough = false
            )
        }

        /**
         * Construct a SizeBaselinesAndMargins from just the container and content sizes
         */
        fun fromSizes(size: Size<Float>, contentSize: Size<Float>): LayoutOutput {
            return fromSizesAndBaselines(size, contentSize, Point.NONE)
        }

        /**
         * Construct a SizeBaselinesAndMargins from just the container's size.
         */
        fun fromOuterSize(size: Size<Float>): LayoutOutput {
            return fromSizes(size, Size.zeroF())
        }
    }
}
