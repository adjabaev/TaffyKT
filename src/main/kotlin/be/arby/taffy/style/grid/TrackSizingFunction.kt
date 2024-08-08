package be.arby.taffy.style.grid

import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.utils.Into

sealed class TrackSizingFunction {
    data class Single(var f: NonRepeatedTrackSizingFunction) : TrackSizingFunction()
    data class Repeat(var g: GridTrackRepetition, var l: List<NonRepeatedTrackSizingFunction>) :
        TrackSizingFunction()

    fun isAutoRepetition(): Boolean {
        return this is Repeat && (this.g is GridTrackRepetition.AutoFit || this.g is GridTrackRepetition.AutoFill)
    }

    companion object {
        fun fitContent(argument: LengthPercentage): TrackSizingFunction {
            return Single(NonRepeatedTrackSizingFunction.fitContent(argument))
        }

        fun fromPoints(points: Into<Float>): TrackSizingFunction {
            return fromPoints(points)
        }

        fun fromPoints(points: Float): TrackSizingFunction {
            return Single(NonRepeatedTrackSizingFunction.fromPoints(points))
        }

        fun fromPercent(percent: Into<Float>): TrackSizingFunction {
            return fromPercent(percent)
        }

        fun fromPercent(percent: Float): TrackSizingFunction {
            return Single(NonRepeatedTrackSizingFunction.fromPercent(percent))
        }

        fun fromFlex(flex: Into<Float>): TrackSizingFunction {
            return fromFlex(flex)
        }

        fun fromFlex(flex: Float): TrackSizingFunction {
            return Single(NonRepeatedTrackSizingFunction.fromFlex(flex))
        }

        fun from(input: NonRepeatedTrackSizingFunction): TrackSizingFunction {
            return Single(input)
        }
    }
}
