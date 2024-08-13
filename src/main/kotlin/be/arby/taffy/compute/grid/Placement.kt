package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.CellOccupancyMatrix
import be.arby.taffy.compute.grid.types.CellOccupancyState
import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.geom.*
import be.arby.taffy.lang.tuples.T2
import be.arby.taffy.lang.tuples.T3
import be.arby.taffy.lang.tuples.T4
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.grid.GridAutoFlow
import be.arby.taffy.style.grid.GridItemStyle
import be.arby.taffy.style.grid.OriginZeroGridPlacement
import be.arby.taffy.tree.NodeId

/**
 * 8.5. Grid Item Placement Algorithm
 * Place items into the grid, generating new rows/column into the implicit grid as required
 * [Specification](https://www.w3.org/TR/css-grid-2/#auto-placement-algo)
 */
fun <S: GridItemStyle, ChildIter: List<T3<Int, NodeId, S>>> placeGridItems(
    cellOccupancyMatrix: CellOccupancyMatrix,
    items: MutableList<GridItem>,
    childrenIter: () -> ChildIter,
    gridAutoFlow: GridAutoFlow,
    alignItems: AlignItems,
    justifyItems: AlignItems,
) {
    var primaryAxis = gridAutoFlow.primaryAxis()
    var secondaryAxis = primaryAxis.otherAxis()

    val mapChildStyleToOriginZeroPlacement: (Int, NodeId, S) -> T4<Int, NodeId, InBothAbsAxis<Line<OriginZeroGridPlacement>>, S> = { index, node, style ->
        val explicitColCount = cellOccupancyMatrix.trackCounts(AbsoluteAxis.HORIZONTAL).explicit
        val explicitRowCount = cellOccupancyMatrix.trackCounts(AbsoluteAxis.VERTICAL).explicit

        val originZeroPlacement = InBothAbsAxis(
            horizontal = style.gridColumn().map { placement -> placement.intoOriginZeroPlacement(explicitColCount) },
            vertical = style.gridRow().map { placement -> placement.intoOriginZeroPlacement(explicitRowCount) }
        )

        T4(index, node, originZeroPlacement, style)
    }

    // 1. Place children with definite positions
    var idx = 0
    childrenIter()
        .filter { (_, _, childStyle) -> childStyle.gridRow().isDefinite() && childStyle.gridColumn().isDefinite() }
        .map { (index, node, style) -> mapChildStyleToOriginZeroPlacement(index, node, style) }
        .forEach { (index, childNode, childPlacement, style) ->
            idx += 1

            val (rowSpan, colSpan) = placeDefiniteGridItem(childPlacement, primaryAxis)
            recordGridPlacement(
                cellOccupancyMatrix,
                items,
                childNode,
                index,
                style,
                alignItems,
                justifyItems,
                primaryAxis,
                rowSpan,
                colSpan,
                CellOccupancyState.DEFINITELY_PLACED
            )
        }

    // 2. Place remaining children with definite secondary axis positions
    idx = 0
    childrenIter()
        .filter { (_, _, childStyle) -> childStyle.gridPlacement(secondaryAxis).isDefinite() && !childStyle.gridPlacement(primaryAxis).isDefinite() }
        .map { (index, node, style) -> mapChildStyleToOriginZeroPlacement(index, node, style) }
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
                justifyItems,
                primaryAxis,
                primarySpan,
                secondarySpan,
                CellOccupancyState.AUTOPLACED
            )
        }

        // 3. Determine the number of columns in the implicit grid
        // By the time we get to this point in the execution, this is actually already accounted for:
        //
        // 3.1 Start with the columns from the explicit grid
        //        -> Handled by grid size estimate which is used to pre-size the GridOccupancyMatrix
        //
        // 3.2 Among all the items with a definite column position (explicitly positioned items, items positioned in the previous step,
        //     and items not yet positioned but with a definite column) add columns to the beginning and end of the implicit grid as necessary
        //     to accommodate those items.
        //        -> Handled by expand_to_fit_range which expands the GridOccupancyMatrix as necessary
        //            -> Called by mark_area_as
        //            -> Called by record_grid_placement
        //
        // 3.3 If the largest column span among all the items without a definite column position is larger than the width of
        //     the implicit grid, add columns to the end of the implicit grid to accommodate that column span.
        //        -> Handled by grid size estimate which is used to pre-size the GridOccupancyMatrix

        // 4. Position the remaining grid items
        // (which either have definite position only in the secondary axis or indefinite positions in both axis)
        primaryAxis = gridAutoFlow.primaryAxis()
        secondaryAxis = primaryAxis.otherAxis()
        val primaryNegTracks = cellOccupancyMatrix.trackCounts(primaryAxis).negativeImplicit
        val secondaryNegTracks = cellOccupancyMatrix.trackCounts(secondaryAxis).negativeImplicit
        val gridStartPosition = T2(OriginZeroLine(-primaryNegTracks), OriginZeroLine(-secondaryNegTracks))
        var gridPosition = gridStartPosition
        idx = 0
        childrenIter()
            .filter { (_, _, childStyle) -> !childStyle.gridPlacement(secondaryAxis).isDefinite() }
            .map { (index, node, style) -> mapChildStyleToOriginZeroPlacement(index, node, style) }
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
                    justifyItems,
                    primaryAxis,
                    primarySpan,
                    secondarySpan,
                    CellOccupancyState.AUTOPLACED,
                )

                // If using the "dense" placement algorithm then reset the grid position back to grid_start_position ready for the next item
                // Otherwise set it to the position of the current item so that the next item it placed after it.
                gridPosition = when (gridAutoFlow.isDense()) {
                    true -> gridStartPosition
                    false -> T2(primarySpan.end, secondarySpan.start)
                }
            }
}

