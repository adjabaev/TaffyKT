package be.arby.taffy.compute.flexbox

import be.arby.taffy.geom.Point
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.JustifyContent
import be.arby.taffy.style.flex.FlexDirection

/**
 * Values that can be cached during the flexbox algorithm.
 */
data class AlgoConstants(
    /**
     * The direction of the current segment being laid out
     */
    val dir: FlexDirection,
    /**
     * Is this segment a row
     */
    val isRow: Boolean,
    /**
     * Is this segment a column
     */
    val isColumn: Boolean,
    /**
     * Is wrapping enabled (in either direction)
     */
    val isWrap: Boolean,
    /**
     * Is the wrap direction inverted
     */
    val isWrapReverse: Boolean,

    /**
     * The item's min_size style
     */
    val minSize: Size<Option<Float>>,
    /**
     * The item's max_size style
     */
    val maxSize: Size<Option<Float>>,
    /**
     * The margin of this section
     */
    val margin: Rect<Float>,
    /**
     * The border of this section
     */
    val border: Rect<Float>,
    /**
     * The space between the content box and the border box.
     * This consists of padding + border + scrollbar_gutter.
     */
    val contentBoxInset: Rect<Float>,
    /**
     * The size reserved for scrollbar gutters in each axis
     */
    val scrollbarGutter: Point<Float>,
    /**
     * The gap of this section
     */
    val gap: Size<Float>,
    /**
     * The align_items property of this node
     */
    val alignItems: AlignItems,
    /**
     * The align_content property of this node
     */
    val alignContent: AlignContent,
    /**
     * The justify_content property of this node
     */
    val justifyContent: Option<JustifyContent>,

    /**
     * The border-box size of the node being laid out (if known)
     */
    val nodeOuterSize: Size<Option<Float>>,
    /**
     * The content-box size of the node being laid out (if known)
     */
    val nodeInnerSize: Size<Option<Float>>,

    /**
     * The size of the virtual container containing the flex items
     */
    val containerSize: Size<Float>,
    /**
     * The size of the internal container
     */
    val innerContainerSize: Size<Float>
)
