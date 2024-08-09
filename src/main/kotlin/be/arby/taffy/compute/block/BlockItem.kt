package be.arby.taffy.compute.block

import be.arby.taffy.geom.Point
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.Position
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.tree.NodeId

/**
 * Per-child data that is accumulated and modified over the course of the layout algorithm
 */
data class BlockItem(
    /**
     * The identifier for the associated node
     */
    val nodeId: NodeId,

    /**
     * The "source order" of the item. This is the index of the item within the children iterator,
     * and controls the order in which the nodes are placed
     */
    val order: Int,

    /**
     * Items that are tables don't have stretch sizing applied to them
     */
    val isTable: Boolean,

    /**
     * The base size of this item
     */
    val size: Size<Option<Float>>,
    /**
     * The minimum allowable size of this item
     */
    val minSize: Size<Option<Float>>,
    /**
     * The maximum allowable size of this item
     */
    val maxSize: Size<Option<Float>>,

    /**
     * The overflow style of the item
     */
    val overflow: Point<Overflow>,
    /**
     * The width of the item's scrollbars (if it has scrollbars)
     */
    val scrollbarWidth: Float,

    /**
     * The position style of the item
     */
    val position: Position,
    /**
     * The final offset of this item
     */
    val inset: Rect<LengthPercentageAuto>,
    /**
     * The margin of this item
     */
    val margin: Rect<LengthPercentageAuto>,
    /**
     * The padding of this item
     */
    val padding: Rect<Float>,
    /**
     * The border of this item
     */
    val border: Rect<Float>,
    /**
     * The sum of padding and border for this item
     */
    val paddingBorderSum: Size<Float>,

    /**
     * The computed border box size of this item
     */
    var computedSize: Size<Float>,
    /**
     * The computed "static position" of this item. The static position is the position
     * taking into account padding, border, margins, and scrollbar_gutters but not inset
     */
    var staticPosition: Point<Float>,
    /**
     * Whether margins can be collapsed through this item
     */
    var canBeCollapsedThrough: Boolean
)
