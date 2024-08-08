package be.arby.taffy.style

import be.arby.taffy.geom.Point
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.dimension.LengthPercentageAuto

/**
 * The core set of styles that are shared between all CSS layout nodes
 *
 * Note that all methods come with a default implementation which simply returns the default value for that style property
 * but this is a just a convenience to save on boilerplate for styles that your implementation doesn't support. You will need
 * to override the default implementation for each style property that your style type actually supports.
 */
interface CoreStyle {
    /**
     * Which box generation mode should be used
     */
    fun boxGenerationMode(): BoxGenerationMode {
        return BoxGenerationMode.DEFAULT
    }

    /**
     * Is block layout?
     */
    fun isBlock(): Boolean {
        return false
    }

    /**
     * Which box do size styles apply to
     */
    fun boxSizing(): BoxSizing {
        return BoxSizing.BORDER_BOX
    }

    /// Overflow properties

    /**
     * How children overflowing their container should affect layout
     */
    fun overflow(): Point<Overflow> {
        return Style.DEFAULT.overflow
    }

    /**
     * How much space (in points) should be reserved for the scrollbars of `Overflow::Scroll` and `Overflow::Auto` nodes.
     */
    fun scrollbarWidth(): Float {
        return 0f
    }

    /// Position properties

    /**
     * What should the `position` value of this struct use as a base offset?
     */
    fun position(): Position {
        return Style.DEFAULT.position
    }

    /**
     * How should the position of this element be tweaked relative to the layout defined?
     */
    fun inset(): Rect<LengthPercentageAuto> {
        return Style.DEFAULT.inset
    }

    /// Size properies

    /**
     * Sets the initial size of the item
     */
    fun size(): Size<Dimension> {
        return Style.DEFAULT.size
    }

    /**
     * Controls the minimum size of the item
     */
    fun minSize(): Size<Dimension> {
        return Style.DEFAULT.minSize
    }

    /**
     * Controls the maximum size of the item
     */
    fun maxSize(): Size<Dimension> {
        return Style.DEFAULT.maxSize
    }

    /**
     * Sets the preferred aspect ratio for the item
     * The ratio is calculated as width divided by height.
     */
    fun aspectRatio(): Option<Float> {
        return Style.DEFAULT.aspectRatio
    }

    /// Spacing Properties

    /**
     * How large should the margin be on each side?
     */
    fun margin(): Rect<LengthPercentageAuto> {
        return Style.DEFAULT.margin
    }

    /**
     * How large should the padding be on each side?
     */
    fun padding(): Rect<LengthPercentage> {
        return Style.DEFAULT.padding
    }

    /**
     * How large should the border be on each side?
     */
    fun border(): Rect<LengthPercentage> {
        return Style.DEFAULT.border
    }
}
