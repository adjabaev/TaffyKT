package be.arby.taffy.compute.grid.types

import be.arby.taffy.geometry.Line
import be.arby.taffy.lang.Option
import be.arby.taffy.maths.axis.AbsoluteAxis
import be.arby.taffy.utils.Grid
import be.arby.taffy.utils.rposition
import java.util.*
import kotlin.math.max
import kotlin.math.min

class CellOccupancyMatrix(
    var inner: Grid<CellOccupancyState>, var columns: TrackCounts, var rows: TrackCounts
) {
    fun isAreaInRange(
        primaryAxis: AbsoluteAxis,
        primaryRange: IntRange,
        secondaryRange: IntRange,
    ): Boolean {
        if (primaryRange.first < 0 || (primaryRange.last + 1) > trackCounts(primaryAxis).len()) {
            return false
        }
        if (secondaryRange.first < 0 || (secondaryRange.last + 1) > trackCounts(primaryAxis.otherAxis()).len()) {
            return false
        }
        return true
    }

    fun expandToFitRange(rowRange: IntRange, colRange: IntRange) {
        // Calculate number of rows and columns missing to accomodate ranges (if any)
        val reqNegativeRows = min(rowRange.first, 0)
        val reqPositiveRows = max((rowRange.last + 1) - rows.explicit - rows.positiveImplicit, 0)
        val reqNegativeCols = min(colRange.first, 0)
        val reqPositiveCols = max((colRange.last + 1) - columns.explicit - columns.positiveImplicit, 0)

        val oldRowCount = rows.len()
        val oldColCount = columns.len()
        val newRowCount = oldRowCount + (reqNegativeRows + reqPositiveRows)
        val newColCount = oldColCount + (reqNegativeCols + reqPositiveCols)

        val data = ArrayList<CellOccupancyState>()

        // Push new negative rows
        for (v in 0 until (reqNegativeRows * newColCount)) {
            data.add(CellOccupancyState.UNOCCUPIED)
        }

        // Push existing rows
        for (row in 0 until oldRowCount) {
            // Push new negative columns
            for (v in 0 until reqNegativeCols) {
                data.add(CellOccupancyState.UNOCCUPIED)
            }
            // Push existing columns
            for (col in 0 until oldColCount) {
                data.add(inner[row, col].unwrap())
            }
            // Push new positive columns
            for (v in 0 until reqPositiveCols) {
                data.add(CellOccupancyState.UNOCCUPIED)
            }
        }

        // Push new negative rows
        for (v in 0 until (reqPositiveRows * newColCount)) {
            data.add(CellOccupancyState.UNOCCUPIED)
        }

        // Update self with new data
        inner = Grid.fromList(data, newColCount)
        rows.negativeImplicit += reqNegativeRows
        rows.positiveImplicit += reqPositiveRows
        columns.negativeImplicit += reqNegativeCols
        columns.positiveImplicit += reqPositiveCols
    }

    fun markAreaAs(
        primaryAxis: AbsoluteAxis,
        primarySpan: Line<OriginZeroLine>,
        secondarySpan: Line<OriginZeroLine>,
        value: CellOccupancyState,
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

    fun lineAreaIsUnoccupied(
        primaryAxis: AbsoluteAxis,
        primarySpan: Line<OriginZeroLine>,
        secondarySpan: Line<OriginZeroLine>,
    ): Boolean {
        val primaryRange = trackCounts(primaryAxis).ozLineRangeToTrackRange(primarySpan)
        val secondaryRange = trackCounts(primaryAxis.otherAxis()).ozLineRangeToTrackRange(secondarySpan)
        return trackAreaIsUnoccupied(primaryAxis, primaryRange, secondaryRange)
    }

    fun trackAreaIsUnoccupied(
        primaryAxis: AbsoluteAxis,
        primaryRange: IntRange,
        secondaryRange: IntRange,
    ): Boolean {
        val (rowRange, colRange) = when (primaryAxis) {
            AbsoluteAxis.HORIZONTAL -> Pair(secondaryRange, primaryRange)
            AbsoluteAxis.VERTICAL -> Pair(primaryRange, secondaryRange)
        }

        // Search for occupied cells in the specified area. Out of bounds cells are considered unoccupied.
        for (x in rowRange) {
            for (y in colRange) {
                val v = inner[x, y]
                if (v.isNone() || v.unwrap() == CellOccupancyState.UNOCCUPIED) {
                    continue
                } else {
                    return false
                }
            }
        }

        return true
    }

    fun rowIsOccupied(rowIndex: Int): Boolean {
        return inner.iterRow(rowIndex).any { opCell -> opCell.isSome() && opCell.unwrap() != CellOccupancyState.UNOCCUPIED }
    }

    fun columnIsOccupied(columnIndex: Int): Boolean {
        return inner.iterCol(columnIndex).any { opCell -> opCell.isSome() && opCell.unwrap() != CellOccupancyState.UNOCCUPIED }
    }

    fun trackCounts(trackType: AbsoluteAxis): TrackCounts {
        return when (trackType) {
            AbsoluteAxis.HORIZONTAL -> columns
            AbsoluteAxis.VERTICAL -> rows
        }
    }

    fun lastOfType(trackType: AbsoluteAxis, startAt: OriginZeroLine, kind: CellOccupancyState): Option<OriginZeroLine> {
        val trackCounts = trackCounts(trackType.otherAxis())
        val trackComputedIndex = trackCounts.ozLineToNextTrack(startAt)

        val maybeIndex = when (trackType) {
            AbsoluteAxis.HORIZONTAL -> {
                inner.iterRow(trackComputedIndex.toInt()).rposition { item -> item.isSome() && item.unwrap() == kind }
            }

            AbsoluteAxis.VERTICAL -> {
                inner.iterCol(trackComputedIndex.toInt()).rposition { item -> item.isSome() && item.unwrap() == kind }
            }
        }

        return maybeIndex.map { idx -> trackCounts.trackToPrevOzLine(idx) }
    }

    companion object {
        fun withTrackCounts(columns: TrackCounts, rows: TrackCounts): CellOccupancyMatrix {
            return CellOccupancyMatrix(inner = Grid.make(rows.len(), columns.len()) { CellOccupancyState.UNOCCUPIED }, rows = rows, columns = columns)
        }
    }
}
