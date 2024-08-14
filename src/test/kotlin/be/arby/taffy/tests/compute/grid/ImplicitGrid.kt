package be.arby.taffy.tests.compute.grid

import be.arby.taffy.auto
import be.arby.taffy.compute.grid.childMinLineMaxLineSpan
import be.arby.taffy.compute.grid.computeGridSizeEstimate
import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.compute.grid.util.intoGridChild
import be.arby.taffy.geom.Line
import be.arby.taffy.lang.collections.iter
import be.arby.taffy.lang.tuples.T4
import be.arby.taffy.line
import be.arby.taffy.span
import be.arby.taffy.style.grid.GridPlacement
import be.arby.taffy.tests.assertEq
import be.arby.taffy.vec
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestChildMinMaxLine {
    @Test
    fun `child min max line auto`() {
        val (minCol, maxCol, span) = childMinLineMaxLineSpan(Line(start = line(5), end = span(6)), 6)
        assertEq(minCol, OriginZeroLine(4))
        assertEq(maxCol, OriginZeroLine(10))
        assertEquals(span, 1)
    }

    @Test
    fun `child min max line negative track`() {
        val (minCol, maxCol, span) = childMinLineMaxLineSpan(Line(start = line(-5), end = span(3)), 6)
        assertEq(minCol, OriginZeroLine(2))
        assertEq(maxCol, OriginZeroLine(5))
        assertEq(span, 1)
    }
}

class TestInitialGridSizing {
    @Test
    fun `explicit grid sizing with children`() {
        val explicitColCount = 6
        val explicitRowCount = 8
        val childStyles = vec(
            T4<GridPlacement, GridPlacement, GridPlacement, GridPlacement>(
                line(1),
                span(2),
                line(2),
                auto()
            ).intoGridChild(),
            T4<GridPlacement, GridPlacement, GridPlacement, GridPlacement>(
                line(-4),
                auto(),
                line(-2),
                auto()
            ).intoGridChild()
        )
        val (inline, block) =
            computeGridSizeEstimate(explicitColCount, explicitRowCount, childStyles.iter())
        assertEq(inline.negativeImplicit, 0)
        assertEq(inline.explicit, explicitColCount)
        assertEq(inline.positiveImplicit, 0)
        assertEq(block.negativeImplicit, 0)
        assertEq(block.explicit, explicitRowCount)
        assertEq(block.positiveImplicit, 0)
    }

    @Test
    fun `negative implicit grid sizing`() {
        val explicitColCount = 4
        val explicitRowCount = 4
        val childStyles = vec(
            T4<GridPlacement, GridPlacement, GridPlacement, GridPlacement>(
                line(-6), span(2), line(-8), auto()
            ).intoGridChild(),
            T4<GridPlacement, GridPlacement, GridPlacement, GridPlacement>(
                line(4), auto(), line(3), auto()
            ).intoGridChild(),
        )
        val (inline, block) =
            computeGridSizeEstimate(explicitColCount, explicitRowCount, childStyles.iter())
        assertEq(inline.negativeImplicit, 1)
        assertEq(inline.explicit, explicitColCount)
        assertEq(inline.positiveImplicit, 0)
        assertEq(block.negativeImplicit, 3)
        assertEq(block.explicit, explicitRowCount)
        assertEq(block.positiveImplicit, 0)
    }
}
