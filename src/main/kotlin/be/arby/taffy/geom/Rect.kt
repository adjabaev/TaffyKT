package be.arby.taffy.geom

import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.flex.FlexDirection

/**
 * An axis-aligned UI rectangle
 */
data class Rect<T>(
    /**
     * This can represent either the x-coordinate of the starting edge,
     * or the amount of padding on the starting side.
     *
     * The starting edge is the left edge when working with LTR text,
     * and the right edge when working with RTL text.
     */
    var left: T,
    /**
     * This can represent either the x-coordinate of the ending edge,
     * or the amount of padding on the ending side.
     *
     * The ending edge is the right edge when working with LTR text,
     * and the left edge when working with RTL text.
     */
    var right: T,
    /**
     * This can represent either the y-coordinate of the top edge,
     * or the amount of padding on the top side.
     */
    var top: T,
    /**
     * This can represent either the y-coordinate of the bottom edge,
     * or the amount of padding on the bottom side.
     */
    var bottom: T
) {
    /**
     * Applies the function `f` to all four sides of the rect
     *
     * When applied to the left and right sides, the width is used
     * as the second parameter of `f`.
     * When applied to the top or bottom sides, the height is used instead.
     */
    fun <R, U> zipSize(size: Size<U>, f: (T, U) -> R): Rect<R> {
        return Rect(
            left = f(left, size.width),
            right = f(right, size.width),
            top = f(top, size.height),
            bottom = f(bottom, size.height)
        )
    }

    /**
     * Applies the function `f` to the left, right, top, and bottom properties
     *
     * This is used to transform a `Rect<T>` into a `Rect<R>`.
     */
    fun <R> map(f: (T) -> R): Rect<R> {
        return Rect(
            left = f(left),
            right = f(right),
            top = f(top),
            bottom = f(bottom)
        )
    }

    /**
     * Returns a `Line<T>` representing the left and right properties of the Rect
     */
    fun horizontalComponents(): Line<T> {
        return Line(start = left, end = right)
    }

    /**
     * Returns a `Line<T>` containing the top and bottom properties of the Rect
     */
    fun verticalComponents(): Line<T> {
        return Line(start = top, end = bottom)
    }

    /**
     * The `start` or `top` value of the [`Rect`], from the perspective of the main layout axis
     */
    fun mainStart(direction: FlexDirection): T {
        return if (direction.isRow()) {
            left
        } else {
            top
        }
    }

    /**
     * The `end` or `bottom` value of the [`Rect`], from the perspective of the main layout axis
     */
    fun mainEnd(direction: FlexDirection): T {
        return if (direction.isRow()) {
            right
        } else {
            bottom
        }
    }

    /**
     * The `start` or `top` value of the [`Rect`], from the perspective of the cross layout axis
     */
    fun crossStart(direction: FlexDirection): T {
        return if (direction.isRow()) {
            top
        } else {
            left
        }
    }

    /**
     * The `end` or `bottom` value of the [`Rect`], from the perspective of the main layout axis
     */
    fun crossEnd(direction: FlexDirection): T {
        return if (direction.isRow()) {
            bottom
        } else {
            right
        }
    }

    companion object {
        val AUTO = Rect(left = Dimension.AUTO, right = Dimension.AUTO, top = Dimension.AUTO, bottom = Dimension.AUTO)
        val ZERO = Rect(left = 0f, right = 0f, top = 0f, bottom = 0f)

        fun new(start: Float, end: Float, top: Float, bottom: Float): Rect<Float> {
            return Rect(left = start, right = end, top = top, bottom = bottom)
        }

        inline fun <reified T> auto(): Rect<T> {
            if (T::class == Dimension::class) {
                return Rect(left = Dimension.Auto, right = Dimension.Auto, top = Dimension.Auto, bottom = Dimension.Auto) as Rect<T>
            } else {
                throw IllegalArgumentException("Unsupported type: ${T::class}")
            }
        }

        fun fromLength(start: Float, end: Float, top: Float, bottom: Float): Rect<Dimension> {
            return Rect(
                left = Dimension.Length(start),
                right = Dimension.Length(end),
                top = Dimension.Length(top),
                bottom = Dimension.Length(bottom)
            )
        }

        fun fromPercent(start: Float, end: Float, top: Float, bottom: Float): Rect<Dimension> {
            return Rect(
                left = Dimension.Percent(start),
                right = Dimension.Percent(end),
                top = Dimension.Percent(top),
                bottom = Dimension.Percent(bottom)
            )
        }

        inline fun <reified T> zero(): Rect<T> {
            if (T::class == Float::class) {
                return Rect(
                    left = 0f,
                    right = 0f,
                    top = 0f,
                    bottom = 0f
                ) as Rect<T>
            } else if (T::class == Option::class) {
                return Rect(
                    left = Option.Some(0f),
                    right = Option.Some(0f),
                    top = Option.Some(0f),
                    bottom = Option.Some(0f)
                ) as Rect<T>
            } else if (T::class == LengthPercentage::class) {
                return Rect(
                    left = LengthPercentage.Length(0f),
                    right = LengthPercentage.Length(0f),
                    top = LengthPercentage.Length(0f),
                    bottom = LengthPercentage.Length(0f)
                ) as Rect<T>
            } else if (T::class == LengthPercentageAuto::class) {
                return Rect(
                    left = LengthPercentageAuto.Length(0f),
                    right = LengthPercentageAuto.Length(0f),
                    top = LengthPercentageAuto.Length(0f),
                    bottom = LengthPercentageAuto.Length(0f)
                ) as Rect<T>
            } else {
                throw IllegalArgumentException("Unsupported type: ${T::class}")
            }
        }

        fun zeroOF(): Rect<Option<Float>> {
            return Rect(
                left = Option.Some(0f),
                right = Option.Some(0f),
                top = Option.Some(0f),
                bottom = Option.Some(0f)
            )
        }

        fun autoLPA(): Rect<LengthPercentageAuto> {
            return Rect(
                left = LengthPercentageAuto.Auto,
                right = LengthPercentageAuto.Auto,
                top = LengthPercentageAuto.Auto,
                bottom = LengthPercentageAuto.Auto
            )
        }

        fun zeroLPA(): Rect<LengthPercentageAuto> {
            return Rect(
                left = LengthPercentageAuto.Length(0f),
                right = LengthPercentageAuto.Length(0f),
                top = LengthPercentageAuto.Length(0f),
                bottom = LengthPercentageAuto.Length(0f)
            )
        }

        fun zeroLP(): Rect<LengthPercentage> {
            return Rect(
                left = LengthPercentage.Length(0f),
                right = LengthPercentage.Length(0f),
                top = LengthPercentage.Length(0f),
                bottom = LengthPercentage.Length(0f)
            )
        }
    }
}

