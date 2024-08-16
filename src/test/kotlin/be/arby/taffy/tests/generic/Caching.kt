package be.arby.taffy.tests.generic

import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.Display
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tests.assertEq
import be.arby.taffy.tree.node.TaffyTree
import org.junit.jupiter.api.Test

class Caching {
    data class CountMeasure(var count: Int) {
        operator fun plusAssign(rhs: Int) {
            count += rhs
        }
    }

    val countMeasureFunction = { knownDimensions: Size<Option<Float>>, _availableSpace: Size<AvailableSpace>,
        _nodeId: Int, nodeContext: Option<CountMeasure>, _style: Style ->

        nodeContext.unwrap().count += 1
        Size(width = knownDimensions.width.unwrapOr(50f), height = knownDimensions.height.unwrapOr(50f))
    }

    @Test
    fun measure_count_flexbox() {
        val taffy: TaffyTree<CountMeasure> = TaffyTree.new()

        val leaf = taffy.newLeafWithContext(Style.default(), CountMeasure(0)).unwrap()

        var node = taffy.newWithChildren(Style.DEFAULT, listOf(leaf)).unwrap()
        for (u in 0 until 100) {
            node = taffy.newWithChildren(Style.DEFAULT, listOf(node)).unwrap()
        }

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT.clone(), countMeasureFunction).unwrap()

        assertEq(taffy.getNodeContext(leaf).unwrap().count, 4)
    }

    @Test
    fun measure_count_grid() {
        val taffy: TaffyTree<CountMeasure> = TaffyTree.new()

        val style = { Style(display = Display.GRID) }
        val leaf = taffy.newLeafWithContext(style(), CountMeasure(0)).unwrap()

        var node = taffy.newWithChildren(Style.DEFAULT, listOf(leaf)).unwrap()
        for (u in 0 until 100) {
            node = taffy.newWithChildren(Style.DEFAULT, listOf(node)).unwrap()
        }

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT.clone(), countMeasureFunction).unwrap()
        assertEq(taffy.getNodeContext(leaf).unwrap().count, 4)
    }
}
