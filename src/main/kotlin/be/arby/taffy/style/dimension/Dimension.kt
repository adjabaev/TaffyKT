package be.arby.taffy.style.dimension

import be.arby.taffy.lang.DoubleFrom
import be.arby.taffy.lang.From
import be.arby.taffy.lang.Option
import be.arby.taffy.resolve.MaybeResolve
import be.arby.taffy.resolve.ResolveOrZero
import be.arby.taffy.style.dimension.LengthPercentageAuto.Auto
import be.arby.taffy.style.dimension.LengthPercentageAuto.Length

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

    companion object: DoubleFrom<LengthPercentage, LengthPercentageAuto, Dimension> {
        val ZERO = Length(0f)
        val AUTO = Auto

        fun fromLength(value: Float): Length {
            return Length(value)
        }

        fun fromPercent(percent: Float): Percent {
            return Percent(percent)
        }

        override fun from(value: LengthPercentage): Dimension {
            return when (value) {
                is LengthPercentage.Length -> Length(value.f)
                is LengthPercentage.Percent -> Percent(value.f)
            }
        }

        override fun from(value: LengthPercentageAuto): Dimension {
            return when (value) {
                is LengthPercentageAuto.Length -> Length(value.f)
                is LengthPercentageAuto.Percent -> Percent(value.f)
                is LengthPercentageAuto.Auto -> Auto
            }
        }
    }
}
