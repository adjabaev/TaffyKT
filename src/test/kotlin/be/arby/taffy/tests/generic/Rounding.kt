package be.arby.taffy.tests.generic

import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.length
import be.arby.taffy.style.Style
import be.arby.taffy.style.alignment.JustifyContent
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.tests.assertEq
import be.arby.taffy.tree.node.TaffyTree
import org.junit.jupiter.api.Test

class Rounding {
    @Test
    fun rounding_doesnt_leave_gaps() {
        // First create an instance of TaffyTree
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val wSquare = Size<Dimension>(width = length(100.3f), height = length(100.3f))
        val childA = taffy.newLeaf(Style(size = wSquare)).unwrap()
        val childB = taffy.newLeaf(Style(size = wSquare)).unwrap()

        val rootNode = taffy
            .newWithChildren(
                Style(
                    size = Size(width = length(963.3333f), height = length(1000f)),
                    justifyContent = Option.Some(JustifyContent.CENTER)
                ),
                listOf(childA, childB),
            )
            .unwrap()

        taffy.computeLayout(rootNode, Size.MAX_CONTENT).unwrap()
        taffy.printTree(rootNode)

        val layoutA = taffy.layout(childA).unwrap()
        val layoutB = taffy.layout(childB).unwrap()
        assertEq(layoutA.location.x + layoutA.size.width, layoutB.location.x)
    }
}
