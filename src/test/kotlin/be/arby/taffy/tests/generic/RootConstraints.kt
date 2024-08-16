package be.arby.taffy.tests.generic

import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.length
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.tests.assertEq
import be.arby.taffy.tree.node.TaffyTree
import org.junit.jupiter.api.Test

class RootConstraints {
    @Test
    fun root_with_percentage_size() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy
            .newLeaf(
                Style(
                    size = Size(
                        width = Dimension.Percent(1f),
                        height = Dimension.Percent(1f),
                    )
                )
            )
            .unwrap()

        taffy
            .computeLayout(
                node,
                Size(
                    width = AvailableSpace.Definite(100f),
                    height = AvailableSpace.Definite(200f),
                ),
            )
            .unwrap()
        val layout = taffy.layout(node).unwrap()

        assertEq(layout.size.width, 100f)
        assertEq(layout.size.height, 200f)
    }

    @Test
    fun root_with_no_size() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy.newLeaf(Style.default()).unwrap()

        taffy
            .computeLayout(
                node,
                Size(
                    width = AvailableSpace.Definite(100f),
                    height = AvailableSpace.Definite(100f),
                ),
            )
            .unwrap()
        val layout = taffy.layout(node).unwrap()

        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)
    }

    @Test
    fun root_with_larger_size() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy
            .newLeaf(
                Style(
                    size = Size(
                        width = Dimension.Length(200f),
                        height = Dimension.Length(200f),
                    )
                )
            )
            .unwrap()

        taffy
            .computeLayout(
                node,
                Size(
                    width = AvailableSpace.Definite(100f),
                    height = AvailableSpace.Definite(100f),
                ),
            )
            .unwrap()
        val layout = taffy.layout(node).unwrap()

        assertEq(layout.size.width, 200f)
        assertEq(layout.size.height, 200f)
    }

    @Test
    fun root_padding_and_border_larger_than_definite_size() {
        val tree: TaffyTree<Nothing> = TaffyTree.new()

        val child = tree.newLeaf(Style.default()).unwrap()

        val root = tree
            .newWithChildren(
                Style(
                    size = Size(width = length(10f), height = length(10f)),
                    padding = Rect(left = length(10f), right = length(10f), top = length(10f), bottom = length(10f)),

                    border = Rect(left = length(10f), right = length(10f), top = length(10f), bottom = length(10f))
                ),
                listOf(child),
            )
            .unwrap()

        tree.computeLayout(root, Size.MAX_CONTENT).unwrap()

        val layout = tree.layout(root).unwrap()

        assertEq(layout.size.width, 40f)
        assertEq(layout.size.height, 40f)
    }
}
