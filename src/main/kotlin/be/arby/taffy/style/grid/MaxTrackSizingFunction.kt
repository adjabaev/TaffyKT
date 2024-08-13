package be.arby.taffy.style.grid

import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.LengthPercentage

/**
 * Maximum track sizing function
 *
 * Specifies the maximum size of a grid track. A grid track will automatically size between it's minimum and maximum size based
 * on the size of it's contents, the amount of available space, and the sizing constraint the grid is being size under.
 * See <https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-columns>
 */
sealed class MaxTrackSizingFunction {
    /**
     * Track maximum size should be a fixed length or percentage value
     */
    data class Fixed(val l: LengthPercentage) : MaxTrackSizingFunction()

    /**
     * Track maximum size should be content sized under a min-content constraint
     */
    data object MinContent : MaxTrackSizingFunction()

    /**
     * Track maximum size should be content sized under a max-content constraint
     */
    data object MaxContent : MaxTrackSizingFunction()

    /**
     * Track maximum size should be sized according to the fit-content formula
     */
    data class FitContent(val l: LengthPercentage) : MaxTrackSizingFunction()

    /**
     * Track maximum size should be automatically sized
     */
    data object Auto : MaxTrackSizingFunction()

    /**
     * The dimension as a fraction of the total available grid space (`fr` units in CSS)
     * Specified value is the numerator of the fraction. Denominator is the sum of all fraction specified in that grid dimension
     * Spec: <https://www.w3.org/TR/css3-grid-layout/#fr-unit>
     */
    data class Fraction(val f: Float) : MaxTrackSizingFunction()

    /**
     * Returns true if the max track sizing function is `MinContent`, `MaxContent`, `FitContent` or `Auto`, else false.
     */
    fun isIntrinsic(): Boolean {
        return this is MinContent || this is MaxContent || this is FitContent || this is Auto
    }

    /**
     * Returns true if the max track sizing function is `MaxContent`, `FitContent` or `Auto` else false.
     * "In all cases, treat auto and fit-content() as max-content, except where specified otherwise for fit-content()."
     * See: <https://www.w3.org/TR/css-grid-1/#algo-terms>
     */
    fun isMaxContentAlike(): Boolean {
        return this is MaxContent || this is FitContent || this is Auto
    }

    /**
     * Returns true if the max track sizing function is `Flex`, else false.
     */
    fun isFlexible(): Boolean {
        return this is Fraction
    }

    /**
     * Returns fixed point values directly. Attempts to resolve percentage values against
     * the passed available_space and returns if this results in a concrete value (which it
     * will if the available_space is `Some`). Otherwise returns None.
     */
    fun definiteValue(parentSize: Option<Float>): Option<Float> {
        return when {
            this is Fixed && this.l is LengthPercentage.Length -> Option.Some(this.l.f)
            this is Fixed && this.l is LengthPercentage.Percent -> parentSize.map { size -> this.l.f * size }

            else -> Option.None
        }
    }

    /**
     * Resolve the maximum size of the track as defined by either:
     *     - A fixed track sizing function
     *     - A percentage track sizing function (with definite available space)
     *     - A fit-content sizing function with fixed argument
     *     - A fit-content sizing function with percentage argument (with definite available space)
     * All other kinds of track sizing function return None.
     */
    fun definiteLimit(parentSize: Option<Float>): Option<Float> {
        return when {
            this is FitContent && l is LengthPercentage.Length -> Option.Some(l.f)
            this is FitContent && l is LengthPercentage.Percent -> parentSize.map { size -> l.f * size }
            else -> definiteValue(parentSize)
        }
    }

    /**
     * Resolve percentage values against the passed parent_size, returning Some(value)
     * Non-percentage values always return None.
     */
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
        return this is Fixed && this.l is LengthPercentage.Percent ||
                this is FitContent && this.l is LengthPercentage.Percent
    }

    companion object {
        val MIN_CONTENT: MaxTrackSizingFunction = MinContent
        val MAX_CONTENT: MaxTrackSizingFunction = MaxContent

        fun fitContent(argument: LengthPercentage): MaxTrackSizingFunction {
            return FitContent(argument)
        }

        fun fromLength(points: Float): MaxTrackSizingFunction {
            return Fixed(LengthPercentage.fromLength(points))
        }

        fun fromPercent(percent: Float): MaxTrackSizingFunction {
            return Fixed(LengthPercentage.fromPercent(percent))
        }

        fun fromFlex(flex: Float): MaxTrackSizingFunction {
            return Fraction(flex)
        }
    }
}
