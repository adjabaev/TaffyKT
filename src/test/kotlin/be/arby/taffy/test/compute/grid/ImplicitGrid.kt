package be.arby.taffy.test.compute.grid

import net.asterium.taffy.compute.grid.ImplicitGrid
import net.asterium.taffy.compute.grid.types.OriginZeroLine
import net.asterium.taffy.geometry.Line
import net.asterium.taffy.utils.StyleHelper.Companion.auto
import net.asterium.taffy.utils.StyleHelper.Companion.line
import net.asterium.taffy.utils.StyleHelper.Companion.span
import net.asterium.taffy.style.grid.GridPlacement
import net.asterium.taffy.utils.tuples.Quadruple
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ImplicitGrid {
    @Test
    fun `Child min max line auto`() {
        val (minCol, maxCol, span) = ImplicitGrid.childMinLineMaxLineSpan(
            Line(
                start = line(5),
                end = span(6)
            ), 6
        )
        Assertions.assertEquals(OriginZeroLine(4), minCol)
        Assertions.assertEquals(OriginZeroLine(10), maxCol)
        Assertions.assertEquals(1, span)
    }

    @Test
    fun `Child min max line negative track`() {
        val (minCol, maxCol, span) = ImplicitGrid.childMinLineMaxLineSpan(
            Line(
                start = line(-5),
                end = span(3)
            ), 6
        )
        Assertions.assertEquals(OriginZeroLine(2), minCol)
        Assertions.assertEquals(OriginZeroLine(5), maxCol)
        Assertions.assertEquals(1, span)
    }

    @Test
    fun `Explicit grid sizing with children`() {
        val explicitColCount = 6
        val explicitRowCount = 8
        val childStyles = listOf(
            Quadruple(
                line<GridPlacement>(1),
                span<GridPlacement>(2),
                line<GridPlacement>(2),
                auto<GridPlacement>()
            ).intoGridChild(),
            Quadruple(
                line<GridPlacement>(-4),
                auto<GridPlacement>(),
                line<GridPlacement>(-2),
                auto<GridPlacement>()
            ).intoGridChild()
        )
        val (inline, block) = ImplicitGrid.computeGridSizeEstimate(
            explicitColCount,
            explicitRowCount,
            childStyles.iterator()
        )
        Assertions.assertEquals(0, inline.negativeImplicit)
        Assertions.assertEquals(explicitColCount, inline.explicit)
        Assertions.assertEquals(0, inline.positiveImplicit)
        Assertions.assertEquals(0, block.negativeImplicit)
        Assertions.assertEquals(explicitRowCount, block.explicit)
        Assertions.assertEquals(0, block.positiveImplicit)
    }

    @Test
    fun `Negative implicit grid sizing`() {
        val explicitColCount = 4
        val explicitRowCount = 4
        val childStyles = listOf(
            Quadruple(
                line<GridPlacement>(-6),
                span<GridPlacement>(2),
                line<GridPlacement>(-8),
                auto<GridPlacement>()
            ).intoGridChild(),
            Quadruple(
                line<GridPlacement>(4),
                auto<GridPlacement>(),
                line<GridPlacement>(3),
                auto<GridPlacement>()
            ).intoGridChild()
        )
        val (inline, block) = ImplicitGrid.computeGridSizeEstimate(
            explicitColCount,
            explicitRowCount,
            childStyles.iterator()
        )
        Assertions.assertEquals(1, inline.negativeImplicit)
        Assertions.assertEquals(explicitColCount, inline.explicit)
        Assertions.assertEquals(0, inline.positiveImplicit)
        Assertions.assertEquals(3, block.negativeImplicit)
        Assertions.assertEquals(explicitRowCount, block.explicit)
        Assertions.assertEquals(0, block.positiveImplicit)
    }
}
