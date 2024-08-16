package be.arby.taffy.tests.generic

import be.arby.taffy.geom.Size
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.tests.assertEq
import be.arby.taffy.tree.node.TaffyTree
import org.junit.jupiter.api.Test

class MinMaxOverrides {
    @Test
    fun min_overrides_max() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val child = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.Length(50f), height = Dimension.Length(50f)),
                minSize = Size(width = Dimension.Length(100f), height = Dimension.Length(100f)),
                maxSize = Size(width = Dimension.Length(10f), height = Dimension.Length(10f))
            )
        ).unwrap()

        taffy.computeLayout(
            child,
            Size(width = AvailableSpace.Definite(100f), height = AvailableSpace.Definite(100f)),
        ).unwrap()

        assertEq(taffy.layout(child).unwrap().size, Size(width = 100f, height = 100f))
    }

    @Test
    fun max_overrides_size() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val child = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.Length(50f), height = Dimension.Length(50f)),
                maxSize = Size(width = Dimension.Length(10f), height = Dimension.Length(10f))
            )
        ).unwrap()

        taffy.computeLayout(
            child,
            Size(width = AvailableSpace.Definite(100f), height = AvailableSpace.Definite(100f)),
        ).unwrap()

        assertEq(taffy.layout(child).unwrap().size, Size(width = 10f, height = 10f))
    }

    @Test
    fun min_overrides_size() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val child = taffy.newLeaf(
            Style(
                size = Size(width = Dimension.Length(50f), height = Dimension.Length(50f)),
                minSize = Size(width = Dimension.Length(100f), height = Dimension.Length(100f))
            )
        ).unwrap()

        taffy.computeLayout(
            child,
            Size(width = AvailableSpace.Definite(100f), height = AvailableSpace.Definite(100f)),
        ).unwrap()

        assertEq(taffy.layout(child).unwrap().size, Size(width = 100f, height = 100f))
    }
}
