package be.arby.taffy.tests.compute.grid

import be.arby.taffy.auto
import be.arby.taffy.compute.grid.computeGridSizeEstimate
import be.arby.taffy.compute.grid.placeGridItems
import be.arby.taffy.compute.grid.types.CellOccupancyMatrix
import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.compute.grid.types.TrackCounts
import be.arby.taffy.compute.grid.util.intoGridChild
import be.arby.taffy.compute.grid.util.intoOz
import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.lang.collections.enumerate
import be.arby.taffy.lang.collections.iter
import be.arby.taffy.lang.tuples.T3
import be.arby.taffy.lang.tuples.T4
import be.arby.taffy.line
import be.arby.taffy.span
import be.arby.taffy.style.Style
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.grid.GridAutoFlow
import be.arby.taffy.style.grid.GridPlacement
import be.arby.taffy.tests.assertEq
import be.arby.taffy.vec
import org.junit.jupiter.api.Test

typealias ExpectedPlacement = T4<Int, Int, Int, Int>
typealias T4GP = T4<GridPlacement, GridPlacement, GridPlacement, GridPlacement>

class TestPlacementAlgorithm {
    fun placementTestRunner(
        explicitColCount: Int,
        explicitRowCount: Int,
        children: MutableList<T3<Int, Style, ExpectedPlacement>>,
        expectedColCounts: TrackCounts,
        expectedRowCounts: TrackCounts,
        flow: GridAutoFlow
    ) {
        // Setup test
        val childrenIter = { children.map { (index, style, _) -> T3(index, index, style) } }
        val childStylesIter = children.map { (_, style, _) -> style }.iter()
        val estimatedSizes = computeGridSizeEstimate(explicitColCount, explicitRowCount, childStylesIter)
        val items = vec<GridItem>()
        val cellOccupancyMatrix =
            CellOccupancyMatrix.withTrackCounts(estimatedSizes.first, estimatedSizes.second)

        // Run placement algorithm
        placeGridItems(
            cellOccupancyMatrix,
            items,
            childrenIter,
            flow,
            AlignSelf.START,
            AlignSelf.START
        )

        // Assert that each item has been placed in the right location
        val sortedChildren = children.sortedWith { f, s -> f.first.compareTo(s.first) }
        for ((idx, v1) in sortedChildren.zip(items).enumerate()) {
            val (v2, item) = v1
            val (id, us, expectedPlacement) = v2
            assertEq(item.node, id)
            val actualPlacement = T4(item.column.start, item.column.end, item.row.start, item.row.end)
            assertEq(actualPlacement, (expectedPlacement).intoOz(), "Item $idx (0-indexed)")
        }

        // Assert that the correct number of implicit rows have been generated
        val actualRowCounts = cellOccupancyMatrix.trackCounts(AbsoluteAxis.VERTICAL)
        assertEq(actualRowCounts, expectedRowCounts, "row track counts")
        val actualColCounts = cellOccupancyMatrix.trackCounts(AbsoluteAxis.HORIZONTAL)
        assertEq(actualColCounts, expectedColCounts, "column track counts")
    }

