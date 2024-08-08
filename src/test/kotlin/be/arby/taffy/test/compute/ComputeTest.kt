package be.arby.taffy.test.compute

import be.arby.taffy.Taffy
import net.asterium.taffy.compute.Compute
import net.asterium.taffy.geometry.Point
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.Display
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ComputeTest {
    @Test
    fun `Hidden layout should hide recursively`() {
        val taffy = be.arby.taffy.Taffy()

        val style = Style(display = Display.FLEX, size = Size.fromPoints(50f, 50f))

        val grandchild_00 = taffy.newLeaf(style)
        val grandchild_01 = taffy.newLeaf(style)
        val child_00 = taffy.newLeafWithChildren(style, listOf(grandchild_00, grandchild_01))

        val grandchild_02 = taffy.newLeaf(style)
        val child_01 = taffy.newLeafWithChildren(style, listOf(grandchild_02))

        val root = taffy.newLeafWithChildren(
            Style(display = Display.NONE, size = Size.fromPoints(50f, 50f)),
            listOf(child_00, child_01)
        )

        Compute.performHiddenLayout(taffy, root)

        // Whatever size and display-mode the nodes had previously,
        // all layouts should resolve to ZERO due to the root's DISPLAY::NONE
        for (node in taffy.leafs.filter{ node -> node != root }) {
            val layout  = taffy.layout(node)
            Assertions.assertEquals(Size.zeroF(), layout.size)
            Assertions.assertEquals(Point.zeroF(), layout.location)
        }
    }
}
