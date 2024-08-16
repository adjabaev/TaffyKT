package be.arby.taffy.tests.generic

import be.arby.taffy.auto
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.length
import be.arby.taffy.percent
import be.arby.taffy.style.Display
import be.arby.taffy.style.Style
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.alignment.JustifyContent
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.tests.assertEq
import be.arby.taffy.tree.node.TaffyTree
import org.junit.jupiter.api.Test

class Relayout {
    @Test
    fun relayout() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node1 = taffy.newLeaf(
            Style(
                size = Size(width = length(8f), height = length(80f))
            )
        ).unwrap()
        val node0 = taffy.newWithChildren(
            Style(
                alignSelf = Option.Some(AlignSelf.CENTER),
                size = Size(width = Dimension.Auto, height = Dimension.Auto),
                // size: Size { width: Dimension.Percent(1f), height: Dimension.Percent(1f) },
            ),
            listOf(node1),
        )
            .unwrap()
        val node = taffy
            .newWithChildren(
                Style(
                    size = Size(width = Dimension.Percent(1f), height = Dimension.Percent(1f))
                ),
                listOf(node0),
            )
            .unwrap()
        taffy
            .computeLayout(
                node,
                Size(width = AvailableSpace.Definite(100f), height = AvailableSpace.Definite(100f)),
            )
            .unwrap()
        val initial = taffy.layout(node).unwrap().location
        val initial0 = taffy.layout(node0).unwrap().location
        val initial1 = taffy.layout(node1).unwrap().location
        for (u in 1 until 10) {
            taffy
                .computeLayout(
                    node,
                    Size(
                        width = AvailableSpace.Definite(100f),
                        height = AvailableSpace.Definite(100f),
                    ),
                )
                .unwrap()
            assertEq(taffy.layout(node).unwrap().location, initial)
            assertEq(taffy.layout(node0).unwrap().location, initial0)
            assertEq(taffy.layout(node1).unwrap().location, initial1)
        }
    }

    @Test
    fun toggle_root_display_none() {
        val hiddenStyle = Style(
            display = Display.NONE,
            size = Size(width = length(100f), height = length(100f))
        )

        val flexStyle = Style(
            display = Display.FLEX,
            size = Size(width = length(100f), height = length(100f))
        )

        // Setup
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy.newLeaf(hiddenStyle.clone()).unwrap()

        // Layout 1 (None)
        taffy.computeLayout(node, Size.MAX_CONTENT).unwrap()
        var layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)

        // Layout 2 (Flex)
        taffy.setStyle(node, flexStyle).unwrap()
        taffy.computeLayout(node, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 100f)
        assertEq(layout.size.height, 100f)

        // Layout 3 (None)
        taffy.setStyle(node, hiddenStyle).unwrap()
        taffy.computeLayout(node, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)
    }

    @Test
    fun toggle_root_display_none_with_children() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()

        val child = taffy
            .newLeaf(
                Style(
                    size = Size(width = length(800f), height = length(100f))
                )
            )
            .unwrap()

        val parent = taffy
            .newWithChildren(
                Style(size = Size(width = length(800f), height = length(100f))),
                listOf(child),
            )
            .unwrap()

        val root = taffy.newWithChildren(Style.default(), listOf(parent)).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        assertEq(taffy.layout(child).unwrap().size.width, 800f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)

        taffy.setStyle(root, Style(display = Display.NONE)).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        assertEq(taffy.layout(child).unwrap().size.width, 0f)
        assertEq(taffy.layout(child).unwrap().size.height, 0f)

        taffy.setStyle(root, Style.default()).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        assertEq(taffy.layout(parent).unwrap().size.width, 800f)
        assertEq(taffy.layout(parent).unwrap().size.height, 100f)
        assertEq(taffy.layout(child).unwrap().size.width, 800f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)
    }

    @Test
    fun toggle_flex_child_display_none() {
        val hiddenStyle = Style(
            display = Display.NONE,
            size = Size(width = length(100f), height = length(100f))
        )

        val flexStyle = Style(
            display = Display.FLEX,
            size = Size(width = length(100f), height = length(100f))
        )

        // Setup
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy.newLeaf(hiddenStyle.clone()).unwrap()
        val root = taffy.newWithChildren(flexStyle.clone(), listOf(node)).unwrap()

        // Layout 1 (None)
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        var layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)

        // Layout 2 (Flex)
        taffy.setStyle(node, flexStyle).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 100f)
        assertEq(layout.size.height, 100f)

        // Layout 3 (None)
        taffy.setStyle(node, hiddenStyle).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)
    }

    @Test
    fun toggle_flex_container_display_none() {
        val hiddenStyle = Style(
            display = Display.NONE,
            size = Size(width = length(100f), height = length(100f))
        )

        val flexStyle = Style(
            display = Display.FLEX,
            size = Size(width = length(100f), height = length(100f))
        )

        // Setup
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy.newLeaf(hiddenStyle.clone()).unwrap()
        val root = taffy.newWithChildren(hiddenStyle.clone(), listOf(node)).unwrap()

        // Layout 1 (None)
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        var layout = taffy.layout(root).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)

        // Layout 2 (Flex)
        taffy.setStyle(root, flexStyle).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(root).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 100f)
        assertEq(layout.size.height, 100f)

        // Layout 3 (None)
        taffy.setStyle(root, hiddenStyle).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(root).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)
    }

    @Test
    fun toggle_grid_child_display_none() {
        val hiddenStyle = Style(
            display = Display.NONE,
            size = Size(width = length(100f), height = length(100f))
        )

        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = length(100f), height = length(100f))
        )

        // Setup
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy.newLeaf(hiddenStyle.clone()).unwrap()
        val root = taffy.newWithChildren(gridStyle.clone(), listOf(node)).unwrap()

        // Layout 1 (None)
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        var layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)

        // Layout 2 (Flex)
        taffy.setStyle(node, gridStyle).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 100f)
        assertEq(layout.size.height, 100f)

        // Layout 3 (None)
        taffy.setStyle(node, hiddenStyle).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(node).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)
    }

    @Test
    fun toggle_grid_container_display_none() {
        val hidden_style = Style(
            display = Display.NONE,
            size = Size(width = length(100f), height = length(100f))
        )

        val gridStyle = Style(
            display = Display.GRID,
            size = Size(width = length(100f), height = length(100f))
        )

        // Setup
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        val node = taffy.newLeaf(hidden_style.clone()).unwrap()
        val root = taffy.newWithChildren(hidden_style.clone(), listOf(node)).unwrap()

        // Layout 1 (None)
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        var layout = taffy.layout(root).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)

        // Layout 2 (Flex)
        taffy.setStyle(root, gridStyle).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(root).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 100f)
        assertEq(layout.size.height, 100f)

        // Layout 3 (None)
        taffy.setStyle(root, hidden_style).unwrap()
        taffy.computeLayout(root, Size.MAX_CONTENT).unwrap()
        layout = taffy.layout(root).unwrap()
        assertEq(layout.location.x, 0f)
        assertEq(layout.location.y, 0f)
        assertEq(layout.size.width, 0f)
        assertEq(layout.size.height, 0f)
    }

    @Test
    fun relayout_is_stable_with_rounding() {
        val taffy: TaffyTree<Nothing> = TaffyTree.new()
        taffy.enableRounding()

        // <div style="width: 1920px; height: 1080px">
        //     <div style="width: 100%; left: 1.5px">
        //         <div style="width: 150px; justify-content: end">
        //             <div style="min-width: 300px" />
        //         </div>
        //     </div>
        // </div>

        val innerr = taffy.newLeaf(Style(minSize = Size(width = length(300f), height = auto()))).unwrap()
        val wrapper = taffy.newWithChildren(
                Style(
                    size = Size(width = length(150f), height = auto()),
                    justifyContent = Option.Some(JustifyContent.END)
                ),
                listOf(innerr),
            ).unwrap()
        val outer = taffy.newWithChildren(
                Style(
                    size = Size(width = percent(1f), height = auto()),
                    inset = Rect(left = length(1.5f), right = auto(), top = auto(), bottom = auto())
                ),
                listOf(wrapper),
            ).unwrap()
        val root = taffy.newWithChildren(
                Style(size = Size(width = length(1920f), height = length(1080f))),
                listOf(outer)
            ).unwrap()
        for (u in 0 until 5) {
            taffy.markDirty(root)
            taffy.computeLayout(root, Size.MAX_CONTENT)
            taffy.printTree(root)

            val rootLayout = taffy.layout(root).unwrap()
            assertEq(rootLayout.location.x, 0f)
            assertEq(rootLayout.location.y, 0f)
            assertEq(rootLayout.size.width, 1920f)
            assertEq(rootLayout.size.height, 1080f)

            val outerLayout = taffy.layout(outer).unwrap()
            assertEq(outerLayout.location.x, 2f)
            assertEq(outerLayout.location.y, 0f)
            assertEq(outerLayout.size.width, 1920f)
            assertEq(outerLayout.size.height, 1080f)

            val wrapperLayout = taffy.layout(wrapper).unwrap()
            assertEq(wrapperLayout.location.x, 0f)
            assertEq(wrapperLayout.location.y, 0f)
            assertEq(wrapperLayout.size.width, 150f)
            assertEq(wrapperLayout.size.height, 1080f)

            val innerLayout = taffy.layout(innerr).unwrap()
            assertEq(innerLayout.location.x, -150f)
            assertEq(innerLayout.location.y, 0f)
            assertEq(innerLayout.size.width, 301f)
            assertEq(innerLayout.size.height, 1080f)
        }
    }
}
