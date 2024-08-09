package be.arby.taffy.geom

import be.arby.taffy.lang.From
import be.arby.taffy.lang.Option
import be.arby.taffy.style.flex.FlexDirection

/**
 * A 2-dimensional coordinate.
 *
 * When used in association with a [`Rect`], represents the top-left corner.
 */
data class Point<T>(
    /**
     * The x-coordinate
     */
    var x: T,
    /**
     * The y-coordinate
     */
    var y: T
) {
    /**
     * Applies the function `f` to both the x and y
     *
     * This is used to transform a `Point<T>` into a `Point<R>`.
     */
    fun <R> map(f: (T) -> R): Point<R> {
        return Point(x = f(x), y = f(y))
    }

    /**
     * Gets the extent of the specified layout axis
     * Whether this is the width or height depends on the `GridAxis` provided
     */
    fun getAxis(axis: AbstractAxis): T {
        return when (axis) {
            AbstractAxis.INLINE -> x
            AbstractAxis.BLOCK -> y
        }
    }

    /**
     * Swap x and y components
     */
    fun transpose(): Point<T> {
        return Point(x = y, y = x)
    }

    /**
     * Sets the extent of the specified layout axis
     * Whether this is the width or height depends on the `GridAxis` provided
     */
    fun setAxis(axis: AbstractAxis, value: T) {
        when (axis) {
            AbstractAxis.INLINE -> x = value
            AbstractAxis.BLOCK -> y = value
        }
    }

    /**
     * Gets the component in the main layout axis
     *
     * Whether this is the x or y depends on the `direction` provided
     */
    fun main(direction: FlexDirection): T {
        return if (direction.isRow()) {
            x
        } else {
            y
        }
    }

    /**
     * Gets the component in the cross layout axis
     *
     * Whether this is the x or y depends on the `direction` provided
     */
    fun cross(direction: FlexDirection): T {
        return if (direction.isRow()) {
            y
        } else {
            x
        }
    }

    companion object {
        /**
         * A [Point] with values (0, 0), representing the origin
         */
        val ZERO = Point(x = 0f, y = 0f)
        /**
         * A [Point] with values (None, None)
         */
        val NONE: Point<Option<Float>> = Point(x = Option.None, y = Option.None)
    }
}

/// FLOAT VARIANTS
operator fun Point<Float>.plus(rhs: Point<Float>): Point<Float> {
    return Point(x = x + rhs.x, y = y + rhs.y)
}

fun Point<Option<Float>>.into(): Size<Option<Float>> {
    return Size(width = x, height = y)
}

fun Point<Float>.into(): Size<Float> {
    return Size(width = x, height = y)
}
