package be.arby.taffy.test

import be.arby.taffy.Taffy
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.lang.Option
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.AvailableSpace
import net.asterium.taffy.style.dimension.Dimension
import net.asterium.taffy.style.alignment.AlignSelf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ReLayoutTest {

    @Test
    fun relayout() {
        val taffy = be.arby.taffy.Taffy()

        val node1 = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.fromPoints(8f), height = Dimension.fromPoints(80f))
            )
        )

        val node0 = taffy.newLeafWithChildren(
            Style(
                alignSelf = Option.Some(AlignSelf.CENTER),
                size = Size(width = Dimension.makeAuto(), height = Dimension.makeAuto())
            ), listOf(node1)
        )

        val node = taffy.newLeafWithChildren(
            Style(
                size = Size(width = Dimension.fromPercent(1f), height = Dimension.fromPercent(1f))
            ), listOf(node0)
        )

        println("0:")

        taffy.computeLayout(node, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

        val initial = taffy.layout(node).location
        val initial0 = taffy.layout(node0).location
        val initial1 = taffy.layout(node1).location

        for (i in 1 until 10) {
            println("\n\n$i:")
        }

        taffy.computeLayout(node, Size(AvailableSpace.fromPoints(100f), AvailableSpace.fromPoints(100f)))

        Assertions.assertEquals(initial, taffy.layout(node).location)
        Assertions.assertEquals(initial0, taffy.layout(node0).location)
        Assertions.assertEquals(initial1, taffy.layout(node1).location)
    }
}
