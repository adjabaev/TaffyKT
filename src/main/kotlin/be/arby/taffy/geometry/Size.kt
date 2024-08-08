package be.arby.taffy.geometry

import be.arby.taffy.lang.Option
import be.arby.taffy.maths.axis.AbsoluteAxis
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.flex.FlexDirection

data class Size<T>(var width: T, var height: T) {
    fun <R> map(f: (T) -> R): Size<R> {
        return Size(
            width = f(width), height = f(height)
        )
    }

    fun mapWidth(f: (T) -> T): Size<T> {
        return Size(
            width = f(width), height = height
        )
    }

    fun mapHeight(f: (T) -> T): Size<T> {
        return Size(
            width = width, height = f(height)
        )
    }

    fun <U, R> zipMap(size: Size<U>, f: (T, U) -> R): Size<R> {
        return Size(
            width = f(width, size.width), height = f(height, size.height)
        )
    }

    fun setMain(direction: FlexDirection, value: T) {
        if (direction.isRow()) {
            this.width = value
        } else {
            this.height = value
        }
    }

    fun setCross(direction: FlexDirection, value: T) {
        if (direction.isRow()) {
            this.height = value
        } else {
            this.width = value
        }
    }

    fun main(direction: FlexDirection): T {
        return if (direction.isRow()) {
            this.width
        } else {
            this.height
        }
    }

    fun cross(direction: FlexDirection): T {
        return if (direction.isRow()) {
            this.height
        } else {
            this.width
        }
    }

    fun get(axis: AbstractAxis): T {
        return when (axis) {
            AbstractAxis.INLINE -> width
            AbstractAxis.BLOCK -> height
        }
    }

    fun set(axis: AbstractAxis, value: T) {
        when (axis) {
            AbstractAxis.INLINE -> width = value
            AbstractAxis.BLOCK -> height = value
        }
    }

    fun getAbs(axis: AbsoluteAxis): T {
        return when (axis) {
            AbsoluteAxis.HORIZONTAL -> width
            AbsoluteAxis.VERTICAL -> height
        }
    }

    companion object {
        @JvmField
        val MIN_CONTENT: Size<AvailableSpace> = Size(AvailableSpace.MinContent, AvailableSpace.MinContent)
        @JvmField
        val MAX_CONTENT: Size<AvailableSpace> = Size(AvailableSpace.MaxContent, AvailableSpace.MaxContent)

        @JvmStatic
        fun new(width: Float, height: Float): Size<Option<Float>> {
            return Size(width = Option.Some(width), height = Option.Some(height));
        }

        @JvmStatic
        fun fromPoints(width: Float, height: Float): Size<Dimension> {
            return Size(width = Dimension.Length(width), height = Dimension.Length(height))
        }

        @JvmStatic
        fun fromPercent(width: Float, height: Float): Size<Dimension> {
            return Size(width = Dimension.Percent(width), height = Dimension.Percent(height))
        }

        @JvmStatic
        fun fromPointsLP(v: Float): Size<LengthPercentage> {
            return Size(width = LengthPercentage.Length(v), height = LengthPercentage.Length(v))
        }

        fun autoD(): Size<Dimension> {
            return Size(Dimension.Auto, Dimension.Auto)
        }

        fun zeroLP(): Size<LengthPercentage> {
            return Size(LengthPercentage.Length(0f), LengthPercentage.Length(0f))
        }

        fun zeroF(): Size<Float> {
            return Size(0f, 0f)
        }

        fun zeroOF(): Size<Option<Float>> {
            return Size(Option.Some(0f), Option.Some(0f))
        }

        fun none(): Size<Option<Float>> {
            return Size(Option.None, Option.None)
        }
    }
}
