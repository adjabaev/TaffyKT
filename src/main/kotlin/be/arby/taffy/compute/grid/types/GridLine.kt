package be.arby.taffy.compute.grid.types

import be.arby.taffy.lang.From

/**
 * Represents a grid line position in "CSS Grid Line" coordinates
 *
 * "CSS Grid Line" coordinates are those used in grid-row/grid-column in the CSS grid spec:
 *   - The line at left hand (or top) edge of the explicit grid is line 1
 *     (and counts up from there)
 *   - The line at the right hand (or bottom) edge of the explicit grid is -1
 *     (and counts down from there)
 *   - 0 is not a valid index
 */
data class GridLine(
    val value: Int
): GridCoordinate {

    /**
     * Returns the underlying i16
     */
    fun asI16(): Int {
        return value
    }

    /**
     * Convert into OriginZero coordinates using the specified explicit track count
     */
    fun intoOriginZeroLine(explicitTrackCount: Int): OriginZeroLine {
        val explicitLineCount = explicitTrackCount + 1
        val cmp = value.compareTo(0)
        val ozLine = when {
            cmp > 0 -> (value - 1)
            cmp < 0 -> (value + explicitLineCount)
            else -> throw UnsupportedOperationException("Grid line of zero is invalid")
        }
        return OriginZeroLine(ozLine)
    }

    companion object: From<Int, GridLine> {
        override fun from(value: Int): GridLine {
            return GridLine(value)
        }
    }
}
