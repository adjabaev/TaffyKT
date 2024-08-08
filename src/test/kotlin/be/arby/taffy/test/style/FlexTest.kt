package be.arby.taffy.test.style

import net.asterium.taffy.style.flex.FlexDirection
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FlexTest {

    @Test
    fun `Flex direction is row`() {
        Assertions.assertEquals(true, FlexDirection.ROW.isRow())
        Assertions.assertEquals(true, FlexDirection.ROW_REVERSE.isRow())
        Assertions.assertEquals(false, FlexDirection.COLUMN.isRow())
        Assertions.assertEquals(false, FlexDirection.COLUMN_REVERSE.isRow())
    }

    @Test
    fun `Flex direction is column`() {
        Assertions.assertEquals(false, FlexDirection.ROW.isColumn())
        Assertions.assertEquals(false, FlexDirection.ROW_REVERSE.isColumn())
        Assertions.assertEquals(true, FlexDirection.COLUMN.isColumn())
        Assertions.assertEquals(true, FlexDirection.COLUMN_REVERSE.isColumn())
    }

    @Test
    fun `Flex direction is reverse`() {
        Assertions.assertEquals(false, FlexDirection.ROW.isReverse())
        Assertions.assertEquals(true, FlexDirection.ROW_REVERSE.isReverse())
        Assertions.assertEquals(false, FlexDirection.COLUMN.isReverse())
        Assertions.assertEquals(true, FlexDirection.COLUMN_REVERSE.isReverse())
    }
}
