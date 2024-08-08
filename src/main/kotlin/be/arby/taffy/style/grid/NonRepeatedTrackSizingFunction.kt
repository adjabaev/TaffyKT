package be.arby.taffy.style.grid

import be.arby.taffy.geom.MinMax
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.utils.Into

class NonRepeatedTrackSizingFunction(min: MinTrackSizingFunction, max: MaxTrackSizingFunction) :
    MinMax<MinTrackSizingFunction, MaxTrackSizingFunction>(min, max) {

    fun minSizingFunction(): MinTrackSizingFunction {
        return min
    }

    fun maxSizingFunction(): MaxTrackSizingFunction {
        return max
    }

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

        fun fromPoints(points: Into<Float>): NonRepeatedTrackSizingFunction {
            return fromPoints(points.into())
        }

        fun fromPoints(points: Float): NonRepeatedTrackSizingFunction {
            return NonRepeatedTrackSizingFunction(
                min = MinTrackSizingFunction.fromPoints(points),
                max = MaxTrackSizingFunction.fromPoints(points)
            )
        }

        fun fromPercent(percent: Into<Float>): NonRepeatedTrackSizingFunction {
            return fromPercent(percent.into())
        }

        fun fromPercent(percent: Float): NonRepeatedTrackSizingFunction {
            return NonRepeatedTrackSizingFunction(
                min = MinTrackSizingFunction.fromPercent(percent),
                max = MaxTrackSizingFunction.fromPercent(percent)
            )
        }

        fun fromFlex(flex: Into<Float>): NonRepeatedTrackSizingFunction {
            return fromFlex(flex.into())
        }

        fun fromFlex(flex: Float): NonRepeatedTrackSizingFunction {
            return NonRepeatedTrackSizingFunction(
                min = MinTrackSizingFunction.Auto,
                max = MaxTrackSizingFunction.fromFlex(flex)
            )
        }
    }
}
