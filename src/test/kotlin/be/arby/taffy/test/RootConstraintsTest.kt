package be.arby.taffy.test

import be.arby.taffy.Taffy
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.AvailableSpace
import net.asterium.taffy.style.dimension.Dimension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RootConstraintsTest {

    @Test
    fun `Root with percentage size`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.fromPercent(1f), height = Dimension.fromPercent(1f))
            )
        )

        taffy.computeLayout(node, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(200f)))

        Assertions.assertEquals(100f, taffy.layout(node).size.width)
        Assertions.assertEquals(200f, taffy.layout(node).size.height)
    }

    @Test
    fun `Root with no size`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(
            Style()
        )

        taffy.computeLayout(node, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

        Assertions.assertEquals(0f, taffy.layout(node).size.width)
        Assertions.assertEquals(0f, taffy.layout(node).size.height)
    }

    @Test
    fun `Root with larger size`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.fromPoints(200f), height = Dimension.fromPoints(200f))
            )
        )

        taffy.computeLayout(node, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

        Assertions.assertEquals(200f, taffy.layout(node).size.width)
        Assertions.assertEquals(200f, taffy.layout(node).size.height)
    }
}
