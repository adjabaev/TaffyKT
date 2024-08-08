package be.arby.taffy.style

import be.arby.taffy.lang.Default

/**
 * The positioning strategy for this item.
 *
 * This controls both how the origin is determined for the [`Style::position`] field,
 * and whether or not the item will be controlled by flexbox's layout algorithm.
 *
 * WARNING: this enum follows the behavior of [CSS's `position` property](https://developer.mozilla.org/en-US/docs/Web/CSS/position),
 * which can be unintuitive.
 *
 * [`Position::Relative`] is the default value, in contrast to the default behavior in CSS.
 */
enum class Position {
    /**
     * The offset is computed relative to the final position given by the layout algorithm.
     * Offsets do not affect the position of any other items; they are effectively a correction factor applied at the end.
     */
    RELATIVE,

    /**
     * The offset is computed relative to this item's closest positioned ancestor, if any.
     * Otherwise, it is placed relative to the origin.
     * No space is created for the item in the page layout, and its size will not be altered.
     *
     * WARNING: to opt-out of layouting entirely, you must use [`Display::None`] instead on your [`Style`] object.
     */
    ABSOLUTE;

    companion object: Default<Position> {
        override fun default(): Position {
            return RELATIVE
        }
    }
}
