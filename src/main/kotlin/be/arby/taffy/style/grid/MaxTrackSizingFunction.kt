package be.arby.taffy.style.grid

import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.utils.Into

sealed class MaxTrackSizingFunction {
    data class Fixed(val l: LengthPercentage) : MaxTrackSizingFunction()
    object MinContent : MaxTrackSizingFunction()
    object MaxContent : MaxTrackSizingFunction()
    data class FitContent(val l: LengthPercentage) : MaxTrackSizingFunction()
    object Auto : MaxTrackSizingFunction()
    data class Flex(val f: Float) : MaxTrackSizingFunction()

    /**
     * Returns true if the max track sizing function is `MinContent`, `MaxContent`, `FitContent` or `Auto`, else false.
     */
    fun isIntrinsic(): Boolean {
        return this is MinContent || this is MaxContent || this is FitContent || this is Auto
    }

    fun isMaxContentAlike(): Boolean {
        return this is MaxContent || this is FitContent || this is Auto
    }

    fun isFlexible(): Boolean {
        return this is Flex
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

    fun definiteLimit(parentSize: Option<Float>): Option<Float> {
        return when {
            this is FitContent && l is LengthPercentage.Length -> Option.Some(l.f)
            this is FitContent && l is LengthPercentage.Percent -> parentSize.map { size -> l.f * size }
            else -> definiteValue(parentSize)
        }
    }

    /**
     * Whether the track sizing functions depends on the size of the parent node
     */
    fun usesPercentage(): Boolean {
        if (
            this is Fixed && this.l is LengthPercentage.Percent ||
            this is FitContent && this.l is LengthPercentage.Percent
        ) {
            return true
        }
        return false
    }

    companion object {
        val MIN_CONTENT: MaxTrackSizingFunction = MinContent
        val MAX_CONTENT: MaxTrackSizingFunction = MaxContent

        @JvmStatic
        fun fitContent(argument: LengthPercentage): MaxTrackSizingFunction {
            return FitContent(argument)
        }

        @JvmStatic
        fun fromPoints(points: Into<Float>): MaxTrackSizingFunction {
            return fromPoints(points.into())
        }

        @JvmStatic
        fun fromPoints(points: Float): MaxTrackSizingFunction {
            return Fixed(LengthPercentage.fromLength(points))
        }

        @JvmStatic
        fun fromPercent(percent: Into<Float>): MaxTrackSizingFunction {
            return fromPercent(percent.into())
        }

        @JvmStatic
        fun fromPercent(percent: Float): MaxTrackSizingFunction {
            return Fixed(LengthPercentage.fromPercent(percent))
        }

        @JvmStatic
        fun fromFlex(flex: Into<Float>): MaxTrackSizingFunction {
            return fromFlex(flex.into())
        }

        @JvmStatic
        fun fromFlex(flex: Float): MaxTrackSizingFunction {
            return Flex(flex)
        }
    }
}
