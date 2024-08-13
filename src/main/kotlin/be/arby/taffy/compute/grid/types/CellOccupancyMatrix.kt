package be.arby.taffy.compute.grid.types

import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.geom.Line
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.collections.rposition
import be.arby.taffy.lang.grid.Grid
import kotlin.math.max
import kotlin.math.min

/**
 * A dynamically sized matrix (2d grid) which tracks the occupancy of each grid cell during auto-placement
 * It also keeps tabs on how many tracks there are and which tracks are implicit and which are explicit.
 */
data class CellOccupancyMatrix(
    var inner: Grid<CellOccupancyState>,
    val columns: TrackCounts,
    val rows: TrackCounts
) {

    /**
     * Determines whether the specified area fits within the tracks currently represented by the matrix
     */
    fun isAreaInRange(
        primaryAxis: AbsoluteAxis,
        primaryRange: IntRange,
        secondaryRange: IntRange
    ): Boolean {
        if (primaryRange.start < 0 || primaryRange.endInclusive + 1 > trackCounts(primaryAxis).len()) {
            return false
        }
        if (secondaryRange.start < 0 || secondaryRange.endInclusive + 1 > trackCounts(primaryAxis.otherAxis()).len()) {
            return false
        }
        return true
    }

    /**
     * Expands the grid (potentially in all 4 directions) in order to ensure that the specified range fits within the allocated space
     */

    fun expandToFitRange(
        rowRange: IntRange,
        colRange: IntRange
    ) {
        // Calculate number of rows and columns missing to accommodate ranges (if any)
        val reqNegativeRows = min(rowRange.start, 0)
        val reqPositiveRows = max(rowRange.endExclusive - rows.len(), 0)
        val reqNegativeCols = min(colRange.start, 0)
        val reqPositiveCols = max(colRange.endExclusive - columns.len(), 0)

        val oldRowCount = rows.len()
        val oldColCount = columns.len()
        val newColCount = oldColCount + (reqNegativeCols + reqPositiveCols)

        val data = mutableListOf<CellOccupancyState>()

        // Push new negative rows
        for (u in 0 until (reqNegativeRows * newColCount)) {
            data.add(CellOccupancyState.UNOCCUPIED)
        }

        // Push existing rows
        for (row in 0 until oldRowCount) {
            // Push new negative columns
            for (u in 0 until reqNegativeCols) {
                data.add(CellOccupancyState.UNOCCUPIED)
            }
            // Push existing columns
            for (col in 0 until oldColCount) {
                data.add(inner.get(row, col).unwrap())
            }
            // Push new positive columns
            for (u in 0 until reqPositiveCols) {
                data.add(CellOccupancyState.UNOCCUPIED)
            }
        }

        // Push new negative rows
        for (u in 0 until (reqPositiveRows * newColCount)) {
            data.add(CellOccupancyState.UNOCCUPIED)
        }

        // Update self with new data
        inner = Grid.fromList(data, newColCount);
        rows.negativeImplicit += reqNegativeRows
        rows.positiveImplicit += reqPositiveRows
        columns.negativeImplicit += reqNegativeCols
        columns.positiveImplicit += reqPositiveCols
    }

    /**
     * Mark an area of the matrix as occupied, expanding the allocated space as necessary to accommodate the passed area.
     */
    fun markAreaAs(
        primaryAxis: AbsoluteAxis,
        primarySpan: Line<OriginZeroLine>,
        secondarySpan: Line<OriginZeroLine>,
        value: CellOccupancyState
    ) {
        val (rowSpan, columnSpan) = when (primaryAxis) {
            AbsoluteAxis.HORIZONTAL -> Pair(secondarySpan, primarySpan)
            AbsoluteAxis.VERTICAL -> Pair(primarySpan, secondarySpan)
        }

        var colRange = columns.ozLineRangeToTrackRange(columnSpan)
        var rowRange = rows.ozLineRangeToTrackRange(rowSpan)

        // Check that if the resolved ranges fit within the allocated grid. And if they don't then expand the grid to fit
        // and then re-resolve the ranges once the grid has been expanded as the resolved indexes may have changed
        val isInRange = isAreaInRange(AbsoluteAxis.HORIZONTAL, colRange, rowRange)
        if (!isInRange) {
            expandToFitRange(rowRange, colRange)
            colRange = columns.ozLineRangeToTrackRange(columnSpan)
            rowRange = rows.ozLineRangeToTrackRange(rowSpan)
        }

        for (x in rowRange) {
            for (y in colRange) {
                inner[x, y] = value
            }
        }
    }

    /**
     * Determines whether a grid area specified by the bounding grid lines in OriginZero coordinates
     * is entirely unoccupied. Returns true if all grid cells within the grid area are unoccupied, else false.
     */
    fun lineAreaIsUnoccupied(
        primaryAxis: AbsoluteAxis,
        primarySpan: Line<OriginZeroLine>,
        secondarySpan: Line<OriginZeroLine>,
    ): Boolean {
        val primaryRange = trackCounts(primaryAxis).ozLineRangeToTrackRange(primarySpan)
        val secondaryRange = trackCounts(primaryAxis.otherAxis()).ozLineRangeToTrackRange(secondarySpan)
        return trackAreaIsUnoccupied(primaryAxis, primaryRange, secondaryRange)
    }

    /**
     * Determines whether a grid area specified by a range of indexes into this CellOccupancyMatrix
     * is entirely unoccupied. Returns true if all grid cells within the grid area are unoccupied, else false.
     */
    fun trackAreaIsUnoccupied(
        primaryAxis: AbsoluteAxis,
        primaryRange: IntRange,
        secondaryRange: IntRange
    ): Boolean {
        val (rowRange, colRange) = when (primaryAxis) {
            AbsoluteAxis.HORIZONTAL -> Pair(secondaryRange, primaryRange)
            AbsoluteAxis.VERTICAL -> Pair(primaryRange, secondaryRange)
        }

        // Search for occupied cells in the specified area. Out of bounds cells are considered unoccupied.
        for (x in rowRange) {
            for (y in colRange) {
                when (val v = inner.get(x, y)) {
                    is Option.None -> continue
                    is Option.Some -> {
                        if (v.unwrap() == CellOccupancyState.UNOCCUPIED) {
                            continue
                        } else {
                            return false
                        }
                    }
                }
            }
        }

        return true
    }

    /**
     * Determines whether the specified row contains any items
     */
    fun rowIsOccupied(rowIndex: Int): Boolean {
        return inner.iterRow(rowIndex).any { cell -> cell != CellOccupancyState.UNOCCUPIED }
    }

    /**
     * Determines whether the specified row contains any items
     */
    fun columnIsOccupied(columnIndex: Int): Boolean {
        return inner.iterCol(columnIndex).any { cell -> cell != CellOccupancyState.UNOCCUPIED }
    }

    /**
     * Returns the track counts of this CellOccunpancyMatrix in the relevant axis
     */
    fun trackCounts(trackType: AbsoluteAxis): TrackCounts {
        return when (trackType) {
            AbsoluteAxis.HORIZONTAL -> columns
            AbsoluteAxis.VERTICAL -> rows
        }
    }

    /**
     * Given an axis and a track index
     * Search backwards from the end of the track and find the last grid cell matching the specified state (if any)
     * Return the index of that cell or None.
     */
    fun lastOfType(
        trackType: AbsoluteAxis,
        startAt: OriginZeroLine,
        kind: CellOccupancyState
    ): Option<OriginZeroLine> {
        val trackCounts = trackCounts(trackType.otherAxis())
        val trackComputedIndex = trackCounts.ozLineToNextTrack(startAt)

        val maybeIndex = when (trackType) {
            AbsoluteAxis.HORIZONTAL -> {
                inner.iterRow(trackComputedIndex).rposition { item -> item == kind }
            }

            AbsoluteAxis.VERTICAL -> {
                inner.iterCol(trackComputedIndex).rposition { item -> item == kind }
            }
        }

        return maybeIndex.map { idx -> trackCounts.trackToPrevOzLine(idx) }
    }

    companion object {
        /**
         * Create a CellOccupancyMatrix given a set of provisional track counts. The grid can expand as needed to fit more tracks,
         * the provisional track counts represent a best effort attempt to avoid the extra allocations this requires.
         */
        fun withTrackCounts(columns: TrackCounts, rows: TrackCounts): CellOccupancyMatrix {
            return CellOccupancyMatrix(inner = Grid.new(rows.len(), columns.len()), rows, columns)
        }
    }
}
