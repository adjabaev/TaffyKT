package be.arby.taffy.style

import be.arby.taffy.lang.Option

/**
 * How children overflowing their container should affect layout
 *
 * In CSS the primary effect of this property is to control whether contents of a parent container that overflow that container should
 * be displayed anyway, be clipped, or trigger the container to become a scroll container. However it also has secondary effects on layout,
 * the main ones being:
 *
 *   - The automatic minimum size Flexbox/CSS Grid items with non-`Visible` overflow is `0` rather than being content based
 *   - `Overflow::Scroll` nodes have space in the layout reserved for a scrollbar (width controlled by the `scrollbar_width` property)
 *
 * In Taffy, we only implement the layout related secondary effects as we are not concerned with drawing/painting. The amount of space reserved for
 * a scrollbar is controlled by the `scrollbar_width` property. If this is `0` then `Scroll` behaves identically to `Hidden`.
 *
 * <https://developer.mozilla.org/en-US/docs/Web/CSS/overflow>
 */
enum class Overflow {
    /**
     * The automatic minimum size of this node as a flexbox/grid item should be based on the size of its content.
     * Content that overflows this node *should* contribute to the scroll region of its parent.
     */
    VISIBLE,

    /**
     * The automatic minimum size of this node as a flexbox/grid item should be based on the size of its content.
     * Content that overflows this node should *not* contribute to the scroll region of its parent.
     */
    CLIP,

    /**
     * The automatic minimum size of this node as a flexbox/grid item should be `0`.
     * Content that overflows this node should *not* contribute to the scroll region of its parent.
     */
    HIDDEN,

    /**
     * The automatic minimum size of this node as a flexbox/grid item should be `0`. Additionally, space should be reserved
     * for a scrollbar. The amount of space reserved is controlled by the `scrollbar_width` property.
     * Content that overflows this node should *not* contribute to the scroll region of its parent.
     */
    SCROLL;

    /**
     * Returns true for overflow modes that contain their contents (`Overflow::Hidden`, `Overflow::Scroll`, `Overflow::Auto`)
     * or else false for overflow modes that allow their contains to spill (`Overflow::Visible`).
     */
    fun isScrollContainer(): Boolean {
        return when(this) {
            VISIBLE, CLIP -> false
            HIDDEN, SCROLL -> true
        }
    }

    /**
     * Returns `Some(0f)` if the overflow mode would cause the automatic minimum size of a Flexbox or CSS Grid item
     * to be `0`. Else returns None.
     */
    fun maybeIntoAutomaticMinSize(): Option<Float> {
        return when (this.isScrollContainer()) {
            true -> Option.Some(0f)
            false -> Option.None
        }
    }
}
