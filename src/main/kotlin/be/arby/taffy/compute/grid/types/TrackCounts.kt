package be.arby.taffy.compute.grid.types

import be.arby.taffy.geom.Line

/**
 * Stores the number of tracks in a given dimension.
 * Stores separately the number of tracks in the implicit and explicit grids
 */
data class TrackCounts(
    /**
     * The number of track in the implicit grid before the explicit grid
     */
    var negativeImplicit: Int,
    /**
     * The number of tracks in the explicit grid
     */
    val explicit: Int,
    /**
     * The number of tracks in the implicit grid after the explicit grid
     */
    var positiveImplicit: Int
) {
    /**
     * Count the total number of tracks in the axis
     */
    fun len(): Int {
        return negativeImplicit + explicit + positiveImplicit
    }

    /**
     * The OriginZeroLine representing the start of the implicit grid
     */
    fun implicitStartLine(): OriginZeroLine {
        return OriginZeroLine((-negativeImplicit))
    }

    /**
     * The OriginZeroLine representing the end of the implicit grid
     */
    fun implicitEndLine(): OriginZeroLine {
        return OriginZeroLine((explicit + positiveImplicit))
    }

    /**
     * Converts a grid line in OriginZero coordinates into the track immediately
     * following that grid line as an index into the CellOccupancyMatrix.
     */
    fun ozLineToNextTrack(index: OriginZeroLine): Int {
        return (index.value + negativeImplicit)
    }

    /**
     * Converts start and end grid lines in OriginZero coordinates into a range of tracks
     * as indexes into the CellOccupancyMatrix
     */
    fun ozLineRangeToTrackRange(input: Line<OriginZeroLine>): IntRange {
        val start = ozLineToNextTrack(input.start)
        val end = ozLineToNextTrack(input.end) // Don't subtract 1 as output range is exclusive
        return start until end
    }

    /**
     * Converts a track as an index into the CellOccupancyMatrix into the grid line immediately
     * preceding that track in OriginZero coordinates.
     */
    fun trackToPrevOzLine(index: Int): OriginZeroLine {
        return OriginZeroLine((index - negativeImplicit))
    }

    /**
     * Converts a range of tracks as indexes into the CellOccupancyMatrix into
     * start and end grid lines in OriginZero coordinates
     */
    fun trackRangeToOzLineRange(input: IntRange): Line<OriginZeroLine> {
        val start = trackToPrevOzLine(input.start)
        val end = trackToPrevOzLine(input.endExclusive) // Don't add 1 as input range is exclusive
        return Line(start, end)
    }

    companion object {
        /**
         * Create a TrackCounts instance from raw track count numbers
         */
        fun fromRaw(negativeImplicit: Int, explicit: Int, positiveImplicit: Int): TrackCounts {
            return TrackCounts(negativeImplicit, explicit, positiveImplicit)
        }
    }
}
