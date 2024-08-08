package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.CellOccupancyMatrix
import be.arby.taffy.compute.grid.types.CellOccupancyState
import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.geometry.Line
import be.arby.taffy.geometry.extensions.*
import be.arby.taffy.maths.axis.AbsoluteAxis
import be.arby.taffy.geom.InBothAbsAxis
import be.arby.taffy.node.Node
import be.arby.taffy.style.Style
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.grid.GridAutoFlow
import be.arby.taffy.style.grid.OriginZeroGridPlacement
import be.arby.taffy.utils.tuples.Quadruple

class Placement {
    companion object {
        fun placeGridItems(
            cellOccupancyMatrix: CellOccupancyMatrix,
            items: ArrayList<GridItem>,
            childrenIter: List<Triple<Int, Node, Style>>,
            gridAutoFlow: GridAutoFlow,
            alignItems: AlignItems
        ) {
            val primaryAxis = gridAutoFlow.primaryAxis()
            val secondaryAxis = primaryAxis.otherAxis()

            val mapChildStyleToOriginZeroPlacement: (Int, Node, Style) ->
            Quadruple<Int, Node, InBothAbsAxis<Line<OriginZeroGridPlacement>>, Style> = { index, node, style ->
                val explicitColCount = cellOccupancyMatrix.trackCounts(AbsoluteAxis.HORIZONTAL).explicit
                val explicitRowCount = cellOccupancyMatrix.trackCounts(AbsoluteAxis.VERTICAL).explicit
                val originZeroPlacement = InBothAbsAxis(
                    horizontal = style.gridColumn.map { placement -> placement.intoOriginZeroPlacement(explicitColCount) },
                    vertical = style.gridRow.map { placement -> placement.intoOriginZeroPlacement(explicitRowCount) }
                )
                Quadruple(index, node, originZeroPlacement, style)
            }

            // 1. Place children with definite positions
            var idx = 0
            childrenIter
                .filter { (_, _, childStyle) -> childStyle.gridRow.isDefinite() && childStyle.gridColumn.isDefinite() }
                .map { (a, b, c) -> mapChildStyleToOriginZeroPlacement.invoke(a, b, c) }
                .forEach { (index, childNode, childPlacement, style) ->
                    idx += 1;
                    val (rowSpan, colSpan) = placeDefiniteGridItem(childPlacement, primaryAxis)
                    recordGridPlacement(
                        cellOccupancyMatrix,
                        items,
                        childNode,
                        index,
                        style,
                        alignItems,
                        primaryAxis,
                        rowSpan,
                        colSpan,
                        CellOccupancyState.DEFINITELY_PLACED,
                    )
                }

            // 2. Place remaining children with definite secondary axis positions
            idx = 0
            childrenIter
                .filter { (_, _, childStyle) ->
                    childStyle.gridPlacement(secondaryAxis).isDefinite() &&
                            !childStyle.gridPlacement(primaryAxis).isDefinite()
                }
                .map { (a, b, c) -> mapChildStyleToOriginZeroPlacement.invoke(a, b, c) }
                .forEach { (index, childNode, childPlacement, style) ->
                    idx += 1
                    val (primarySpan, secondarySpan) =
                        placeDefiniteSecondaryAxisItem(cellOccupancyMatrix, childPlacement, gridAutoFlow)

                    recordGridPlacement(
                        cellOccupancyMatrix,
                        items,
                        childNode,
                        index,
                        style,
                        alignItems,
                        primaryAxis,
                        primarySpan,
                        secondarySpan,
                        CellOccupancyState.AUTO_PLACED,
                    )
                }

            // 3. Determine the number of columns in the implicit grid
            // By the time we get to this point in the execution, this is actually already accounted for:
            //
            // 3.1 Start with the columns from the explicit grid
            //        => Handled by grid size estimate which is used to pre-size the GridOccupancyMatrix
            //
            // 3.2 Among all the items with a definite column position (explicitly positioned items, items positioned in the previous step,
            //     and items not yet positioned but with a definite column) add columns to the beginning and end of the implicit grid as necessary
            //     to accommodate those items.
            //        => Handled by expand_to_fit_range which expands the GridOccupancyMatrix as necessary
            //            -> Called by mark_area_as
            //            -> Called by record_grid_placement
            //
            // 3.3 If the largest column span among all the items without a definite column position is larger than the width of
            //     the implicit grid, add columns to the end of the implicit grid to accommodate that column span.
            //        => Handled by grid size estimate which is used to pre-size the GridOccupancyMatrix

            // 4. Position the remaining grid items
            // (which either have definite position only in the secondary axis or indefinite positions in both axis)
            val xNegTracks = cellOccupancyMatrix.trackCounts(AbsoluteAxis.HORIZONTAL).negativeImplicit.toShort()
            val yNegTracks = cellOccupancyMatrix.trackCounts(AbsoluteAxis.VERTICAL).negativeImplicit.toShort()
            val gridStartPosition =
                Pair(OriginZeroLine((-xNegTracks).toShort()), OriginZeroLine((-yNegTracks).toShort()))
            var gridPosition = gridStartPosition
            idx = 0
            childrenIter
                .filter { (_, _, childStyle) -> !childStyle.gridPlacement(secondaryAxis).isDefinite() }
                .map { (a, b, c) -> mapChildStyleToOriginZeroPlacement.invoke(a, b, c) }
                .forEach { (index, childNode, childPlacement, style) ->
                    idx += 1

                    // Compute placement
                    val (primarySpan, secondarySpan) = placeIndefinitelyPositionedItem(
                        cellOccupancyMatrix,
                        childPlacement,
                        gridAutoFlow,
                        gridPosition,
                    )

                    // Record item
                    recordGridPlacement(
                        cellOccupancyMatrix,
                        items,
                        childNode,
                        index,
                        style,
                        alignItems,
                        primaryAxis,
                        primarySpan,
                        secondarySpan,
                        CellOccupancyState.AUTO_PLACED,
                    )

                    // If using the "dense" placement algorithm then reset the grid position back to gridStartPosition ready for the next item
                    // Otherwise set it to the position of the current item so that the next item it placed after it.
                    gridPosition = when (gridAutoFlow.isDense()) {
                        true -> gridStartPosition
                        false -> Pair(primarySpan.end, secondarySpan.start)
                    }
                }
        }

        fun placeDefiniteGridItem(
            placement: InBothAbsAxis<Line<OriginZeroGridPlacement>>,
            primaryAxis: AbsoluteAxis,
        ): Pair<Line<OriginZeroLine>, Line<OriginZeroLine>> {
            // resolveSpansToTracks
            val primarySpan = placement.get(primaryAxis).resolveDefiniteGridLines()
            val secondarySpan = placement.get(primaryAxis.otherAxis()).resolveDefiniteGridLines()

            return Pair(primarySpan, secondarySpan)
        }

        fun placeDefiniteSecondaryAxisItem(
            cellOccupancyMatrix: CellOccupancyMatrix,
            placement: InBothAbsAxis<Line<OriginZeroGridPlacement>>,
            autoFlow: GridAutoFlow,
        ): Pair<Line<OriginZeroLine>, Line<OriginZeroLine>> {
            val primaryAxis = autoFlow.primaryAxis()
            val secondaryAxis = primaryAxis.otherAxis()

            val secondaryAxisPlacement = placement.get(secondaryAxis).resolveDefiniteGridLines()
            val primaryAxisGridStartLine = cellOccupancyMatrix.trackCounts(primaryAxis).implicitStartLine()
            val startingPosition = when (autoFlow.isDense()) {
                true -> primaryAxisGridStartLine
                false -> cellOccupancyMatrix
                    .lastOfType(primaryAxis, secondaryAxisPlacement.start, CellOccupancyState.AUTO_PLACED)
                    .unwrapOr(primaryAxisGridStartLine)
            }

            var position = startingPosition
            while (true) {
                val primaryAxisPlacement = placement.get(primaryAxis).resolveIndefiniteGridTracks(position)

                val doesFit = cellOccupancyMatrix.lineAreaIsUnoccupied(
                    primaryAxis,
                    primaryAxisPlacement,
                    secondaryAxisPlacement,
                )

                if (doesFit) {
                    return Pair(primaryAxisPlacement, secondaryAxisPlacement)
                } else {
                    position += 1
                }
            }
        }

        fun placeIndefinitelyPositionedItem(
            cellOccupancyMatrix: CellOccupancyMatrix,
            placement: InBothAbsAxis<Line<OriginZeroGridPlacement>>,
            autoFlow: GridAutoFlow,
            gridPosition: Pair<OriginZeroLine, OriginZeroLine>,
        ): Pair<Line<OriginZeroLine>, Line<OriginZeroLine>> {
            val primaryAxis = autoFlow.primaryAxis()

            val primaryPlacementStyle = placement.get(primaryAxis)
            val secondaryPlacementStyle = placement.get(primaryAxis.otherAxis())

            val primarySpan = primaryPlacementStyle.indefiniteSpan()
            val secondarySpan = secondaryPlacementStyle.indefiniteSpan()
            val hasDefinitePrimaryAxisPosition = primaryPlacementStyle.isDefinite()
            val primaryAxisGridStartLine = cellOccupancyMatrix.trackCounts(primaryAxis).implicitStartLine()
            val primaryAxisGridEndLine = cellOccupancyMatrix.trackCounts(primaryAxis).implicitEndLine()
            val secondaryAxisGridStartLine =
                cellOccupancyMatrix.trackCounts(primaryAxis.otherAxis()).implicitStartLine()

            val lineAreaIsOccupied = { primarySpan: Line<OriginZeroLine>, secondarySpan: Line<OriginZeroLine> ->
                !cellOccupancyMatrix.lineAreaIsUnoccupied(primaryAxis, primarySpan, secondarySpan)
            }

            var (primaryIdx, secondaryIdx) = gridPosition

            if (hasDefinitePrimaryAxisPosition) {
                val definitePrimaryPlacement = primaryPlacementStyle.resolveDefiniteGridLines()
                val definedPrimaryIdx = definitePrimaryPlacement.start;

                // Compute starting position for search
                if (definedPrimaryIdx < primaryIdx && secondaryIdx != secondaryAxisGridStartLine) {
                    secondaryIdx = secondaryAxisGridStartLine
                    primaryIdx = definedPrimaryIdx + 1
                } else {
                    primaryIdx = definedPrimaryIdx
                }

                // Item has fixed primary axis position: so we simply increment the secondary axis position
                // until we find a space that the item fits in
                while (true) {
                    val primarySpan = Line(start = primaryIdx, end = primaryIdx + primarySpan)
                    val secondarySpan = Line(start = secondaryIdx, end = secondaryIdx + secondarySpan)

                    // If area is occupied, increment the index and try again
                    if (lineAreaIsOccupied(primarySpan, secondarySpan)) {
                        secondaryIdx += 1
                        continue
                    }

                    // Once we find a free space, return that position
                    return Pair(primarySpan, secondarySpan)
                }
            } else {
                // Item does not have any fixed axis, so we search along the primary axis until we hit the end of the already
                // existent tracks, and then we reset the primary axis back to zero and increment the secondary axis index.
                // We continue in this vein until we find a space that the item fits in.
                while (true) {
                    val primarySpan = Line(start = primaryIdx, end = primaryIdx + primarySpan);
                    val secondarySpan = Line(start = secondaryIdx, end = secondaryIdx + secondarySpan);

                    // If the primary index is out of bounds, then increment the secondary index and reset the primary
                    // index back to the start of the grid
                    val primaryOutOfBounds = primarySpan.end > primaryAxisGridEndLine;
                    if (primaryOutOfBounds) {
                        secondaryIdx += 1;
                        primaryIdx = primaryAxisGridStartLine;
                        continue;
                    }

                    // If area is occupied, increment the primary index and try again
                    if (lineAreaIsOccupied(primarySpan, secondarySpan)) {
                        primaryIdx += 1;
                        continue;
                    }

                    // Once we find a free space that's in bounds, return that position
                    return Pair(primarySpan, secondarySpan);
                }
            }
        }

        fun recordGridPlacement(
            cellOccupancyMatrix: CellOccupancyMatrix,
            items: ArrayList<GridItem>,
            node: Node,
            index: Int,
            style: Style,
            parentAlignItems: AlignItems,
            primaryAxis: AbsoluteAxis,
            primarySpan: Line<OriginZeroLine>,
            secondarySpan: Line<OriginZeroLine>,
            placementType: CellOccupancyState,
        ) {
            // Mark area of grid as occupied
            cellOccupancyMatrix.markAreaAs(primaryAxis, primarySpan, secondarySpan, placementType)

            // Create grid item
            val (colSpan, rowSpan) = when (primaryAxis) {
                AbsoluteAxis.HORIZONTAL -> Pair(primarySpan, secondarySpan)
                AbsoluteAxis.VERTICAL -> Pair(secondarySpan, primarySpan)
            }
            items.add(GridItem.newWithPlacementStyleAndOrder(
                node,
                colSpan,
                rowSpan,
                style,
                parentAlignItems,
                index
            ))
        }
    }
}
