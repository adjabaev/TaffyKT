package be.arby.taffy.tests.style

import be.arby.taffy.style.flex.FlexDirection
import org.junit.jupiter.api.Test
import be.arby.taffy.tests.assert

class TestFlexDirection {
    @Test
    fun `flex direction is row`() {
        assert(FlexDirection.ROW.isRow())
        assert(FlexDirection.ROW_REVERSE.isRow())
        assert(!FlexDirection.COLUMN.isRow())
        assert(!FlexDirection.COLUMN_REVERSE.isRow())
    }

    @Test
    fun `flex direction is column`() {
        assert(!FlexDirection.ROW.isColumn())
        assert(!FlexDirection.ROW_REVERSE.isColumn())
        assert(FlexDirection.COLUMN.isColumn())
        assert(FlexDirection.COLUMN_REVERSE.isColumn())
    }

    @Test
    fun `flex direction is revese`() {
        assert(!FlexDirection.ROW.isReverse())
        assert(FlexDirection.ROW_REVERSE.isReverse())
        assert(!FlexDirection.COLUMN.isReverse())
        assert(FlexDirection.COLUMN_REVERSE.isReverse())
    }
}