/**
 * 8.5. Grid Item Placement Algorithm
 * Place a single definitely placed item into the grid
 */
fun placeDefiniteGridItem(
    placement: InBothAbsAxis<Line<OriginZeroGridPlacement>>,
    primaryAxis: AbsoluteAxis,
): T2<Line<OriginZeroLine>, Line<OriginZeroLine>> {
    // Resolve spans to tracks
    val primarySpan = placement.get(primaryAxis).resolveDefiniteGridLines()
    val secondarySpan = placement.get(primaryAxis.otherAxis()).resolveDefiniteGridLines()

    return T2(primarySpan, secondarySpan)
}

/**
 * 8.5. Grid Item Placement Algorithm
 * Step 2. Place remaining children with definite secondary axis positions
 */
fun placeDefiniteSecondaryAxisItem(
cellOccupancyMatrix: CellOccupancyMatrix,
placement: InBothAbsAxis<Line<OriginZeroGridPlacement>>,
autoFlow: GridAutoFlow,
): T2<Line<OriginZeroLine>, Line<OriginZeroLine>> {
    val primaryAxis = autoFlow.primaryAxis()
    val secondaryAxis = primaryAxis.otherAxis()

    val secondaryAxisPlacement = placement.get(secondaryAxis).resolveDefiniteGridLines()
    val primaryAxisGridStartLine = cellOccupancyMatrix.trackCounts(primaryAxis).implicitStartLine()
    val startingPosition = when (autoFlow.isDense()) {
        true -> primaryAxisGridStartLine
        false -> cellOccupancyMatrix
        .lastOfType(primaryAxis, secondaryAxisPlacement.start, CellOccupancyState.AUTOPLACED)
        .unwrapOr(primaryAxisGridStartLine)
    }

    var position: OriginZeroLine = startingPosition
    while (true) {
        val primaryAxisPlacement = placement.get(primaryAxis).resolveIndefiniteGridTracks(position)

        val doesFit = cellOccupancyMatrix.lineAreaIsUnoccupied(
                primaryAxis,
                primaryAxisPlacement,
                secondaryAxisPlacement
        )

        if (doesFit) {
            return T2(primaryAxisPlacement, secondaryAxisPlacement)
        } else {
            position += 1
        }
    }
}

/**
 * 8.5. Grid Item Placement Algorithm
 * Step 4. Position the remaining grid items.
 */
