package be.arby.taffy.style.dimension

import be.arby.taffy.lang.DoubleFrom
import be.arby.taffy.lang.Option

/**
 * A unit of linear measurement
 *
 * This is commonly combined with [`Rect`], [`Point`](crate::geometry::Point) and [`Size<T>`].
 */
sealed class Dimension {
    /**
     *  An absolute length in some abstract units. Users of Taffy may define what they correspond
     *  to in their application (pixels, logical pixels, mm, etc) as they see fit.
     */
    data class Length(val f: Float) : Dimension()

    /**
     * The dimension is stored in percentage relative to the parent item.
     */
    data class Percent(val f: Float) : Dimension()

    /**
     * The dimension should be automatically computed
     */
    data object Auto : Dimension()

    /**
     * Get Length value if value is Length variant
     */
    fun intoOption(): Option<Float> {
        return when (this) {
            is Length -> Option.Some(this.f)
            else -> Option.None
        }
    }

    fun maybeResolve(context: Option<Float>): Option<Float> {
        return when (this) {
            is Length -> Option.Some(this.f)
            is Percent -> context.map { dim -> dim * this.f }
            is Auto -> Option.None
        }
    }

    fun maybeResolve(context: Float): Option<Float> {
        return maybeResolve(Option.Some(context))
    }

    /**
     * Will return a default value of result is evaluated to `None`
     */
    fun resolveOrZero(context: Option<Float>): Float {
        return maybeResolve(context).unwrapOr(0f)
    }

    companion object: DoubleFrom<LengthPercentage, LengthPercentageAuto, Dimension> {
        val ZERO = Length(0f)
        val AUTO = Auto

        fun fromLength(value: Float): Length {
            return Length(value)
        }

        fun fromPercent(percent: Float): Percent {
            return Percent(percent)
        }

        override fun from1(value: LengthPercentage): Dimension {
            return when (value) {
                is LengthPercentage.Length -> Length(value.f)
                is LengthPercentage.Percent -> Percent(value.f)
            }
        }

        override fun from2(value: LengthPercentageAuto): Dimension {
            return when (value) {
                is LengthPercentageAuto.Length -> Length(value.f)
                is LengthPercentageAuto.Percent -> Percent(value.f)
                is LengthPercentageAuto.Auto -> Auto
            }
        }
    }
}
