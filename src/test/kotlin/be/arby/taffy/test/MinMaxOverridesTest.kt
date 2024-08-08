package be.arby.taffy.test

import be.arby.taffy.Taffy
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.AvailableSpace
import net.asterium.taffy.style.dimension.Dimension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MinMaxOverridesTest {

    @Test
    fun `Min overrides max`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.fromPoints(50f), height = Dimension.fromPoints(50f)),
                minSize = Size(width = Dimension.fromPoints(100f), height = Dimension.fromPoints(100f)),
                maxSize = Size(width = Dimension.fromPoints(10f), height = Dimension.fromPoints(10f))
            )
        )

        taffy.computeLayout(child, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

        Assertions.assertEquals(Size(width = 100.0f, height = 100.0f), taffy.layout(child).size)
    }

    @Test
    fun `Max overrides size`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.fromPoints(50f), height = Dimension.fromPoints(50f)),
                maxSize = Size(width = Dimension.fromPoints(10f), height = Dimension.fromPoints(10f))
            )
        )

        taffy.computeLayout(child, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

        Assertions.assertEquals(Size(width = 10.0f, height = 10.0f), taffy.layout(child).size)
    }

    @Test
    fun `Min overrides size`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.fromPoints(50f), height = Dimension.fromPoints(50f)),
                minSize = Size(width = Dimension.fromPoints(100f), height = Dimension.fromPoints(100f))
            )
        )

        taffy.computeLayout(child, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

        Assertions.assertEquals(Size(width = 100.0f, height = 100.0f), taffy.layout(child).size)
    }
}
