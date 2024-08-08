package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.compute.grid.types.TrackCounts
import be.arby.taffy.geometry.Line
import be.arby.taffy.geometry.extensions.indefiniteSpan
import be.arby.taffy.geometry.extensions.intoOriginZero
import be.arby.taffy.style.Style
import be.arby.taffy.style.grid.GenericGridPlacement
import be.arby.taffy.style.grid.GridPlacement
import be.arby.taffy.utils.max
import be.arby.taffy.utils.min
import be.arby.taffy.utils.tuples.Sextuple
import kotlin.math.max

class ImplicitGrid {
    companion object {
        fun computeGridSizeEstimate(
            explicitColCount: Int,
            explicitRowCount: Int,
            childStylesIter: Iterator<Style>
        ): Pair<TrackCounts, TrackCounts> {
            // Iterate over children, producing an estimate of the min and max grid lines (in origin-zero coordinates where)
            // along with the span of each itme
            val (colMin, colMax, colMaxSpan, rowMin, rowMax, rowMaxSpan) = getKnownChildPositions(
                childStylesIter,
                explicitColCount,
                explicitRowCount
            )

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
            val totInlineTracks = negativeImplicitInlineTracks + explicitInlineTracks + positiveImplicitInlineTracks
            if (totInlineTracks < colMaxSpan) {
                positiveImplicitInlineTracks = colMaxSpan - explicitInlineTracks - negativeImplicitInlineTracks
            }

            val totBlockTracks = negativeImplicitBlockTracks + explicitBlockTracks + positiveImplicitBlockTracks
            if (totBlockTracks < rowMaxSpan) {
                positiveImplicitBlockTracks = rowMaxSpan - explicitBlockTracks - negativeImplicitBlockTracks
            }

            val columnCounts =
                TrackCounts.fromRaw(negativeImplicitInlineTracks, explicitInlineTracks, positiveImplicitInlineTracks)

            val rowCounts =
                TrackCounts.fromRaw(negativeImplicitBlockTracks, explicitBlockTracks, positiveImplicitBlockTracks)

            return Pair(columnCounts, rowCounts)
        }

        fun getKnownChildPositions(
            childrenIter: Iterator<Style>,
            explicitColCount: Int,
            explicitRowCount: Int,
        ): Sextuple<OriginZeroLine, OriginZeroLine, Int, OriginZeroLine, OriginZeroLine, Int> {
            var (colMin: OriginZeroLine, colMax: OriginZeroLine, colMaxSpan) = Triple(OriginZeroLine(0), OriginZeroLine(0), 0)
            var (rowMin: OriginZeroLine, rowMax: OriginZeroLine, rowMaxSpan) = Triple(OriginZeroLine(0), OriginZeroLine(0), 0)
            childrenIter.forEach { childStyle: Style ->
                // Note: that the children reference the lines in between (and around) the tracks not tracks themselves,
                // and thus we must subtract 1 to get an accurate estimate of the number of tracks
                val (childColMin, childColMax, childColSpan) = childMinLineMaxLineSpan(
                    childStyle.gridColumn,
                    explicitColCount
                )
                val (childRowMin, childRowMax, childRowSpan) = childMinLineMaxLineSpan(
                    childStyle.gridRow,
                    explicitRowCount
                )
                colMin = min(colMin, childColMin)
                colMax = max(colMax, childColMax)
                colMaxSpan = max(colMaxSpan, childColSpan.toInt())
                rowMin = min(rowMin, childRowMin)
                rowMax = max(rowMax, childRowMax)
                rowMaxSpan = max(rowMaxSpan, childRowSpan.toInt())
            }

            return Sextuple(colMin, colMax, colMaxSpan, rowMin, rowMax, rowMaxSpan)
        }

        fun childMinLineMaxLineSpan(line: Line<GridPlacement>, explicitTrackCount: Int): Triple<OriginZeroLine, OriginZeroLine, Int> {
            // 8.3.1. Grid Placement Conflict Handling
            // A. If the placement for a grid item contains two lines, and the start line is further end-ward than the end line, swap the two lines.
            // B. If the start line is equal to the end line, remove the end line.
            // C. If the placement contains two spans, remove the one contributed by the end grid-placement property.
            // D. If the placement contains only a span for a named line, replace it with a span of 1.

            // Convert line into origin-zero coordinates before attempting to analyze
            val ozLine = line.intoOriginZero(explicitTrackCount);

            val (lineA, lineB) = Pair(ozLine.start, ozLine.end)
            val min: OriginZeroLine = when {
                // Both tracks specified
                lineA is GenericGridPlacement.Line && lineB is GenericGridPlacement.Line -> {
                    val track1 = lineA.s
                    val track2 = lineB.s
                    // See rules A and B above
                    if (track1 == track2) {
                        track1
                    } else {
                        min(track1, track2)
                    }
                }

                // Start track specified
                lineA is GenericGridPlacement.Line && lineB is GenericGridPlacement.Auto -> lineA.s
                lineA is GenericGridPlacement.Line && lineB is GenericGridPlacement.Span -> lineA.s

                // End track specified
                lineA is GenericGridPlacement.Auto && lineB is GenericGridPlacement.Line -> lineB.s
                lineA is GenericGridPlacement.Span && lineB is GenericGridPlacement.Line -> (lineB.s - lineA.i)

                // Only spans or autos
                // We ignore spans here by returning 0 which never effect the estimate as these are accounted for separately
                else -> OriginZeroLine(0)
            }

            val max: OriginZeroLine = when {
                // Both tracks specified
                lineA is GenericGridPlacement.Line && lineB is GenericGridPlacement.Line -> {
                    val track1 = lineA.s
                    val track2 = lineB.s
                    // See rules A and B above
                    if (track1 == track2) {
                        track1 + 1
                    } else {
                        max(track1, track2)
                    }
                }

                // Start track specified
                lineA is GenericGridPlacement.Line && lineB is GenericGridPlacement.Auto -> lineA.s + 1
                lineA is GenericGridPlacement.Line && lineB is GenericGridPlacement.Span -> lineA.s + lineB.i

                // End track specified
                lineA is GenericGridPlacement.Auto && lineB is GenericGridPlacement.Line -> lineB.s
                lineA is GenericGridPlacement.Span && lineB is GenericGridPlacement.Line -> lineB.s

                // Only spans or autos
                // We ignore spans here by returning 0 which never effect the estimate as these are accounted for separately
                else -> OriginZeroLine(0)
            }

            // Calculate span only for indefinitely placed items as we don't need for other items (whose required space will
            // be taken into account by min and max)
            val span = when {
                (lineA is GenericGridPlacement.Auto || lineA is GenericGridPlacement.Span) && (lineB is GenericGridPlacement.Auto || lineB is GenericGridPlacement.Span) -> {
                    line.indefiniteSpan()
                }
                else -> 1
            }

            return Triple(min, max, span)
        }
    }
}
