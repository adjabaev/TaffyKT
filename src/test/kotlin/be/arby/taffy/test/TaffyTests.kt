package be.arby.taffy.test

import be.arby.taffy.Taffy
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.node.Node
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.AvailableSpace
import net.asterium.taffy.style.dimension.Dimension
import net.asterium.taffy.style.Display
import net.asterium.taffy.style.flex.FlexDirection
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TaffyTests {

    @Test
    fun `Test new leaf`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(Style())

        Assertions.assertTrue(taffy.leafs.size == 1)
        // node should be in the taffy tree and have no children
        Assertions.assertTrue(taffy.childCount(node) == 0)
    }

    @Test
    fun `Test new leaf with measure`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeafWithMeasure(Style()) { _, _ -> Size.zeroF() }

        Assertions.assertTrue(taffy.leafs.size == 1)
        // node should be in the taffy tree and have no children
        Assertions.assertTrue(taffy.childCount(node) == 0)
    }

    @Test
    fun `Test new leaf with children`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0, child1))

        Assertions.assertTrue(taffy.leafs.size == 3)
        Assertions.assertEquals(2, taffy.childCount(node))
        Assertions.assertEquals(child0, taffy.child(node, 0))
        Assertions.assertEquals(child1, taffy.child(node, 1))
    }

    @Test
    fun `Remove node should remove`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(Style())

        Assertions.assertTrue(taffy.leafs.size == 1)

        taffy.remove(node)

        Assertions.assertTrue(taffy.leafs.size == 0)
    }

    @Test
    fun `Remove node should detach hierarchy`() {
        val taffy = be.arby.taffy.Taffy()

        // Build a linear tree layout: <0> <- <1> <- <2>
        val node2 = taffy.newLeaf(Style())
        val node1 = taffy.newLeafWithChildren(Style(), listOf(node2))
        val node0 = taffy.newLeafWithChildren(Style(), listOf(node1))

        // Both node0 and node1 should have 1 child nodes
        Assertions.assertEquals(node1, taffy.children(node0)[0])
        Assertions.assertEquals(node2, taffy.children(node1)[0])

        // Disconnect the tree: <0> <2>
        taffy.remove(node1)

        // Both remaining nodes should have no child nodes
        Assertions.assertTrue(taffy.children(node0).isEmpty())
        Assertions.assertTrue(taffy.children(node2).isEmpty())
    }

    @Test
    fun `Remove last node`() {
        val taffy = be.arby.taffy.Taffy()

        val parent = taffy.newLeaf(Style())
        val child = taffy.newLeaf(Style())

        Assertions.assertTrue(taffy.leafs.size == 2)
        Assertions.assertTrue(taffy.childCount(parent) == 0)
        Assertions.assertTrue(taffy.childCount(child) == 0)

        taffy.addChild(parent, child)

        Assertions.assertTrue(taffy.leafs.size == 2)
        Assertions.assertTrue(taffy.childCount(parent) == 1)
        Assertions.assertTrue(taffy.childCount(child) == 0)

        taffy.remove(child)
        taffy.remove(parent)

        Assertions.assertTrue(taffy.leafs.size == 0)
    }

    @Test
    fun `Set measure`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeafWithMeasure(Style()) { _, _ -> Size(200f, 200f) }
        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(200f, taffy.layout(node).size.width)

        taffy.setMeasure(node) { _, _ -> Size(100f, 100f) }
        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100f, taffy.layout(node).size.width)
    }

    @Test
    fun `Set measure of previously unmeasured node`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(Style())
        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(0f, taffy.layout(node).size.width)

        taffy.setMeasure(node) { _, _ -> Size(100f, 100f) }
        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100f, taffy.layout(node).size.width)
    }

    @Test
    fun `Add child`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(Style())
        Assertions.assertEquals(0, taffy.childCount(node))

        val child0 = taffy.newLeaf(Style())
        taffy.addChild(node, child0)
        Assertions.assertEquals(1, taffy.childCount(node))

        val child1 = taffy.newLeaf(Style())
        taffy.addChild(node, child1)
        Assertions.assertEquals(2, taffy.childCount(node))
    }

    @Test
    fun `Set children`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0, child1))
        Assertions.assertEquals(2, taffy.childCount(node))
        Assertions.assertEquals(child0, taffy.children(node)[0])
        Assertions.assertEquals(child1, taffy.children(node)[1])

        val child2 = taffy.newLeaf(Style())
        val child3 = taffy.newLeaf(Style())
        taffy.setChildren(node, listOf(child2, child3))

        Assertions.assertEquals(2, taffy.childCount(node))
        Assertions.assertEquals(child2, taffy.children(node)[0])
        Assertions.assertEquals(child3, taffy.children(node)[1])
    }

    @Test
    fun `Remove child`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0, child1))
        Assertions.assertEquals(2, taffy.childCount(node))

        taffy.removeChild(node, child0)
        Assertions.assertEquals(1, taffy.childCount(node))
        Assertions.assertEquals(child1, taffy.children(node)[0])

        taffy.removeChild(node, child1)
        Assertions.assertEquals(0, taffy.childCount(node))
    }

    @Test
    fun `Remove child at index`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0, child1))

        Assertions.assertEquals(2, taffy.childCount(node))

        taffy.removeChildAtIndex(node, 0)
        Assertions.assertEquals(1, taffy.childCount(node))
        Assertions.assertEquals(child1, taffy.children(node)[0])

        taffy.removeChildAtIndex(node, 0)
        Assertions.assertEquals(0, taffy.childCount(node))
    }

    @Test
    fun `Replace child at index`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0))

        Assertions.assertEquals(1, taffy.childCount(node))
        Assertions.assertEquals(child0, taffy.children(node)[0])

        taffy.replaceChildAtIndex(node, 0, child1)
        Assertions.assertEquals(1, taffy.childCount(node))
        Assertions.assertEquals(child1, taffy.children(node)[0])
    }

    @Test
    fun `Test child at index`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val child2 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0, child1, child2))

        Assertions.assertEquals(child0, taffy.childAtIndex(node, 0))
        Assertions.assertEquals(child1, taffy.childAtIndex(node, 1))
        Assertions.assertEquals(child2, taffy.childAtIndex(node, 2))
    }

    @Test
    fun `Test child count`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0, child1))

        Assertions.assertEquals(2, taffy.childCount(node))
        Assertions.assertEquals(0, taffy.childCount(child0))
        Assertions.assertEquals(0, taffy.childCount(child1))
    }

    @Test
    fun `Test children`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0, child1))

        val children = ArrayList<Node>()
        children.add(child0)
        children.add(child1)

        Assertions.assertEquals(children, taffy.children(node))
        Assertions.assertTrue(taffy.children(child0).isEmpty())
    }

    @Test
    fun `Test set style`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(Style())
        Assertions.assertEquals(Display.FLEX, taffy.style(node).display)

        taffy.setStyle(node, Style(display = Display.NONE))
        Assertions.assertEquals(Display.NONE, taffy.style(node).display)
    }

    @Test
    fun `Test style`() {
        val taffy = be.arby.taffy.Taffy()

        val style = Style(display = Display.NONE, flexDirection = FlexDirection.ROW_REVERSE)

        val node = taffy.newLeaf(style)
        Assertions.assertSame(style, taffy.style(node))
    }

    @Test
    fun `Test layout`() {
        val taffy = be.arby.taffy.Taffy()

        // TODO: Improve this test?
        taffy.newLeaf(Style())
    }

    @Test
    fun `Test mark dirty`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style())
        val child1 = taffy.newLeaf(Style())
        val node = taffy.newLeafWithChildren(Style(), listOf(child0, child1))
        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(false, taffy.dirty(child0))
        Assertions.assertEquals(false, taffy.dirty(child1))
        Assertions.assertEquals(false, taffy.dirty(node))

        taffy.markDirty(node)

        Assertions.assertEquals(false, taffy.dirty(child0))
        Assertions.assertEquals(false, taffy.dirty(child1))
        Assertions.assertEquals(true, taffy.dirty(node))

        taffy.computeLayout(node, Size.MAX_CONTENT)
        taffy.markDirty(child0)

        Assertions.assertEquals(true, taffy.dirty(child0))
        Assertions.assertEquals(false, taffy.dirty(child1))
        Assertions.assertEquals(true, taffy.dirty(node))
    }

    @Test
    fun `Compute layout should produce valid result`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeaf(Style(size = Size(width = Dimension.Length(10f), height = Dimension.Length(10f))))
        taffy.computeLayout(node, Size(width = AvailableSpace.fromPoints(100f), height = AvailableSpace.fromPoints(100f)))
    }
}
