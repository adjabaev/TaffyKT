package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.compute.grid.types.TrackCounts
import be.arby.taffy.compute.grid.types.maxLine
import be.arby.taffy.compute.grid.types.minLine
import be.arby.taffy.geom.Line
import be.arby.taffy.geom.indefiniteSpan
import be.arby.taffy.geom.intoOriginZero
import be.arby.taffy.lang.tuples.T2
import be.arby.taffy.lang.tuples.T3
import be.arby.taffy.lang.tuples.T6
import be.arby.taffy.style.grid.GridItemStyle
import be.arby.taffy.style.grid.GridPlacement
import kotlin.math.max

/**
 * Estimate the number of rows and columns in the grid
 * This is used as a performance optimisation to pre-size vectors and reduce allocations. It also forms a necessary step
 * in the auto-placement
 *   - The estimates for the explicit and negative implicit track counts are exact.
 *   - However, the estimates for the positive explicit track count is a lower bound as auto-placement can affect this
 *     in ways which are impossible to predict until the auto-placement algorithm is run.
 *
 * Note that this function internally mixes use of grid track numbers and grid line numbers
 */
fun <S : GridItemStyle> computeGridSizeEstimate(
    explicitColCount: Int,
    explicitRowCount: Int,
    childStylesIter: Iterator<S>
): T2<TrackCounts, TrackCounts> {
    // Iterate over children, producing an estimate of the min and max grid lines (in origin-zero coordinates where)
    // along with the span of each item
    val decomp = getKnownChildPositions(childStylesIter, explicitColCount, explicitRowCount)
    val colMin = decomp.first
    val colMax = decomp.second
    val colMaxSpan = decomp.third
    val rowMin = decomp.fourth
    val rowMax = decomp.fifth
    val rowMaxSpan = decomp.sixth

    // Compute *track* count estimates for each axis from:
    //   - The explicit track counts
    //   - The origin-zero coordinate min and max grid line variables
    val negativeImplicitInlineTracks = colMin.impliedNegativeImplicitTracks()
    val explicitInlineTracks = explicitColCount
    var positiveImplicitInlineTracks = colMax.impliedPositiveImplicitTracks(explicitColCount)
    val negativeImplicitBlockTracks = rowMin.impliedNegativeImplicitTracks()
    val explicitBlockTracks = explicitRowCount
    var positiveImplicitBlockTracks = rowMax.impliedPositiveImplicitTracks(explicitRowCount)

    // In each axis, adjust positive track estimate if any items have a span that does not fit within
    // the total number of tracks in the estimate
    val totInlineTracks = negativeImplicitInlineTracks + explicitInlineTracks + positiveImplicitInlineTracks;
    if (totInlineTracks < colMaxSpan) {
        positiveImplicitInlineTracks = colMaxSpan - explicitInlineTracks - negativeImplicitInlineTracks;
    }

    val totBlockTracks = negativeImplicitBlockTracks + explicitBlockTracks + positiveImplicitBlockTracks;
    if (totBlockTracks < rowMaxSpan) {
        positiveImplicitBlockTracks = rowMaxSpan - explicitBlockTracks - negativeImplicitBlockTracks;
    }

    val columnCounts =
        TrackCounts.fromRaw(negativeImplicitInlineTracks, explicitInlineTracks, positiveImplicitInlineTracks);

    val rowCounts =
        TrackCounts.fromRaw(negativeImplicitBlockTracks, explicitBlockTracks, positiveImplicitBlockTracks);

    return T2(columnCounts, rowCounts)
}

/**
 * Iterate over children, producing an estimate of the min and max grid *lines* along with the span of each item
 * Min and max grid lines are returned in origin-zero coordinates)
 *
 * The span is measured in tracks spanned
 */