/// DIMENSION VARIANTS

@JvmName("resolveOrZeroOF")
fun Rect<Dimension>.resolveOrZero(context: Option<Float>): Rect<Float> {
    return Rect(
        left = left.resolveOrZero(context),
        right = right.resolveOrZero(context),
        top = top.resolveOrZero(context),
        bottom = bottom.resolveOrZero(context)
    )
}

@JvmName("resolveOrZeroSOF")
fun Rect<Dimension>.resolveOrZero(context: Size<Option<Float>>): Rect<Float> {
    return Rect(
        left = left.resolveOrZero(context.width),
        right = right.resolveOrZero(context.width),
        top = top.resolveOrZero(context.height),
        bottom = bottom.resolveOrZero(context.height)
    )
}

/// FLOAT VARIANTS

operator fun Rect<Float>.plus(rhs: Rect<Float>): Rect<Float> {
    return Rect(
        left = left + rhs.left,
        right = right + rhs.right,
        top = top + rhs.top,
        bottom = bottom + rhs.bottom
    )
}

/**
 * The sum of [`Rect.start`](Rect) and [`Rect.end`](Rect)
 *
 * This is typically used when computing total padding.
 *
 * **NOTE:** this is *not* the width of the rectangle.
 */
fun Rect<Float>.horizontalAxisSum(): Float {
    return left + right
}

/**
 * The sum of [`Rect.top`](Rect) and [`Rect.bottom`](Rect)
 *
 * This is typically used when computing total padding.
 *
 * **NOTE:** this is *not* the height of the rectangle.
 */
fun Rect<Float>.verticalAxisSum(): Float {
    return top + bottom
}

/**
 * Both horizontal_axis_sum and vertical_axis_sum as a Size<T>
 *
 * **NOTE:** this is *not* the width/height of the rectangle.
 */
fun Rect<Float>.sumAxes(): Size<Float> {
    return Size(width = horizontalAxisSum(), height = verticalAxisSum())
}

/**
 * The sum of the two fields of the [`Rect`] representing the main axis.
 *
 * This is typically used when computing total padding.
 *
 * If the [`FlexDirection`] is [`FlexDirection::Row`] or [`FlexDirection::RowReverse`], this is [`Rect::horizontal`].
 * Otherwise, this is [`Rect::vertical`].
 */
fun Rect<Float>.mainAxisSum(direction: FlexDirection): Float {
    return if (direction.isRow()) {
        horizontalAxisSum()
    } else {
        verticalAxisSum()
    }
}

/**
 * The sum of the two fields of the [`Rect`] representing the cross axis.
 *
 * If the [`FlexDirection`] is [`FlexDirection::Row`] or [`FlexDirection::RowReverse`], this is [`Rect::vertical`].
 * Otherwise, this is [`Rect::horizontal`].
 */
fun Rect<Float>.crossAxisSum(direction: FlexDirection): Float {
    return if (direction.isRow()) {
        verticalAxisSum()
    } else {
        horizontalAxisSum()
    }
}

/// LENGTH_PERCENTAGE VARIANTS

/**
 * Converts any `parent`-relative values for Rect into an absolute Rect
 */
@JvmName("resolveOrZeroLP")
fun Rect<LengthPercentage>.resolveOrZero(context: Option<Float>): Rect<Float> {
    return Rect (
        left = left.resolveOrZero(context),
        right = right.resolveOrZero(context),
        top = top.resolveOrZero(context),
        bottom = bottom.resolveOrZero(context)
    )
}

/**
 * Converts any `parent`-relative values for Rect into an absolute Rect
 */
fun Rect<LengthPercentage>.resolveOrZero(context: Size<Option<Float>>): Rect<Float> {
    return Rect (
        left = left.resolveOrZero(context.width),
        right = right.resolveOrZero(context.width),
        top = top.resolveOrZero(context.height),
        bottom = bottom.resolveOrZero(context.height)
    )
}

/// LENGTH_PERCENTAGE_AUTO VARIANTS

/**
 * Converts any `parent`-relative values for Rect into an absolute Rect
 */
@JvmName("resolveOrZeroLPA")
fun Rect<LengthPercentageAuto>.resolveOrZero(context: Option<Float>): Rect<Float> {
    return Rect (
        left = left.resolveOrZero(context),
        right = right.resolveOrZero(context),
        top = top.resolveOrZero(context),
        bottom = bottom.resolveOrZero(context)
    )
}
