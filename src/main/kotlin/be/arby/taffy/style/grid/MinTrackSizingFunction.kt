package be.arby.taffy.style.grid

import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.LengthPercentage

/**
 * Minimum track sizing function
 * Specifies the minimum size of a grid track. A grid track will automatically size between it's minimum and maximum size based
 * on the size of it's contents, the amount of available space, and the sizing constraint the grid is being size under.
 * See <https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-columns>
 */
sealed class MinTrackSizingFunction {
    /**
     * Track minimum size should be a fixed length or percentage value
     */
    data class Fixed(val l: LengthPercentage) : MinTrackSizingFunction()

    /**
     * Track minimum size should be content sized under a min-content constraint
     */
    data object MinContent : MinTrackSizingFunction()

    /**
     * Track minimum size should be content sized under a max-content constraint
     */
    data object MaxContent : MinTrackSizingFunction()

    /**
     * Track minimum size should be automatically sized
     */
    data object Auto : MinTrackSizingFunction()

    /**
     * Returns true if the min track sizing function is `MinContent`, `MaxContent` or `Auto`, else false.
     */
    fun isIntrinsic(): Boolean {
        return this is MinContent || this is MaxContent || this is Auto
    }

    /**
     * Returns fixed point values directly. Attempts to resolve percentage values against
     * the passed available_space and returns if this results in a concrete value (which it
     * will if the available_space is `Some`). Otherwise returns `None`.
     */
    fun definiteValue(parentSize: Option<Float>): Option<Float> {
        return when {
            this is Fixed && this.l is LengthPercentage.Length -> Option.Some(this.l.f)
            this is Fixed && this.l is LengthPercentage.Percent -> parentSize.map { size -> this.l.f * size }

            else -> Option.None
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
        return this is Fixed && this.l is LengthPercentage.Percent
    }

    companion object {
        val MIN_CONTENT: MinTrackSizingFunction = MinContent
        val MAX_CONTENT: MinTrackSizingFunction = MaxContent

        fun fromLength(points: Float): MinTrackSizingFunction {
            return Fixed(LengthPercentage.fromLength(points))
        }

        fun fromPercent(percent: Float): MinTrackSizingFunction {
            return Fixed(LengthPercentage.fromPercent(percent))
        }
    }
}
