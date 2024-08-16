package be.arby.taffy.tests.node

import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.geom.unwrapOr
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.Result
import be.arby.taffy.length
import be.arby.taffy.style.Display
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.flex.FlexDirection
import be.arby.taffy.tests.assert
import be.arby.taffy.tests.assertEq
import be.arby.taffy.tree.node.TaffyTree
import be.arby.taffy.vec
import org.junit.jupiter.api.Test

class Mod {
    val sizeMeasureFunction: (Size<Option<Float>>, Size<AvailableSpace>, Int, Option<Size<Float>>, Style) -> Size<Float> =
        { knownDimensions, _availableSpace, _nodeId, nodeContext, _style ->
            knownDimensions.unwrapOr(nodeContext.unwrapOr(Size.ZERO.clone()))
        }

    @Test
    fun test_newLeaf() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val res = taffy.newLeaf(Style.default())
        assert(res.isOk())
        val node = res.unwrap()

        // node should be in the taffy tree and have no children
        assert(taffy.childCount(node) == 0)
    }

    @Test
    fun newLeaf_with_context() {
        val taffy: TaffyTree<Size<Float>> = TaffyTree.new()

        val res = taffy.newLeafWithContext(Style.default(), Size.ZERO.clone())
        assert(res.isOk())
        val node = res.unwrap()

        // node should be in the taffy tree and have no children
        assert(taffy.childCount(node) == 0)
    }

    /// Test that new_with_children works as expected
    @Test
    fun test_new_with_children() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val node = taffy.newWithChildren(Style.default(), listOf(child0, child1)).unwrap()

        // node should have two children
        assertEq(taffy.childCount(node), 2)
        assertEq(taffy.children(node).unwrap()[0], child0)
        assertEq(taffy.children(node).unwrap()[1], child1)
    }

    @Test
    fun remove_node_should_remove() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val node = taffy.newLeaf(Style.default()).unwrap()

        val u = taffy.remove(node).unwrap()
    }

    @Test
    fun remove_node_should_detach_hierarchy() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        // Build a linear tree layout: <0> <- <1> <- <2>
        val node2 = taffy.newLeaf(Style.default()).unwrap()
        val node1 = taffy.newWithChildren(Style.default(), listOf(node2)).unwrap()
        val node0 = taffy.newWithChildren(Style.default(), listOf(node1)).unwrap()

        // Both node0 and node1 should have 1 child nodes
        // TODO - assertEq(taffy.children(node0).unwrap().asSlice(), listOf(node1))
        // TODO - assertEq(taffy.children(node1).unwrap().asSlice(), listOf(node2))

        // Disconnect the tree: <0> <2>
        val u = taffy.remove(node1).unwrap()

        // Both remaining nodes should have no child nodes
        assert(taffy.children(node0).unwrap().isEmpty())
        assert(taffy.children(node2).unwrap().isEmpty())
    }

    @Test
    fun remove_last_node() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val parent = taffy.newLeaf(Style.default()).unwrap()
        val child = taffy.newLeaf(Style.default()).unwrap()
        taffy.addChild(parent, child).unwrap()

        taffy.remove(child).unwrap()
        taffy.remove(parent).unwrap()
    }

    @Test
    fun set_measure() {
        val taffy: TaffyTree<Size<Float>> = TaffyTree.new()
        val node = taffy.newLeafWithContext(Style.default(), Size(width = 200f, height = 200f)).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, sizeMeasureFunction).unwrap()
        assertEq(taffy.layout(node).unwrap().size.width, 200f)

        taffy.setNodeContext(node, Option.Some(Size(width = 100f, height = 100f))).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, sizeMeasureFunction).unwrap()
        assertEq(taffy.layout(node).unwrap().size.width, 100f)
    }

    @Test
    fun set_measure_of_previously_unmeasured_node() {
        val taffy: TaffyTree<Size<Float>> = TaffyTree.new()
        val node = taffy.newLeaf(Style.default()).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, sizeMeasureFunction).unwrap()
        assertEq(taffy.layout(node).unwrap().size.width, 0f)

        taffy.setNodeContext(node, Option.Some(Size(width = 100f, height = 100f))).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, sizeMeasureFunction).unwrap()
        assertEq(taffy.layout(node).unwrap().size.width, 100f)
    }

    /// Test that adding `add_child()` works
    @Test
    fun add_child() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy.newLeaf(Style.default()).unwrap()
        assertEq(taffy.childCount(node), 0)

        val child0 = taffy.newLeaf(Style.default()).unwrap()
        taffy.addChild(node, child0).unwrap()
        assertEq(taffy.childCount(node), 1)

        val child1 = taffy.newLeaf(Style.default()).unwrap()
        taffy.addChild(node, child1).unwrap()
        assertEq(taffy.childCount(node), 2)
    }

    @Test
    fun insert_child_at_index() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val child2 = taffy.newLeaf(Style.default()).unwrap()

        val node = taffy.newLeaf(Style.default()).unwrap()
        assertEq(taffy.childCount(node), 0)

        taffy.insertChildAtIndex(node, 0, child0).unwrap()
        assertEq(taffy.childCount(node), 1)
        assertEq(taffy.children(node).unwrap()[0], child0)

        taffy.insertChildAtIndex(node, 0, child1).unwrap()
        assertEq(taffy.childCount(node), 2)
        assertEq(taffy.children(node).unwrap()[0], child1)
        assertEq(taffy.children(node).unwrap()[1], child0)

        taffy.insertChildAtIndex(node, 1, child2).unwrap()
        assertEq(taffy.childCount(node), 3)
        assertEq(taffy.children(node).unwrap()[0], child1)
        assertEq(taffy.children(node).unwrap()[1], child2)
        assertEq(taffy.children(node).unwrap()[2], child0)
    }

    @Test
    fun set_children() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val node = taffy.newWithChildren(Style.default(), listOf(child0, child1)).unwrap()

        assertEq(taffy.childCount(node), 2)
        assertEq(taffy.children(node).unwrap()[0], child0)
        assertEq(taffy.children(node).unwrap()[1], child1)

        val child2 = taffy.newLeaf(Style.default()).unwrap()
        val child3 = taffy.newLeaf(Style.default()).unwrap()
        taffy.setChildren(node, listOf(child2, child3)).unwrap()

        assertEq(taffy.childCount(node), 2)
        assertEq(taffy.children(node).unwrap()[0], child2)
        assertEq(taffy.children(node).unwrap()[1], child3)
    }

    /// Test that removing a child works
    @Test
    fun remove_child() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val node = taffy.newWithChildren(Style.default(), listOf(child0, child1)).unwrap()

        assertEq(taffy.childCount(node), 2)

        taffy.removeChild(node, child0).unwrap()
        assertEq(taffy.childCount(node), 1)
        assertEq(taffy.children(node).unwrap()[0], child1)

        taffy.removeChild(node, child1).unwrap()
        assertEq(taffy.childCount(node), 0)
    }

    @Test
    fun remove_child_at_index() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val node = taffy.newWithChildren(Style.default(), listOf(child0, child1)).unwrap()

        assertEq(taffy.childCount(node), 2)

        taffy.removeChildAtIndex(node, 0).unwrap()
        assertEq(taffy.childCount(node), 1)
        assertEq(taffy.children(node).unwrap()[0], child1)

        taffy.removeChildAtIndex(node, 0).unwrap()
        assertEq(taffy.childCount(node), 0)
    }

    // Related to: https://github.com/DioxusLabs/taffy/issues/510
    @Test
    fun remove_child_updates_parents() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val parent = taffy.newLeaf(Style.default()).unwrap()
        val child = taffy.newLeaf(Style.default()).unwrap()

        taffy.addChild(parent, child)

        taffy.remove(parent).unwrap()

        // Once the parent is removed this shouldn't panic.
        assert(taffy.setChildren(child, listOf()).isOk())
    }

    @Test
    fun replace_child_at_index() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()

        val node = taffy.newWithChildren(Style.default(), listOf(child0)).unwrap()
        assertEq(taffy.childCount(node), 1)
        assertEq(taffy.children(node).unwrap()[0], child0)

        taffy.replaceChildAtIndex(node, 0, child1).unwrap()
        assertEq(taffy.childCount(node), 1)
        assertEq(taffy.children(node).unwrap()[0], child1)
    }

    @Test
    fun test_child_at_index() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val child2 = taffy.newLeaf(Style.default()).unwrap()
        val node = taffy.newWithChildren(Style.default(), listOf(child0, child1, child2)).unwrap()

        assert(when (val v = taffy.childAtIndex(node, 0)) {
            is Result.Ok -> v.unwrap() == child0
            else -> false
        })
        assert(when (val v = taffy.childAtIndex(node, 1)) {
            is Result.Ok -> v.unwrap() == child1
            else -> false
        })
        assert(when (val v = taffy.childAtIndex(node, 2)) {
            is Result.Ok -> v.unwrap() == child2
            else -> false
        })
    }

    @Test
    fun test_child_count() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val node = taffy.newWithChildren(Style.default(), listOf(child0, child1)).unwrap()

        assert(taffy.childCount(node) == 2)
        assert(taffy.childCount(child0) == 0)
        assert(taffy.childCount(child1) == 0)
    }

    @Test
    fun test_children() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val node = taffy.newWithChildren(Style.default(), listOf(child0, child1)).unwrap()

        val children = vec<Int>()
        children.add(child0)
        children.add(child1)

        val children_result = taffy.children(node).unwrap()
        assertEq(children_result, children)

        assert(taffy.children(child0).unwrap().isEmpty())
    }

    @Test
    fun test_set_style() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val node = taffy.newLeaf(Style.default()).unwrap()
        assertEq(taffy.style(node).unwrap().display, Display.FLEX)

        taffy.setStyle(node, Style(display = Display.NONE)).unwrap()
        assertEq(taffy.style(node).unwrap().display, Display.NONE)
    }

    @Test
    fun test_style() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val style = Style(display = Display.NONE, flexDirection = FlexDirection.ROW_REVERSE)

        val node = taffy.newLeaf(style.clone()).unwrap()

        val res = taffy.style(node)
        assert(res.isOk())
        assert(res.unwrap() == style)
    }

    @Test
    fun test_layout() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy.newLeaf(Style.default()).unwrap()

        // TODO: Improve this test?
        val res = taffy.layout(node)
        assert(res.isOk())
    }

    @Test
    fun test_mark_dirty() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style.default()).unwrap()
        val child1 = taffy.newLeaf(Style.default()).unwrap()
        val node = taffy.newWithChildren(Style.default(), listOf(child0, child1)).unwrap()

        taffy.computeLayout(node, Size.MAX_CONTENT).unwrap()

        assertEq(taffy.dirty(child0), Result.Ok(false))
        assertEq(taffy.dirty(child1), Result.Ok(false))
        assertEq(taffy.dirty(node), Result.Ok(false))

        taffy.markDirty(node).unwrap()
        assertEq(taffy.dirty(child0), Result.Ok(false))
        assertEq(taffy.dirty(child1), Result.Ok(false))
        assertEq(taffy.dirty(node), Result.Ok(true))

        taffy.computeLayout(node, Size.MAX_CONTENT).unwrap()
        taffy.markDirty(child0).unwrap()
        assertEq(taffy.dirty(child0), Result.Ok(true))
        assertEq(taffy.dirty(child1), Result.Ok(false))
        assertEq(taffy.dirty(node), Result.Ok(true))
    }

    @Test
    fun compute_layout_should_produce_valid_result() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val nodeResult = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.Length(10f), height = Dimension.Length(10f))
            )
        )
        assert(nodeResult.isOk())
        val node = nodeResult.unwrap()
        val layoutResult = taffy.computeLayout(
            node,
            Size(width = AvailableSpace.Definite(100f), height = AvailableSpace.Definite(100f)),
        )
        assert(layoutResult.isOk())
    }

    @Test
    fun make_sure_layout_location_is_top_left() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val node = taffy.newLeaf(
            Style(size = Size(width = Dimension.Percent(1f), height = Dimension.Percent(1f)))
        ).unwrap()

        val root = taffy.newWithChildren(
            Style(
                size = Size(
                    width = Dimension.Length(100f), height = Dimension.Length(100f)
                ),
                padding = Rect(
                    left = length(10f),
                    right = length(20f),
                    top = length(30f),
                    bottom = length(40f),
                )
            ), listOf(node)
        ).unwrap()

        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()

        // If Layout.location represents top-left coord, 'node' location
        // must be (due applied 'root' padding): {x: 10, y: 30}.
        //
        // It's important, since result will be different for each other
        // coordinate space:
        // - bottom-left:  {x: 10, y: 40}
        // - top-right:    {x: 20, y: 30}
        // - bottom-right: {x: 20, y: 40}
        val layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 10f)
        assertEq(layout.location.y, 30f)
    }
}
