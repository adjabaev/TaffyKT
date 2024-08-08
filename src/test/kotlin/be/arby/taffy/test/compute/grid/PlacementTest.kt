package be.arby.taffy.test.compute.grid

import be.arby.taffy.Taffy
import net.asterium.taffy.compute.grid.ImplicitGrid
import net.asterium.taffy.compute.grid.Placement
import net.asterium.taffy.compute.grid.types.CellOccupancyMatrix
import net.asterium.taffy.compute.grid.types.GridItem
import net.asterium.taffy.compute.grid.types.TrackCounts
import net.asterium.taffy.maths.axis.AbsoluteAxis
import net.asterium.taffy.node.Node
import net.asterium.taffy.style.Style
import net.asterium.taffy.utils.StyleHelper.Companion.auto
import net.asterium.taffy.utils.StyleHelper.Companion.line
import net.asterium.taffy.utils.StyleHelper.Companion.span
import net.asterium.taffy.style.alignment.AlignSelf
import net.asterium.taffy.style.grid.GridAutoFlow
import net.asterium.taffy.style.grid.GridPlacement
import net.asterium.taffy.utils.tuples.Quadruple
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

typealias ExpectedPlacement = Quadruple<Short, Short, Short, Short>

class PlacementTest {

    fun placementTestRunner(
        explicitColCount: Int,
        explicitRowCount: Int,
        children: List<Quadruple<Int, Node, Style, ExpectedPlacement>>,
        expectedColCounts: TrackCounts,
        expectedRowCounts: TrackCounts,
        flow: GridAutoFlow,
    ) {
        // Setup test
        val childrenIter = children.map { (index, node, style, _) -> Triple(index, node, style) }
        val childStylesIter = children.map { (_, _, style, _) -> style }.iterator()
        val estimatedSizes = ImplicitGrid.computeGridSizeEstimate(explicitColCount, explicitRowCount, childStylesIter)
        val items = ArrayList<GridItem>()
        val cellOccupancyMatrix = CellOccupancyMatrix.withTrackCounts(estimatedSizes.first, estimatedSizes.second)

        // Run placement algorithm
        Placement.placeGridItems(
            cellOccupancyMatrix,
            items,
            childrenIter,
            flow,
            AlignSelf.START,
            AlignSelf.START
        )

        // Assert that each item has been placed in the right location
        val sortedChildren = children.sortedWith { f, s -> f.first.compareTo(s.first) }
        for ((idx, p) in (sortedChildren.zip(items).withIndex())) {
            val (q, item) = p
            val (_, node, _, expectedPlacement) = q
            Assertions.assertEquals(node, item.node)
            val actualPlacement = Quadruple(item.column.start, item.column.end, item.row.start, item.row.end)
            Assertions.assertEquals(expectedPlacement.intoOz(), actualPlacement, "Item $idx (0-indexed)")
        }

        // Assert that the correct number of implicit rows have been generated
        val actualRowCounts = cellOccupancyMatrix.trackCounts(AbsoluteAxis.VERTICAL)
        Assertions.assertEquals(expectedRowCounts, actualRowCounts, "row track counts")
        val actualColCounts = cellOccupancyMatrix.trackCounts(AbsoluteAxis.HORIZONTAL)
        Assertions.assertEquals(expectedColCounts, actualColCounts, "column track counts")
    }

