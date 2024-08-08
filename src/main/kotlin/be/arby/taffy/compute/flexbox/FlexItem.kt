package be.arby.taffy.compute.flexbox

import be.arby.taffy.geom.Point
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.tree.NodeId

/**
 * The intermediate results of a flexbox calculation for a single item
 */
data class FlexItem (
    /**
     * The identifier for the associated node
     */
    var node: NodeId,

    /**
     * The order of the node relative to it's siblings
     */
    var order: Int,

    /**
     * The base size of this item
     */
    var size: Size<Option<Float>>,
    /**
     * The minimum allowable size of this item
     */
    var minSize: Size<Option<Float>>,
    /**
     * The maximum allowable size of this item
     */
    var maxSize: Size<Option<Float>>,
    /**
     * The cross-alignment of this item
     */
    var alignSelf: AlignSelf,

    /**
     * The overflow style of the item
     */
    var overflow: Point<Overflow>,
    /**
     * The width of the scrollbars (if it has any)
     */
    var scrollbarWidth: Float,
    /**
     * The flex shrink style of the item
     */
    var flexShrink: Float,
    /**
     * The flex grow style of the item
     */
    var flexGrow: Float,

    /**
     * The minimum size of the item. This differs from min_size above because it also
     * takes into account content based automatic minimum sizes
     */
    var resolvedMinimumMainSize: Float,

    /**
     * The final offset of this item
     */
    var inset: Rect<Option<Float>>,
    /**
     * The margin of this item
     */
    var margin: Rect<Float>,
    /**
     * Whether each margin is an auto margin or not
     */
    var marginIsAuto: Rect<Boolean>,
    /**
     * The padding of this item
     */
    var padding: Rect<Float>,
    /**
     * The border of this item
     */
    var border: Rect<Float>,

    /**
     * The default size of this item
     */
    var flexBasis: Float,
    /**
     * The default size of this item, minus padding and border
     */
    var innerFlexBasis: Float,
    /**
     * The amount by which this item has deviated from its target size
     */
    var violation: Float,
    /**
     * Is the size of this item locked
     */
    var frozen: Boolean,

    /**
     * Either the max- or min- content flex fraction
     * See https://www.w3.org/TR/css-flexbox-1/#intrinsic-main-sizes
     */
    var contentFlexFraction: Float,

    /**
     * The proposed inner size of this item
     */
    var hypotheticalInnerSize: Size<Float>,
    /**
     * The proposed outer size of this item
     */
    var hypotheticalOuterSize: Size<Float>,
    /**
     * The size that this item wants to be
     */
    var targetSize: Size<Float>,
    /**
     * The size that this item wants to be, plus any padding and border
     */
    var outerTargetSize: Size<Float>,

    /**
     * The position of the bottom edge of this item
     */
    var baseline: Float,

    /**
     * A temporary value for the main offset
     *
     * Offset is the relative position from the item's natural flow position based on
     * relative position values, alignment, and justification. Does not include margin/padding/border.
     */
    var offsetMain: Float,
    /**
     * A temporary value for the cross offset
     *
     * Offset is the relative position from the item's natural flow position based on
     * relative position values, alignment, and justification. Does not include margin/padding/border.
     */
    var offsetCross: Float,
)
