package be.arby.taffy.compute.grid.types

import kotlin.math.abs

/**
 * Represents a grid line position in "CSS Grid Line" coordinates
 *
 * Represents a grid line position in "OriginZero" coordinates
 *
 * "OriginZero" coordinates are a normalized form:
 *   - The line at left hand (or top) edge of the explicit grid is line 0
 *   - The next line to the right (or down) is 1, and so on
 *   - The next line to the left (or up) is -1, and so on
 */
data class OriginZeroLine(
    var value: Int
) : GridCoordinate, Comparable<OriginZeroLine> {

    /**
     * Converts a grid line in OriginZero coordinates into the index of that same grid line in the GridTrackVec.
     */
    fun intoTrackVecIndex(trackCounts: TrackCounts): Int {
        return 2 * (value + trackCounts.negativeImplicit)
    }

    /**
     * The minimum number of negative implicit track there must be if a grid item starts at this line.
     */
    fun impliedNegativeImplicitTracks(): Int {
        return if (value < 0) {
            abs(value)
        } else {
            0
        }
    }

    /**
     * The minimum number of positive implicit track there must be if a grid item end at this line.
     */
    fun impliedPositiveImplicitTracks(explicitTrackCount: Int): Int {
        return if (value > explicitTrackCount) {
            (value - explicitTrackCount)
        } else {
            0
        }
    }

    operator fun plus(other: Int): OriginZeroLine {
        return OriginZeroLine((value + other))
    }

    operator fun minus(other: Int): OriginZeroLine {
        return OriginZeroLine((value - other))
    }

    override operator fun compareTo(other: OriginZeroLine): Int {
        return value.compareTo(other.value)
    }
}

fun minLine(a: OriginZeroLine, b: OriginZeroLine): OriginZeroLine {
    return if (a.value < b.value) a else b
}

fun maxLine(a: OriginZeroLine, b: OriginZeroLine): OriginZeroLine {
    return if (a.value > b.value) a else b
}
