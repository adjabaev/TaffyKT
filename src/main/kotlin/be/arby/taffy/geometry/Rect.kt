package be.arby.taffy.geometry

import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.flex.FlexDirection

data class Rect<T>(var left: T, var right: T, var top: T, var bottom: T) {
    internal fun <U, R> zipSize(size: Size<U>, f: (T, U) -> R): Rect<R> {
        return Rect(
            left = f(left, size.width),
            right = f(right, size.width),
            top = f(top, size.height),
            bottom = f(bottom, size.height)
        )
    }

    internal fun <R> map(f: (T) -> R): Rect<R> {
        return Rect(
            left = f(left),
            right = f(right),
            top = f(top),
            bottom = f(bottom)
        )
    }

    internal fun horizontalComponents(): Line<T> {
        return Line(
            start = left,
            end = right
        )
    }

    internal fun verticalComponents(): Line<T> {
        return Line(
            start = top,
            end = bottom
        )
    }

    internal fun mainStart(direction: FlexDirection): T {
        return if (direction.isRow()) left else top
    }

    internal fun mainEnd(direction: FlexDirection): T {
        return if (direction.isRow()) right else bottom
    }

    internal fun crossStart(direction: FlexDirection): T {
        return if (direction.isRow()) top else left
    }

    internal fun crossEnd(direction: FlexDirection): T {
        return if (direction.isRow()) bottom else right
    }

    companion object {
        @JvmStatic
        fun new(start: Float, end: Float, top: Float, bottom: Float): Rect<Float> {
            return Rect(left = start, right = end, top = top, bottom = bottom)
        }

        @JvmStatic
        fun fromPoints(start: Float, end: Float, top: Float, bottom: Float): Rect<Dimension> {
            return Rect(
                left = Dimension.Length(start),
                right = Dimension.Length(end),
                top = Dimension.Length(top),
                bottom = Dimension.Length(bottom)
            )
        }

        @JvmStatic
        fun fromPercent(start: Float, end: Float, top: Float, bottom: Float): Rect<Dimension> {
            return Rect(
                left = Dimension.Percent(start),
                right = Dimension.Percent(end),
                top = Dimension.Percent(top),
                bottom = Dimension.Percent(bottom)
            )
        }

        fun autoLPA(): Rect<LengthPercentageAuto> {
            return Rect(
                LengthPercentageAuto.Auto,
                LengthPercentageAuto.Auto,
                LengthPercentageAuto.Auto,
                LengthPercentageAuto.Auto
            )
        }

        fun zeroLPA(): Rect<LengthPercentageAuto> {
            return Rect(
                LengthPercentageAuto.Length(0f),
                LengthPercentageAuto.Length(0f),
                LengthPercentageAuto.Length(0f),
                LengthPercentageAuto.Length(0f)
            )
        }

        fun zeroLP(): Rect<LengthPercentage> {
            return Rect(
                LengthPercentage.Length(0f),
                LengthPercentage.Length(0f),
                LengthPercentage.Length(0f),
                LengthPercentage.Length(0f)
            )
        }

        fun zeroF(): Rect<Float> {
            return Rect(
                0f,
                0f,
                0f,
                0f
            )
        }
    }
}