    @Test
    fun test_only_fixed_placement() {
        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = run {
            vec(
                // node, style (grid coords), expected_placement (oz coords)
                T3(1, T4GP(line(1), auto(), line(1), auto()).intoGridChild(), T4(0, 1, 0, 1)),
                T3(2, T4GP(line(-4), auto(), line(-3), auto()).intoGridChild(), T4(-1, 0, 0, 1)),
                T3(3, T4GP(line(-3), auto(), line(-4), auto()).intoGridChild(), T4(0, 1, -1, 0)),
                T3(4, T4GP(line(3), span(2), line(5), auto()).intoGridChild(), T4(2, 4, 4, 5))
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 2)
        val expectedRows = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 3)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow);
    }

    @Test
    fun test_placement_spanning_origin() {
        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = run {
            vec(
                // node, style (grid coords), expected_placement (oz coords)
                T3(1, T4GP(line(-1), line(-1), line(-1), line(-1)).intoGridChild(), T4(2, 3, 2, 3)),
                T3(2, T4GP(line(-1), span(2), line(-1), span(2)).intoGridChild(), T4(2, 4, 2, 4)),
                T3(3, T4GP(line(-4), line(-4), line(-4), line(-4)).intoGridChild(), T4(-1, 0, -1, 0)),
                T3(4, T4GP(line(-4), span(2), line(-4), span(2)).intoGridChild(), T4(-1, 1, -1, 1))
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 2)
        val expectedRows = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 2)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun test_only_auto_placement_row_flow() {
        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = run {
            val autoChild = T4GP(auto(), auto(), auto(), auto()).intoGridChild()
            vec(
                // output order, node, style (grid coords), expected_placement (oz coords)
                T3(1, autoChild.clone(), T4(0, 1, 0, 1)),
                T3(2, autoChild.clone(), T4(1, 2, 0, 1)),
                T3(3, autoChild.clone(), T4(0, 1, 1, 2)),
                T3(4, autoChild.clone(), T4(1, 2, 1, 2)),
                T3(5, autoChild.clone(), T4(0, 1, 2, 3)),
                T3(6, autoChild.clone(), T4(1, 2, 2, 3)),
                T3(7, autoChild.clone(), T4(0, 1, 3, 4)),
                T3(8, autoChild.clone(), T4(1, 2, 3, 4))
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 2)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow);
    }

    @Test
    fun test_only_auto_placement_column_flow() {
        val flow = GridAutoFlow.COLUMN
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = run {
            val autoChild = T4GP(auto(), auto(), auto(), auto()).intoGridChild()
            vec(
                // output order, node, style (grid coords), expected_placement (oz coords)
                T3(1, autoChild.clone(), T4(0, 1, 0, 1)),
                T3(2, autoChild.clone(), T4(0, 1, 1, 2)),
                T3(3, autoChild.clone(), T4(1, 2, 0, 1)),
                T3(4, autoChild.clone(), T4(1, 2, 1, 2)),
                T3(5, autoChild.clone(), T4(2, 3, 0, 1)),
                T3(6, autoChild.clone(), T4(2, 3, 1, 2)),
                T3(7, autoChild.clone(), T4(3, 4, 0, 1)),
                T3(8, autoChild.clone(), T4(3, 4, 1, 2))
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 2)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow);
    }

    @Test
    fun test_oversized_item() {
        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = run {
            vec(
                // output order, node, style (grid coords), expected_placement (oz coords)
                T3(1, T4GP(span(5), auto(), auto(), auto()).intoGridChild(), T4(0, 5, 0, 1))
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 3)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow);
    }

    @Test
    fun test_fixed_in_secondary_axis() {
        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = run {
            vec(
                // output order, node, style (grid coords), expected_placement (oz coords)
                T3(1, T4GP(span(2), auto(), line(1), auto()).intoGridChild(), T4(0, 2, 0, 1)),
                T3(2, T4GP(auto(), auto(), line(2), auto()).intoGridChild(), T4(0, 1, 1, 2)),
                T3(3, T4GP(auto(), auto(), line(1), auto()).intoGridChild(), T4(2, 3, 0, 1)),
                T3(4, T4GP(auto(), auto(), line(4), auto()).intoGridChild(), T4(0, 1, 3, 4)),
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 1)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 2)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow);
    }

    @Test
    fun test_definite_in_secondary_axis_with_fully_definite_negative() {
        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = run {
            vec(
                // output order, node, style (grid coords), expected_placement (oz coords)
                T3(2, T4GP(auto(), auto(), line(2), auto()).intoGridChild(), T4(0, 1, 1, 2)),
                T3(1, T4GP(line(-4), auto(), line(2), auto()).intoGridChild(), T4(-1, 0, 1, 2)),
                T3(3, T4GP(auto(), auto(), line(1), auto()).intoGridChild(), T4(-1, 0, 0, 1))
            )
        };
        val expectedCols = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun test_dense_packing_algorithm() {
        val flow = GridAutoFlow.ROW_DENSE;
        val explicitColCount = 4;
        val explicitRowCount = 4;
        val children = run {
            vec(
                // output order, node, style (grid coords), expected_placement (oz coords)
                T3(
                    1,
                    T4GP(line(2), auto(), line(1), auto()).intoGridChild(),
                    T4(1, 2, 0, 1)
                ), // Definitely positioned in column 2
                T3(
                    2,
                    T4GP(span(2), auto(), auto(), auto()).intoGridChild(),
                    T4(2, 4, 0, 1)
                ), // Spans 2 columns, so positioned after item 1
                T3(
                    3,
                    T4GP(auto(), auto(), auto(), auto()).intoGridChild(),
                    T4(0, 1, 0, 1)
                ), // Spans 1 column, so should be positioned before item 1
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 4, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 4, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun test_sparse_packing_algorithm() {
        val flow = GridAutoFlow.ROW
        val explicitColCount = 4
        val explicitRowCount = 4
        val children = run {
            vec(
                // output order, node, style (grid coords), expected_placement (oz coords)
                T3(1, T4GP(auto(), span(3), auto(), auto()).intoGridChild(), T4(0, 3, 0, 1)), // Width 3
                T3(
                    2,
                    T4GP(auto(), span(3), auto(), auto()).intoGridChild(),
                    T4(0, 3, 1, 2)
                ), // Width 3 (wraps to next row)
                T3(
                    3,
                    T4GP(auto(), span(1), auto(), auto()).intoGridChild(),
                    T4(3, 4, 1, 2)
                ), // Width 1 (uses second row as we're already on it)
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 4, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 4, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun test_auto_placement_in_negative_tracks() {
        val flow = GridAutoFlow.ROW_DENSE
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = run {
            vec(
                // output order, node, style (grid coords), expected_placement (oz coords)
                T3(
                    1,
                    T4GP(line(-5), auto(), line(1), auto()).intoGridChild(),
                    T4(-2, -1, 0, 1)
                ), // Row 1. Definitely positioned in column -2
                T3(
                    2,
                    T4GP(auto(), auto(), line(2), auto()).intoGridChild(),
                    T4(-2, -1, 1, 2)
                ), // Row 2. Auto positioned in column -2
                T3(
                    3,
                    T4GP(auto(), auto(), auto(), auto()).intoGridChild(),
                    T4(-1, 0, 0, 1)
                ), // Row 1. Auto positioned in column -1
            )
        }
        val expectedCols = TrackCounts(negativeImplicit = 2, explicit = 2, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }
}
