package be.arby.taffy.test

import be.arby.taffy.Taffy
import net.asterium.taffy.geometry.Rect
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.AvailableSpace
import net.asterium.taffy.style.dimension.LengthPercentage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BorderAndPaddingTest {
    private fun <T> arrToRect(items: Array<T>): Rect<T> {
        return Rect(left = items[0], right = items[1], top = items[2], bottom = items[3])
    }

    @Test
    fun `Border on a single axis doesn't increase size`() {
        for (i in 0 until 4) {
            val taffy = be.arby.taffy.Taffy()

            val lengths = Array(4) { LengthPercentage.ZERO }
            lengths[i] = LengthPercentage.Length(10f)

            val node = taffy.newLeaf(
                Style(
                    border = arrToRect(lengths)
                )
            )

            taffy.computeLayout(node, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

            val layout = taffy.layout(node)
            Assertions.assertEquals(0f, layout.size.width * layout.size.height)
        }
    }

    @Test
    fun `Padding on a single axis doesn't increase size`() {
        for (i in 0 until 4) {
            val taffy = be.arby.taffy.Taffy()

            val lengths = Array(4) { LengthPercentage.ZERO }
            lengths[i] = LengthPercentage.Length(10f)

            val node = taffy.newLeaf(
                Style(
                    padding = arrToRect(lengths)
                )
            )

            taffy.computeLayout(node, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

            val layout = taffy.layout(node)
            Assertions.assertEquals(0f, layout.size.width * layout.size.height)
        }
    }

    @Test
    fun `Border and padding on a single axis doesn't increase size`() {
        for (i in 0 until 4) {
            val taffy = be.arby.taffy.Taffy()

            val lengths = Array(4) { LengthPercentage.ZERO }
            lengths[i] = LengthPercentage.Length(10f)
            val rect = arrToRect(lengths)

            val node = taffy.newLeaf(
                Style(
                    border = rect,
                    padding = rect
                )
            )

            taffy.computeLayout(node, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

            val layout = taffy.layout(node)
            Assertions.assertEquals(0f, layout.size.width * layout.size.height)
        }
    }

    @Test
    fun `Vertical border and padding percentage values use available space correctly`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(
            Style(
                padding = Rect(
                    left = LengthPercentage.Percent(1.0f),
                    right = LengthPercentage.Percent(),
                    top = LengthPercentage.Percent(1.0f),
                    bottom = LengthPercentage.Percent(),
                )
            )
        )

        taffy.computeLayout(node, Size(AvailableSpace.fromPoints(200f), AvailableSpace.fromPoints(100f)))

        val layout = taffy.layout(node)
        Assertions.assertEquals(200f, layout.size.width)
        Assertions.assertEquals(200f, layout.size.height)
    }
}