fun <S : GridItemStyle> getKnownChildPositions(
    childrenIter: Iterator<S>,
    explicitColCount: Int,
    explicitRowCount: Int,
): T6<OriginZeroLine, OriginZeroLine, Int, OriginZeroLine, OriginZeroLine, Int> {
    var (colMin, colMax, colMaxSpan) = T3(OriginZeroLine(0), OriginZeroLine(0), 0)
    var (rowMin, rowMax, rowMaxSpan) = T3(OriginZeroLine(0), OriginZeroLine(0), 0)
    childrenIter.forEach { childStyle ->
        // Note: that the children reference the lines in between (and around) the tracks not tracks themselves,
        // and thus we must subtract 1 to get an accurate estimate of the number of tracks
        val (childColMin, childColMax, childColSpan) =
            childMinLineMaxLineSpan(childStyle.gridColumn(), explicitColCount)
        val (childRowMin, childRowMax, childRowSpan) =
            childMinLineMaxLineSpan(childStyle.gridRow(), explicitRowCount)
        colMin = minLine(colMin, childColMin)
        colMax = maxLine(colMax, childColMax)
        colMaxSpan = max(colMaxSpan, childColSpan)
        rowMin = minLine(rowMin, childRowMin)
        rowMax = maxLine(rowMax, childRowMax)
        rowMaxSpan = max(rowMaxSpan, childRowSpan)
    }

    return T6(colMin, colMax, colMaxSpan, rowMin, rowMax, rowMaxSpan)
}

fun childMinLineMaxLineSpan(
line: Line<GridPlacement>,
explicitTrackCount: Int,
): T3<OriginZeroLine, OriginZeroLine, Int> {
    // 8.3.1. Grid Placement Conflict Handling
    // A. If the placement for a grid item contains two lines, and the start line is further end-ward than the end line, swap the two lines.
    // B. If the start line is equal to the end line, remove the end line.
    // C. If the placement contains two spans, remove the one contributed by the end grid-placement property.
    // D. If the placement contains only a span for a named line, replace it with a span of 1.

    // Convert line into origin-zero coordinates before attempting to analyze
    val ozLine = line.intoOriginZero(explicitTrackCount);

    var start = ozLine.start
    var end = ozLine.end
    val min = when {
        // Both tracks specified
        start.isLine() && end.isLine() -> {
            val track1 = start.getLine<OriginZeroLine>()
            val track2 = end.getLine<OriginZeroLine>()
            // See rules A and B above
            if (track1 == track2) {
                track1
            } else {
                minLine(track1, track2)
            }
        }

        // Start track specified
        (start.isLine() && end.isAuto()) -> start.getLine()
        (start.isLine() && end.isSpan()) -> start.getLine()

        // End track specified
        (start.isAuto() && end.isLine()) -> end.getLine()
        (start.isSpan() && end.isLine()) -> end.getLine<OriginZeroLine>() - start.getSpan()

        // Only spans or autos
        // We ignore spans here by returning 0 which never effect the estimate as these are accounted for separately
        else -> OriginZeroLine(0)
    }

    start = ozLine.start
    end = ozLine.end
    val max = when {
        // Both tracks specified
        start.isLine() && end.isLine() -> {
            val track1 = start.getLine<OriginZeroLine>()
            val track2 = end.getLine<OriginZeroLine>()
            // See rules A and B above
            if (track1 == track2) {
                track1 + 1
            } else {
                maxLine(track1, track2)
            }
    }

        // Start track specified
        (start.isLine() && end.isAuto()) -> start.getLine<OriginZeroLine>() + 1
        (start.isLine() && end.isSpan()) -> start.getLine<OriginZeroLine>() + end.getSpan()

        // End track specified
        (start.isAuto() && end.isLine()) -> end.getLine()
        (start.isSpan() && end.isLine()) -> end.getLine()

        // Only spans or autos
        // We ignore spans here by returning 0 which never effect the estimate as these are accounted for separately
        else -> OriginZeroLine(0)
    };

    // Calculate span only for indefinitely placed items as we don't need for other items (whose required space will
    // be taken into account by min and max)
    val s = line.start
    val e = line.end
    val span: Int = if ((s.isAuto() || s.isSpan()) && (e.isAuto() || e.isSpan())) line.indefiniteSpan() else 1

    return T3(min, max, span)
}
