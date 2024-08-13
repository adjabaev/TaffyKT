package be.arby.taffy.style.grid

import be.arby.taffy.geom.MinMax
import be.arby.taffy.style.dimension.LengthPercentage

/**
 * The sizing function for a grid track (row/column) (either auto-track or template track)
 * May either be a MinMax variant which specifies separate values for the min-/max- track sizing functions
 * or a scalar value which applies to both track sizing functions.
 */
class NonRepeatedTrackSizingFunction(min: MinTrackSizingFunction, max: MaxTrackSizingFunction) :
    MinMax<MinTrackSizingFunction, MaxTrackSizingFunction>(min, max) {

    /**
     * Extract the min track sizing function
     */
    fun minSizingFunction(): MinTrackSizingFunction {
        return min
    }

    /**
     * Extract the max track sizing function
     */
    fun maxSizingFunction(): MaxTrackSizingFunction {
        return max
    }

    /**
     * Determine whether at least one of the components ("min" and "max") are fixed sizing function
     */
    fun hasFixedComponent(): Boolean {
        return min is MinTrackSizingFunction.Fixed || max is MaxTrackSizingFunction.Fixed
    }

    companion object {
        val AUTO: NonRepeatedTrackSizingFunction = NonRepeatedTrackSizingFunction(MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto)
        val MIN_CONTENT: NonRepeatedTrackSizingFunction = NonRepeatedTrackSizingFunction(MinTrackSizingFunction.MinContent, MaxTrackSizingFunction.MinContent)
        val MAX_CONTENT: NonRepeatedTrackSizingFunction = NonRepeatedTrackSizingFunction(MinTrackSizingFunction.MaxContent, MaxTrackSizingFunction.MaxContent)

        fun makeAuto(): NonRepeatedTrackSizingFunction {
            return NonRepeatedTrackSizingFunction(MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto)
        }

        fun fitContent(argument: LengthPercentage): NonRepeatedTrackSizingFunction {
            return NonRepeatedTrackSizingFunction(
                min = MinTrackSizingFunction.Auto,
                max = MaxTrackSizingFunction.FitContent(argument)
            )
        }

        fun fromLength(points: Float): NonRepeatedTrackSizingFunction {
            return NonRepeatedTrackSizingFunction(
                min = MinTrackSizingFunction.fromLength(points),
                max = MaxTrackSizingFunction.fromLength(points)
            )
        }

        fun fromPercent(percent: Float): NonRepeatedTrackSizingFunction {
            return NonRepeatedTrackSizingFunction(
                min = MinTrackSizingFunction.fromPercent(percent),
                max = MaxTrackSizingFunction.fromPercent(percent)
            )
        }

        fun fromFlex(flex: Float): NonRepeatedTrackSizingFunction {
            return NonRepeatedTrackSizingFunction(
                min = MinTrackSizingFunction.Auto,
                max = MaxTrackSizingFunction.fromFlex(flex)
            )
        }
    }
}
