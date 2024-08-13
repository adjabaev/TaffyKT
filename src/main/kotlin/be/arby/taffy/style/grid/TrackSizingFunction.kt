package be.arby.taffy.style.grid

import be.arby.taffy.lang.From
import be.arby.taffy.style.dimension.LengthPercentage

/**
 * The sizing function for a grid track (row/column)
 * See <https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-columns>
 */
sealed class TrackSizingFunction {
    /**
     * A single non-repeated track
     */
    data class Single(var f: NonRepeatedTrackSizingFunction) : TrackSizingFunction()

    /**
     * Automatically generate grid tracks to fit the available space using the specified definite track lengths
     * Only valid if every track in template (not just the repetition) has a fixed size.
     */
    data class Repeat(var g: GridTrackRepetition, var l: List<NonRepeatedTrackSizingFunction>) : TrackSizingFunction()

    /**
     * Whether the track definition is a auto-repeated fragment
     */
    fun isAutoRepetition(): Boolean {
        return this is Repeat && (this.g is GridTrackRepetition.AutoFit || this.g is GridTrackRepetition.AutoFill)
    }

    companion object: From<NonRepeatedTrackSizingFunction, TrackSizingFunction> {
        fun fitContent(argument: LengthPercentage): TrackSizingFunction {
            return Single(NonRepeatedTrackSizingFunction.fitContent(argument))
        }

        fun fromLength(points: Float): TrackSizingFunction {
            return Single(NonRepeatedTrackSizingFunction.fromLength(points))
        }

        fun fromPercent(percent: Float): TrackSizingFunction {
            return Single(NonRepeatedTrackSizingFunction.fromPercent(percent))
        }

        fun fromFlex(flex: Float): TrackSizingFunction {
            return Single(NonRepeatedTrackSizingFunction.fromFlex(flex))
        }

        override fun from(value: NonRepeatedTrackSizingFunction): TrackSizingFunction {
            return Single(value)
        }
    }
}
