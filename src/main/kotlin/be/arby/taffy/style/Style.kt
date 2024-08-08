package be.arby.taffy.style

import be.arby.taffy.geometry.Line
import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Default
import be.arby.taffy.lang.Option
import be.arby.taffy.maths.axis.AbsoluteAxis
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.alignment.*
import be.arby.taffy.style.flex.FlexDirection
import be.arby.taffy.style.flex.FlexWrap
import be.arby.taffy.style.grid.*

/**
 * A typed representation of the CSS style information for a single node.
 *
 * The most important idea in flexbox is the notion of a "main" and "cross" axis, which are always perpendicular to each other.
 * The orientation of these axes are controlled via the [`FlexDirection`] field of this struct.
 *
 * This struct follows the [CSS equivalent](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Flexible_Box_Layout/Basic_Concepts_of_Flexbox) directly;
 * information about the behavior on the web should transfer directly.
 *
 * Detailed information about the exact behavior of each of these fields
 * can be found on [MDN](https://developer.mozilla.org/en-US/docs/Web/CSS) by searching for the field name.
 * The distinction between margin, padding and border is explained well in
 * this [introduction to the box model](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Box_Model/Introduction_to_the_CSS_box_model).
 *
 * If the behavior does not match the flexbox layout algorithm on the web, please file a bug!
 */