    @Test
    fun `Test only fixed placement`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = listOf(
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(1),
                    auto<GridPlacement>(),
                    line<GridPlacement>(1),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(-4),
                    auto<GridPlacement>(),
                    line<GridPlacement>(-3),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple((-1).toShort(), 0.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(-3),
                    auto<GridPlacement>(),
                    line<GridPlacement>(-4),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), (-1).toShort(), 0.toShort())
            ),

            Quadruple(
                4, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(3),
                    span<GridPlacement>(2),
                    line<GridPlacement>(5),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(2.toShort(), 4.toShort(), 4.toShort(), 5.toShort())
            )
        )
        val expectedCols = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 2)
        val expectedRows = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 3)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun `Test placement spanning origin`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = listOf(
            // node, style (grid coords), expected_placement (oz coords)
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(-1),
                    line<GridPlacement>(-1),
                    line<GridPlacement>(-1),
                    line<GridPlacement>(-1)
                ).intoGridChild(),
                Quadruple(2.toShort(), 3.toShort(), 2.toShort(), 3.toShort())
            ),

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(-1),
                    span<GridPlacement>(2),
                    line<GridPlacement>(-1),
                    span<GridPlacement>(2)
                ).intoGridChild(),
                Quadruple(2.toShort(), 4.toShort(), 2.toShort(), 4.toShort())
            ),

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(-4),
                    line<GridPlacement>(-4),
                    line<GridPlacement>(-4),
                    line<GridPlacement>(-4)
                ).intoGridChild(),
                Quadruple((-1).toShort(), 0.toShort(), (-1).toShort(), 0.toShort())
            ),

            Quadruple(
                4, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(-4),
                    span<GridPlacement>(2),
                    line<GridPlacement>(-4),
                    span<GridPlacement>(2)
                ).intoGridChild(),
                Quadruple((-1).toShort(), 1.toShort(), (-1).toShort(), 1.toShort())
            ),
        )
        val expectedCols = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 2)
        val expectedRows = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 2)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow);
    }

    @Test
    fun `Test only auto placement row flow`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = listOf(
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(1.toShort(), 2.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 1.toShort(), 2.toShort())
            ),

            Quadruple(
                4, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(1.toShort(), 2.toShort(), 1.toShort(), 2.toShort())
            ),

            Quadruple(
                5, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 2.toShort(), 3.toShort())
            ),

            Quadruple(
                6, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(1.toShort(), 2.toShort(), 2.toShort(), 3.toShort())
            ),

            Quadruple(
                7, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 3.toShort(), 4.toShort())
            ),

            Quadruple(
                8, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(1.toShort(), 2.toShort(), 3.toShort(), 4.toShort())
            )
        )
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 2)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun `Test only auto placement column flow`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.COLUMN
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = listOf(
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 1.toShort(), 2.toShort())
            ),

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(1.toShort(), 2.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                4, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(1.toShort(), 2.toShort(), 1.toShort(), 2.toShort())
            ),

            Quadruple(
                5, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(2.toShort(), 3.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                6, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(2.toShort(), 3.toShort(), 1.toShort(), 2.toShort())
            ),

            Quadruple(
                7, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(3.toShort(), 4.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                8, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(3.toShort(), 4.toShort(), 1.toShort(), 2.toShort())
            )
        )
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 2)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun `Test oversized item`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = listOf(
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    span<GridPlacement>(5),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 5.toShort(), 0.toShort(), 1.toShort())
            )
        )
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 3)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun `Test fixed in secondary axis`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = listOf(
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    span<GridPlacement>(2),
                    auto<GridPlacement>(),
                    line<GridPlacement>(1),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 2.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    line<GridPlacement>(2),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 1.toShort(), 2.toShort())
            ),

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    line<GridPlacement>(1),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(2.toShort(), 3.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                4, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    line<GridPlacement>(4),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 3.toShort(), 4.toShort())
            )
        )
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 1)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 2)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun `Test definite in secondary axis with fully definite negative`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = listOf(
            // output order, node, style (grid coords), expected_placement (oz coords)

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    line<GridPlacement>(2),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 1.toShort(), 2.toShort())
            ),

            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(-4),
                    auto<GridPlacement>(),
                    line<GridPlacement>(2),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple((-1).toShort(), 0.toShort(), 1.toShort(), 2.toShort())
            ),

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    line<GridPlacement>(1),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple((-1).toShort(), 0.toShort(), 0.toShort(), 1.toShort())
            )
        )
        val expectedCols = TrackCounts(negativeImplicit = 1, explicit = 2, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun `Test dense packing algorithm`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW_DENSE
        val explicitColCount = 4
        val explicitRowCount = 4
        val children = listOf(
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(2),
                    auto<GridPlacement>(),
                    line<GridPlacement>(1),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(1.toShort(), 2.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    span<GridPlacement>(2),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(2.toShort(), 4.toShort(), 0.toShort(), 1.toShort())
            ),

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 1.toShort(), 0.toShort(), 1.toShort())
            )
        )
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 4, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 4, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun `Test sparse packing algorithm`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW
        val explicitColCount = 4
        val explicitRowCount = 4
        val children = listOf(
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    span<GridPlacement>(3),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 3.toShort(), 0.toShort(), 1.toShort())
            ), // Width 3

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    span<GridPlacement>(3),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(0.toShort(), 3.toShort(), 1.toShort(), 2.toShort())
            ), // Width 3 (wraps to next row)

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    span<GridPlacement>(1),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple(3.toShort(), 4.toShort(), 1.toShort(), 2.toShort())
            ) // Width 1 (uses second row as we're already on it)
        )
        val expectedCols = TrackCounts(negativeImplicit = 0, explicit = 4, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 4, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }

    @Test
    fun `Test auto placement in negative tracks`() {
        val taffy = be.arby.taffy.Taffy()

        val flow = GridAutoFlow.ROW_DENSE
        val explicitColCount = 2
        val explicitRowCount = 2
        val children = listOf(
            // output order, node, style (grid coords), expected_placement (oz coords)
            Quadruple(
                1, taffy.newLeaf(Style()),
                Quadruple(
                    line<GridPlacement>(-5),
                    auto<GridPlacement>(),
                    line<GridPlacement>(1),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple((-2).toShort(), (-1).toShort(), 0.toShort(), 1.toShort())
            ), // Row 1. Definitely positioned in column -2

            Quadruple(
                2, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    line<GridPlacement>(2),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple((-2).toShort(), (-1).toShort(), 1.toShort(), 2.toShort())
            ), // Row 2. Auto positioned in column -2

            Quadruple(
                3, taffy.newLeaf(Style()),
                Quadruple(
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>(),
                    auto<GridPlacement>()
                ).intoGridChild(),
                Quadruple((-1).toShort(), 0.toShort(), 0.toShort(), 1.toShort())
            ), // Row 1. Auto positioned in column -1
        )
        val expectedCols = TrackCounts(negativeImplicit = 2, explicit = 2, positiveImplicit = 0)
        val expectedRows = TrackCounts(negativeImplicit = 0, explicit = 2, positiveImplicit = 0)
        placementTestRunner(explicitColCount, explicitRowCount, children, expectedCols, expectedRows, flow)
    }
}
