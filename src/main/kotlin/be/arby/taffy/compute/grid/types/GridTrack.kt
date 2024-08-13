package be.arby.taffy.compute.grid.types

import be.arby.taffy.lang.Option
import be.arby.taffy.lang.f32Min
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.grid.MaxTrackSizingFunction
import be.arby.taffy.style.grid.MinTrackSizingFunction
import be.arby.taffy.util.max

/**
 * Internal sizing information for a single grid track (row/column)
 * Gutters between tracks are sized similarly to actual tracks, so they
 * are also represented by this struct
 */
data class GridTrack(
    /**
     * Whether the track is a full track, a gutter, or a placeholder that has not yet been initialised
     */
    val kind: GridTrackKind,

    /**
     * Whether the track is a collapsed track/gutter. Collapsed tracks are effectively treated as if
     * they don't exist for the purposes of grid sizing. Gutters between collapsed tracks are also collapsed.
     */
    var isCollapsed: Boolean,

    /**
     * The minimum track sizing function of the track
     */
    var minTrackSizingFunction: MinTrackSizingFunction,

    /**
     * The maximum track sizing function of the track
     */
    var maxTrackSizingFunction: MaxTrackSizingFunction,

    /**
     * The distance of the start of the track from the start of the grid container
     */
    var offset: Float,

    /**
     * The size (width/height as applicable) of the track
     */
    var baseSize: Float,

    /**
     * A temporary scratch value when sizing tracks
     * Note: can be infinity
     */
    var growthLimit: Float,

    /**
     * A temporary scratch value when sizing tracks. Is used as an additional amount to add to the
     * estimate for the available space in the opposite axis when content sizing items
     */
    var contentAlignmentAdjustment: Float,

    /**
     * A temporary scratch value when "distributing space" to avoid clobbering planned increase variable
     */
    var itemIncurredIncrease: Float,
    /**
     * A temporary scratch value when "distributing space" to avoid clobbering the main variable
     */
    var baseSizePlannedIncrease: Float,
    /**
     * A temporary scratch value when "distributing space" to avoid clobbering the main variable
     */
    var growthLimitPlannedIncrease: Float,
    /**
     * A temporary scratch value when "distributing space"
     * See: https://www.w3.org/TR/css3-grid-layout/#infinitely-growable
     */
    var infinitelyGrowable: Boolean
) {
    /**
     * Mark a GridTrack as collapsed. Also sets both of the track's sizing functions
     * to fixed zero-sized sizing functions.
     */
    fun collapse() {
        isCollapsed = true
        minTrackSizingFunction = MinTrackSizingFunction.Fixed(LengthPercentage.Length(0f))
        maxTrackSizingFunction = MaxTrackSizingFunction.Fixed(LengthPercentage.Length(0f))
    }

    /**
     * Returns true if the track is flexible (has a Flex MaxTrackSizingFunction), else false.
     */
    fun isFlexible(): Boolean {
        return maxTrackSizingFunction is MaxTrackSizingFunction.Fraction
    }

    /**
     * Returns true if the track is flexible (has a Flex MaxTrackSizingFunction), else false.
     */
    fun usesPercentage(): Boolean {
        return minTrackSizingFunction.usesPercentage() || maxTrackSizingFunction.usesPercentage()
    }

    /**
     * Returns true if the track has an intrinsic min and or max sizing function
     */
    fun hasIntrinsicSizingFunction(): Boolean {
        return minTrackSizingFunction.isIntrinsic() || maxTrackSizingFunction.isIntrinsic()
    }

    /**
     * Returns true if the track is flexible (has a Flex MaxTrackSizingFunction), else false.
     */
    fun fitContentLimit(axisAvailableGridSpace: Option<Float>): Float {
        return when (val mf = maxTrackSizingFunction) {
            is MaxTrackSizingFunction.FitContent -> {
                when (val l = mf.l) {
                    is LengthPercentage.Length -> l.f
                    is LengthPercentage.Percent -> when (axisAvailableGridSpace) {
                        is Option.Some -> axisAvailableGridSpace.value * l.f
                        else -> Float.POSITIVE_INFINITY
                    }
                }
            }
            else -> Float.POSITIVE_INFINITY
        }
    }

    /**
     * Returns true if the track is flexible (has a Flex MaxTrackSizingFunction), else false.
     */
    fun fitContentLimitedGrowthLimit(axisAvailableGridSpace: Option<Float>): Float {
        return f32Min(growthLimit, fitContentLimit(axisAvailableGridSpace))
    }

    /**
     * Returns the track's flex factor if it is a flex track, else 0.
     */
    fun flexFactor(): Float {
        return when (val v = maxTrackSizingFunction) {
            is MaxTrackSizingFunction.Fraction -> v.f
            else -> 0f
        }
    }

    companion object {
        /**
         * GridTrack constructor with all configuration parameters for the other constructors exposed
         */
        fun newWithKind(
            kind: GridTrackKind,
            minTrackSizingFunction: MinTrackSizingFunction,
            maxTrackSizingFunction: MaxTrackSizingFunction
        ): GridTrack {
            return GridTrack(
                kind = kind,
                isCollapsed = false,
                minTrackSizingFunction = minTrackSizingFunction,
                maxTrackSizingFunction = maxTrackSizingFunction,
                offset = 0f,
                baseSize = 0f,
                growthLimit = 0f,
                contentAlignmentAdjustment = 0f,
                itemIncurredIncrease = 0f,
                baseSizePlannedIncrease = 0f,
                growthLimitPlannedIncrease = 0f,
                infinitelyGrowable = false
            )
        }

        /**
         * Create new GridTrack representing an actual track (not a gutter)
         */
        fun new(
            minTrackSizingFunction: MinTrackSizingFunction,
            maxTrackSizingFunction: MaxTrackSizingFunction
        ): GridTrack {
            return newWithKind(GridTrackKind.TRACK, minTrackSizingFunction, maxTrackSizingFunction)
        }

        /**
         * Create a new GridTrack representing a gutter
         */
        fun gutter(
            size: LengthPercentage
        ): GridTrack {
            return newWithKind(GridTrackKind.GUTTER, MinTrackSizingFunction.Fixed(size), MaxTrackSizingFunction.Fixed(size))
        }
    }
}
