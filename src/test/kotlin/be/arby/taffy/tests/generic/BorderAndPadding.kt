package be.arby.taffy.tests.generic

import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.tests.assertEq
import be.arby.taffy.tree.node.TaffyTree
import org.junit.jupiter.api.Test

class BorderAndPadding {
    fun <T> arrToRect(items: Array<T>): Rect<T> {
        return Rect(left = items[0], right = items[1], top = items[2], bottom = items[3])
    }

    @Test
    fun border_on_a_single_axis_doesnt_increase_size() {
        for (i in 0 until 4) {
            val taffy: TaffyTree<Nothing> = TaffyTree.new()
            val node = taffy.newLeaf(
                Style(
                    border = run {
                        val lengths = Array(4) { LengthPercentage.ZERO }
                        lengths[i] = LengthPercentage.Length(10f)
                        arrToRect(lengths)
                    })
            ).unwrap()

            taffy.computeLayout(
                node,
                Size(width = AvailableSpace.Definite(100f), height = AvailableSpace.Definite(100f)),
            ).unwrap()

            val layout = taffy.layout(node).unwrap();
            assertEq(layout.size.width * layout.size.height, 0f)
        }
    }

    @Test
    fun padding_on_a_single_axis_doesnt_increase_size() {
        for (i in 0 until 4) {
            val taffy: TaffyTree<Nothing> = TaffyTree.new()
            val node = taffy.newLeaf(
                Style(
                    padding = run {
                        val lengths = Array(4) { LengthPercentage.ZERO }
                        lengths[i] = LengthPercentage.Length(10f)
                        arrToRect(lengths)
                    })
            ).unwrap()

            taffy.computeLayout(
                node,
                Size(width = AvailableSpace.Definite(100f), height = AvailableSpace.Definite(100f)),
            )
                .unwrap()

            val layout = taffy.layout(node).unwrap();
            assertEq(layout.size.width * layout.size.height, 0f)
        }
    }

    @Test
    fun border_and_padding_on_a_single_axis_doesnt_increase_size() {
        for (i in 0 until 4) {
            val taffy: TaffyTree<Nothing> = TaffyTree.new()
            val rect = {
                val lengths = Array(4) { LengthPercentage.ZERO }
                lengths[i] = LengthPercentage.Length(10f)
                arrToRect(lengths)
            }
            val node = taffy.newLeaf(Style(border = rect(), padding = rect())).unwrap()

            val layout = taffy.layout(node).unwrap()
            assertEq(layout.size.width * layout.size.height, 0f)
        }
    }

    @Test
    fun vertical_border_and_padding_percentage_values_use_available_space_correctly() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val node = taffy.newLeaf(Style(
            padding = Rect(
                left = LengthPercentage.Percent(1f),
                top = LengthPercentage.Percent(1f),
                right = LengthPercentage.ZERO,
                bottom = LengthPercentage.ZERO
        ))).unwrap()

        taffy.computeLayout(node, Size(width = AvailableSpace.Definite(200f), height = AvailableSpace.Definite(100f)))
            .unwrap()

        val layout = taffy.layout(node).unwrap()
        assertEq(layout.size.width, 200f)
        assertEq(layout.size.height, 200f)
    }
}