fun placeIndefinitelyPositionedItem(
cellOccupancyMatrix: CellOccupancyMatrix,
placement: InBothAbsAxis<Line<OriginZeroGridPlacement>>,
autoFlow: GridAutoFlow,
gridPosition: T2<OriginZeroLine, OriginZeroLine>,
): T2<Line<OriginZeroLine>, Line<OriginZeroLine>> {
    val primaryAxis = autoFlow.primaryAxis()

    val primaryPlacementStyle = placement.get(primaryAxis)
    val secondaryPlacementStyle = placement.get(primaryAxis.otherAxis())

    val secondarySpan = secondaryPlacementStyle.indefiniteSpan()
    val hasDefinitePrimaryAxisPosition = primaryPlacementStyle.isDefinite()
    val primaryAxisGridStartLine = cellOccupancyMatrix.trackCounts(primaryAxis).implicitStartLine()
    val primaryAxisGridEndLine = cellOccupancyMatrix.trackCounts(primaryAxis).implicitEndLine()
    val secondaryAxisGridStartLine =
    cellOccupancyMatrix.trackCounts(primaryAxis.otherAxis()).implicitStartLine()

    val lineAreaIsOccupied: (Line<OriginZeroLine>, Line<OriginZeroLine>) -> Boolean = { primarySpan, secondarySpan ->
        !cellOccupancyMatrix.lineAreaIsUnoccupied(primaryAxis, primarySpan, secondarySpan)
    }

    var (primaryIdx, secondaryIdx) = gridPosition

    if (hasDefinitePrimaryAxisPosition) {
        val definitePrimaryPlacement = primaryPlacementStyle.resolveDefiniteGridLines()
        val definedPrimaryIdx = definitePrimaryPlacement.start

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
            val primarySpan = Line(start = primaryIdx, end = primaryIdx + definitePrimaryPlacement.span())
            val secondarySpan = Line(start = secondaryIdx, end = secondaryIdx + secondarySpan)

            // If area is occupied, increment the index and try again
            if (lineAreaIsOccupied(primarySpan, secondarySpan)) {
                secondaryIdx += 1
                continue
            }

            // Once we find a free space, return that position
            return T2(primarySpan, secondarySpan)
        }
    } else {
        val primarySpan = primaryPlacementStyle.indefiniteSpan()

        // Item does not have any fixed axis, so we search along the primary axis until we hit the end of the already
        // existent tracks, and then we reset the primary axis back to zero and increment the secondary axis index.
        // We continue in this vein until we find a space that the item fits in.
        while (true) {
            val primarySpan = Line(start = primaryIdx, end = primaryIdx + primarySpan )
            val secondarySpan = Line(start = secondaryIdx, end = secondaryIdx + secondarySpan )

            // If the primary index is out of bounds, then increment the secondary index and reset the primary
            // index back to the start of the grid
            val primaryOutOfBounds = primarySpan.end > primaryAxisGridEndLine
            if (primaryOutOfBounds) {
                secondaryIdx += 1
                primaryIdx = primaryAxisGridStartLine
                continue
            }

            // If area is occupied, increment the primary index and try again
            if (lineAreaIsOccupied(primarySpan, secondarySpan)) {
                primaryIdx += 1
                continue
            }

            // Once we find a free space that's in bounds, return that position
            return T2(primarySpan, secondarySpan)
        }
    }
}

/**
 * Record the grid item in both CellOccupancyMatric and the GridItems list
 * once a definite placement has been determined
 */
fun <S: GridItemStyle> recordGridPlacement(
    cellOccupancyMatrix: CellOccupancyMatrix,
    items: MutableList<GridItem>,
    node: NodeId,
    index: Int,
    style: S,
    parentAlignItems: AlignItems,
    parentJustifyItems: AlignItems,
    primaryAxis: AbsoluteAxis,
    primarySpan: Line<OriginZeroLine>,
    secondarySpan: Line<OriginZeroLine>,
    placementType: CellOccupancyState,
) {
    // Mark area of grid as occupied
    cellOccupancyMatrix.markAreaAs(primaryAxis, primarySpan, secondarySpan, placementType)

    // Create grid item
    val (colSpan, rowSpan) = when (primaryAxis) {
        AbsoluteAxis.HORIZONTAL -> T2(primarySpan, secondarySpan)
        AbsoluteAxis.VERTICAL -> T2(secondarySpan, primarySpan)
    }
    items.add(GridItem.newWithPlacementStyleAndOrder(
        node,
        colSpan,
        rowSpan,
        style,
        parentAlignItems,
        parentJustifyItems,
        index,
    ))
}
