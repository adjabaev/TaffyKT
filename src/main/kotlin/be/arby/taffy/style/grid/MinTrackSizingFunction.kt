package be.arby.taffy.style.grid

import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.utils.Into

sealed class MinTrackSizingFunction {
    data class Fixed(val l: LengthPercentage) : MinTrackSizingFunction()
    object MinContent : MinTrackSizingFunction()
    object MaxContent : MinTrackSizingFunction()
    object Auto : MinTrackSizingFunction()

    /**
     * Returns true if the min track sizing function is `MinContent`, `MaxContent` or `Auto`, else false.
     */
    fun isIntrinsic(): Boolean {
        return this is MinContent || this is MaxContent || this is Auto
    }

    fun definiteValue(parentSize: Option<Float>): Option<Float> {
        return when {
            this is Fixed && this.l is LengthPercentage.Length -> Option.Some(this.l.f)
            this is Fixed && this.l is LengthPercentage.Percent -> parentSize.map { size -> this.l.f * size }

            else -> Option.None
        }
    }

    fun resolvedPercentageSize(parentSize: Float): Option<Float> {
        return when {
            this is Fixed && this.l is LengthPercentage.Percent -> Option.Some(this.l.f * parentSize)
            else -> Option.None
        }
    }

    /**
     * Whether the track sizing functions depends on the size of the parent node
     */
    fun usesPercentage(): Boolean {
        return this is Fixed && this.l is LengthPercentage.Percent
    }

    companion object {
        val MIN_CONTENT: MinTrackSizingFunction = MinContent
        val MAX_CONTENT: MinTrackSizingFunction = MaxContent

        @JvmStatic
        fun fromPoints(points: Into<Float>): MinTrackSizingFunction {
            return fromPoints(points.into())
        }

        @JvmStatic
        fun fromPoints(points: Float): MinTrackSizingFunction {
            return Fixed(LengthPercentage.fromLength(points))
        }

        @JvmStatic
        fun fromPercent(percent: Into<Float>): MinTrackSizingFunction {
            return fromPercent(percent.into())
        }

        @JvmStatic
        fun fromPercent(percent: Float): MinTrackSizingFunction {
            return Fixed(LengthPercentage.fromPercent(percent))
        }
    }
}