data class Style(
    /**
     * What layout strategy should be used?
     */
    var display: Display,
    /**
     * Whether a child is display:table or not. This affects children of block layouts.
     * This should really be part of `Display`, but it is currently separate because table layout isn't implemented
     */
    var itemIsTable: Boolean,
    /**
     * Should size styles apply to the content box or the border box of the node
     */
    var boxSizing: BoxSizing,

    /// Overflow properties
    /**
     * How children overflowing their container should affect layout
     */
    var overflow: Point<Overflow>,
    /**
     * How much space (in points) should be reserved for the scrollbars of `Overflow::Scroll` and `Overflow::Auto` nodes.
     */
    var scrollbarWidth: Float,

    /// Position properties
    /**
     * What should the `position` value of this struct use as a base offset?
     */
    var position: Position,
    /**
     * How should the position of this element be tweaked relative to the layout defined?
     */
    var inset: Rect<LengthPercentageAuto>,

    /// Size properties
    /**
     * Sets the initial size of the item
     */
    var size: Size<Dimension>,
    /**
     * Controls the minimum size of the item
     */
    var minSize: Size<Dimension>,
    /**
     * Controls the maximum size of the item
     */
    var maxSize: Size<Dimension>,
    /**
     * Sets the preferred aspect ratio for the item
     *
     * The ratio is calculated as width divided by height.
     */
    var aspectRatio: Option<Float>,

    /// Spacing Properties
    /**
     * How large should the margin be on each side?
     */
    var margin: Rect<LengthPercentageAuto>,
    /**
     * How large should the padding be on each side?
     */
    var padding: Rect<LengthPercentage>,
    /**
     * How large should the border be on each side?
     */
    var border: Rect<LengthPercentage>,

    /// Alignment properties
    /**
     * How this node's children aligned in the cross/block axis?
     */
    var alignItems: Option<AlignItems>,
    /**
     * How this node should be aligned in the cross/block axis
     * Falls back to the parents [`AlignItems`] if not set
     */
    var alignSelf: Option<AlignSelf>,
    /**
     * How this node's children should be aligned in the inline axis
     */
    var justifyItems: Option<JustifyItems>,
    /**
     * How this node should be aligned in the inline axis
     * Falls back to the parents [`JustifyItems`] if not set
     */
    var justifySelf: Option<JustifySelf>,
    /**
     * How should content contained within this item be aligned in the cross/block axis
     */
    var alignContent: Option<AlignContent>,
    /**
     * How should contained within this item be aligned in the main/inline axis
     */
    var justifyContent: Option<JustifyContent>,
    /**
     * How large should the gaps between items in a grid or flex container be?
     */
    var gap: Size<LengthPercentage>,

    /// Block container properties
    /**
     * How items elements should aligned in the inline axis
     */
    var textAlign: TextAlign,

    // Flexbox container properties
    /**
     * Which direction does the main axis flow in?
     */
    var flexDirection: FlexDirection,
    /**
     * Should elements wrap, or stay in a single line?
     */
    var flexWrap: FlexWrap,

    /// Flexbox item properties
    /**
     * Sets the initial main axis size of the item
     */
    var flexBasis: Dimension,
    /**
     * The relative rate at which this item grows when it is expanding to fill space
     *
     * 0f is the default value, and this value must be positive.
     */
    var flexGrow: Float,
    /**
     * The relative rate at which this item shrinks when it is contracting to fit into space
     *
     * 1f is the default value, and this value must be positive.
     */
    var flexShrink: Float,

    /// Grid container properties
    /**
     * Defines the track sizing functions (heights) of the grid rows
     */
    var gridTemplateRows: List<TrackSizingFunction>,
    /**
     * Defines the track sizing functions (widths) of the grid columns
     */
    var gridTemplateColumns: List<TrackSizingFunction>,
    /**
     * Defines the size of implicitly created rows
     */
    var gridAutoRows: List<NonRepeatedTrackSizingFunction>,
    /**
     * Defined the size of implicitly created columns
     */
    var gridAutoColumns: List<NonRepeatedTrackSizingFunction>,
    /**
     * Controls how items get placed into the grid for auto-placed items
     */
    var gridAutoFlow: GridAutoFlow,

    /// Grid child properties
    /**
     * Defines which row in the grid the item should start and end at
     */
    var gridRow: Line<GridPlacement>,
    /**
     * Defines which column in the grid the item should start and end at
     */
    var gridColumn: Line<GridPlacement>
): CoreStyle {
    fun gridTemplateTracks(axis: AbsoluteAxis): List<TrackSizingFunction> {
        return when (axis) {
            AbsoluteAxis.HORIZONTAL -> gridTemplateColumns
            AbsoluteAxis.VERTICAL -> gridTemplateRows
        }
    }

    fun gridPlacement(axis: AbsoluteAxis): Line<GridPlacement> {
        return when (axis) {
            AbsoluteAxis.HORIZONTAL -> gridColumn
            AbsoluteAxis.VERTICAL -> gridRow
        }
    }

    fun gridAlignContent(axis: AbstractAxis): AlignContent {
        return when (axis) {
            AbstractAxis.INLINE -> justifyContent.unwrapOr(AlignContent.STRETCH)
            AbstractAxis.BLOCK -> alignContent.unwrapOr(AlignContent.STRETCH)
        }
    }

    override fun boxGenerationMode(): BoxGenerationMode {
        return when (display) {
            Display.NONE -> BoxGenerationMode.NONE
            else -> BoxGenerationMode.NORMAL
        }
    }

    override fun isBlock(): Boolean {
        return display == Display.BLOCK
    }

    override fun boxSizing(): BoxSizing {
        return boxSizing
    }

    override fun overflow(): Point<Overflow> {
        return overflow
    }

    override fun scrollbarWidth(): Float {
        return scrollbarWidth
    }

    override fun position(): Position {
        return position
    }

    override fun inset(): Rect<LengthPercentageAuto> {
        return inset
    }

    override fun size(): Size<Dimension> {
        return size
    }

    override fun minSize(): Size<Dimension> {
        return minSize
    }

    override fun maxSize(): Size<Dimension> {
        return maxSize
    }

    override fun aspectRatio(): Option<Float> {
        return aspectRatio
    }

    override fun margin(): Rect<LengthPercentageAuto> {
        return margin
    }

    override fun padding(): Rect<LengthPercentage> {
        return padding
    }

    override fun border(): Rect<LengthPercentage> {
        return border
    }

    companion object : Default<Style> {
        val DEFAULT = Style(
            display = Display.default(),
            itemIsTable = false,
            boxSizing = BoxSizing.BORDER_BOX,
            overflow = Point(x = Overflow.VISIBLE, y = Overflow.VISIBLE),
            scrollbarWidth = 0f,
            position = Position.RELATIVE,
            inset = Rect.autoLPA(),
            margin = Rect.zeroLPA(),
            padding = Rect.zeroLP(),
            border = Rect.zeroLP(),
            size = Size.autoD(),
            minSize = Size.autoD(),
            maxSize = Size.autoD(),
            aspectRatio = Option.None,
            gap = Size.zeroLP(),
            /// Alignment
            alignItems = Option.None,
            alignSelf = Option.None,
            justifyItems = Option.None,
            justifySelf = Option.None,
            alignContent = Option.None,
            justifyContent = Option.None,
            /// Block
            textAlign = TextAlign.AUTO,
            /// Flexbox
            flexDirection = FlexDirection.ROW,
            flexWrap = FlexWrap.NO_WRAP,
            flexGrow = 0f,
            flexShrink = 1f,
            flexBasis = Dimension.AUTO,
            /// Grid
            gridTemplateRows = ArrayList(),
            gridTemplateColumns = ArrayList(),
            gridAutoRows = ArrayList(),
            gridAutoColumns = ArrayList(),
            gridAutoFlow = GridAutoFlow.ROW,
            gridRow = Line(start = GridPlacement.AUTO, end = GridPlacement.AUTO),
            gridColumn = Line(start = GridPlacement.AUTO, end = GridPlacement.AUTO),
        )

        override fun default(): Style {
            return DEFAULT
        }
    }
}
