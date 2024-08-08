package be.arby.taffy.compute.grid.types

import be.arby.taffy.geometry.Line

data class TrackCounts(
    var negativeImplicit: Int,
    var explicit: Int,
    var positiveImplicit: Int
) {

    fun len(): Int {
        return (negativeImplicit + explicit + positiveImplicit)
    }

    /**
     * The OriginZeroLine representing the start of the implicit grid
     */
    fun implicitStartLine(): OriginZeroLine {
        return OriginZeroLine((-negativeImplicit).toShort())
    }

    /**
     * The OriginZeroLine representing the end of the implicit grid
     */
    fun implicitEndLine(): OriginZeroLine {
        return OriginZeroLine((explicit + positiveImplicit).toShort())
    }

    fun ozLineToNextTrack(index: OriginZeroLine): Short {
        return (index.value + negativeImplicit).toShort()
    }

    fun ozLineRangeToTrackRange(input: Line<OriginZeroLine>): IntRange {
        val start = ozLineToNextTrack(input.start)
        val end = ozLineToNextTrack(input.end) // Don't subtract 1 as output range is exclusive
        return start until end
    }

    fun trackToPrevOzLine(index: Int): OriginZeroLine {
        return OriginZeroLine((index - negativeImplicit).toShort())
    }

    fun trackRangeToOzLineRange(input: IntRange): Line<OriginZeroLine> {
        val start = trackToPrevOzLine(input.first)
        val end = (trackToPrevOzLine(input.last) + 1) // Add 1 as input range is inclusive
        return Line(start, end)
    }

    companion object {
        @JvmStatic
        fun fromRaw(negativeImplicit: Int, explicit: Int, positiveImplicit: Int): TrackCounts {
            return TrackCounts(negativeImplicit, explicit, positiveImplicit)
        }
    }
}
