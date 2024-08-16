package be.arby.taffy.tests.generic

import be.arby.taffy.auto
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.Position
import be.arby.taffy.style.Style
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.tests.assertEq
import be.arby.taffy.tree.node.TaffyTree
import org.junit.jupiter.api.Test

class Measure {
    data class FixedMeasure(
        val width: Float,
        val height: Float
    )

    val fixedMeasureFunction = { knownDimensions: Size<Option<Float>>, _availableSpace: Size<AvailableSpace>,
                                  _nodeId: Int, nodeContext: Option<FixedMeasure>, _style: Style ->

        val size = nodeContext.unwrapOr(FixedMeasure(width = 0f, height = 0f))

        Size(
            width = knownDimensions.width.unwrapOr(size.width),
            height = knownDimensions.height.unwrapOr(size.height)
        )
    }

    data class AspectRatioMeasure(
        val width: Float,
        val heightRatio: Float
    )

    val aspectRatioMeasureFunction = { knownDimensions: Size<Option<Float>>, _availableSpace: Size<AvailableSpace>,
                                       _nodeId: Int, nodeContext: Option<AspectRatioMeasure>, _style: Style ->
        if (nodeContext.isSome()) {
            val v = nodeContext.unwrap()
            val width = knownDimensions.width.unwrapOr(v.width)
            val height = knownDimensions.height.unwrapOr(width * v.heightRatio)

            Size(width, height)
        } else {
            Size.ZERO.clone()
        }
    }

    @Test
    fun measure_root() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val node = taffy.newLeafWithContext(Style.default(), FixedMeasure(width = 100f, height = 100f)).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(node).unwrap().size.width, 100f)
        assertEq(taffy.layout(node).unwrap().size.height, 100f)
    }

    @Test
    fun measure_child() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()

        val child = taffy.newLeafWithContext(Style.default(), FixedMeasure(width = 100f, height = 100f)).unwrap()

        val node = taffy.newWithChildren(Style.default(), listOf(child)).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(node).unwrap().size.width, 100f)
        assertEq(taffy.layout(node).unwrap().size.height, 100f)

        assertEq(taffy.layout(child).unwrap().size.width, 100f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)
    }

    @Test
    fun measure_child_constraint() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child =
        taffy.newLeafWithContext(Style.default(), FixedMeasure(width = 100f, height = 100f)).unwrap()

        val node = taffy
                .newWithChildren(
            Style(size = Size(width = Dimension.Length(50f), height = auto())),
            listOf(child),
        )
        .unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        // Parent
        assertEq(taffy.layout(node).unwrap().size.width, 50f)
        assertEq(taffy.layout(node).unwrap().size.height, 100f)
        // Child
        assertEq(taffy.layout(child).unwrap().size.width, 100f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)
    }

    @Test
    fun measure_child_constraint_padding_parent() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child =
        taffy.newLeafWithContext(Style.default(), FixedMeasure(width = 100f, height = 100f)).unwrap()

        val node = taffy.newWithChildren(
            Style (
                size = Size(width = Dimension.Length(50f), height = auto()),
                padding = Rect(
                    left = LengthPercentage.Length(10f),
                    right = LengthPercentage.Length(10f),
                    top = LengthPercentage.Length(10f),
                    bottom = LengthPercentage.Length(10f),
                )
            ),
            listOf(child)
        ).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(node).unwrap().location.x, 0f)
        assertEq(taffy.layout(node).unwrap().location.y, 0f)
        assertEq(taffy.layout(node).unwrap().size.width, 50f)
        assertEq(taffy.layout(node).unwrap().size.height, 120f)

        assertEq(taffy.layout(child).unwrap().location.x, 10f)
        assertEq(taffy.layout(child).unwrap().location.y, 10f)
        assertEq(taffy.layout(child).unwrap().size.width, 100f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)
    }

    @Test
    fun measure_child_with_flex_grow() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style(
            size = Size(width = Dimension.Length(50f), height = Dimension.Length(50f))
        )).unwrap()

        val child1 = taffy.newLeafWithContext(
            Style( flexGrow = 1f),
            FixedMeasure(width = 10f, height = 50f)
        ).unwrap()

        val node = taffy.newWithChildren(
            Style(size = Size(width = Dimension.Length(100f), height = auto())),
            listOf(child0, child1)
        )
        .unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(child1).unwrap().size.width, 50f)
        assertEq(taffy.layout(child1).unwrap().size.height, 50f)
    }

    @Test
    fun measure_child_with_flex_shrink() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style(
            size = Size(
                width = Dimension.Length(50f),
                height = Dimension.Length(50f)
            ),
            flexShrink = 0f
        )).unwrap()

        val child1 =
        taffy.newLeafWithContext(Style.default(), FixedMeasure(width = 100f, height = 50f)).unwrap()

        val node = taffy.newWithChildren(
            Style(size = Size(width = Dimension.Length(100f), height = auto())),
            listOf(child0, child1)
        )
        .unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(child1).unwrap().size.width, 100f)
        assertEq(taffy.layout(child1).unwrap().size.height, 50f)
    }

    @Test
    fun remeasure_child_after_growing() {
        val taffy: TaffyTree<AspectRatioMeasure> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style(
            size = Size(width = Dimension.Length(50f), height = Dimension.Length(50f))
        )).unwrap()

        val child1 = taffy.newLeafWithContext(
            Style(flexGrow = 1f),
            AspectRatioMeasure(width = 10f, heightRatio = 2f)
        ).unwrap()

        val node = taffy.newWithChildren(
            Style(
                size = Size(width = Dimension.Length(100f), height = auto()),
                alignItems = Option.Some(AlignItems.START)
            ),
            listOf(child0, child1)
        ).unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, aspectRatioMeasureFunction).unwrap()

        assertEq(taffy.layout(child1).unwrap().size.width, 50f)
        assertEq(taffy.layout(child1).unwrap().size.height, 100f)
    }

    @Test
    fun remeasure_child_after_shrinking() {
        val taffy: TaffyTree<AspectRatioMeasure> = TaffyTree.new()

        val child0 = taffy.newLeaf(Style(
            size = Size(width = Dimension.Length(50f), height = Dimension.Length(50f)),
            flexShrink = 0f
        )).unwrap()

        val child1 = taffy.newLeafWithContext(
            Style.default(),
            AspectRatioMeasure(width = 100f, heightRatio = 2f)
        ).unwrap()

        val node = taffy.newWithChildren(
            Style(
                size = Size(width = Dimension.Length(100f), height = auto()),
                alignItems = Option.Some(AlignItems.START)
            ),
            listOf(child0, child1)
        )
        .unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, aspectRatioMeasureFunction).unwrap()

        assertEq(taffy.layout(child1).unwrap().size.width, 100f)
        assertEq(taffy.layout(child1).unwrap().size.height, 200f)
    }

    val customMeasureFunction = { knownDimensions: Size<Option<Float>>, _availableSpace: Size<AvailableSpace>,
                                 _nodeId: Int, nodeContext: Option<() -> Unit>, _style: Style ->

        val height = knownDimensions.height.unwrapOr(50f)
        val width = knownDimensions.width.unwrapOr(height)
        Size(width, height)
    }

    @Test
    fun remeasure_child_after_stretching() {
        val taffy: TaffyTree<() -> Unit> = TaffyTree.new()

        val child = taffy.newLeafWithContext(Style.default()) {}.unwrap()

        val node = taffy.newWithChildren(
            Style(
                size = Size(width = Dimension.Length(100f), height = Dimension.Length(100f))
            ),
            listOf(child)
        )
        .unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, customMeasureFunction).unwrap()

        assertEq(taffy.layout(child).unwrap().size.width, 100f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)
    }

    @Test
    fun width_overrides_measure() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child = taffy.newLeafWithContext(
            Style(size = Size(width = Dimension.Length(50f), height = auto())),
            FixedMeasure(width = 100f, height = 100f),
        ).unwrap()

        val node = taffy.newWithChildren(Style.default(), listOf(child)).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(child).unwrap().size.width, 50f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)
    }

    @Test
    fun height_overrides_measure() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child = taffy.newLeafWithContext(
            Style(size = Size(width = auto(), height = Dimension.Length(50f))),
            FixedMeasure(width = 100f, height = 100f),
        ).unwrap()

        val node = taffy.newWithChildren(Style.default(), listOf(child)).unwrap()
        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(child).unwrap().size.width, 100f)
        assertEq(taffy.layout(child).unwrap().size.height, 50f)
    }

    @Test
    fun flex_basis_overrides_measure() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child0 = taffy.newLeaf(Style(flexBasis = Dimension.Length(50f), flexGrow = 1f)).unwrap()

        val child1 = taffy.newLeafWithContext(
            Style(flexBasis = Dimension.Length(50f), flexGrow = 1f),
            FixedMeasure(width = 100f, height = 100f),
        ).unwrap()

        val node = taffy.newWithChildren(
            Style(
                size = Size(width = Dimension.Length(200f), height = Dimension.Length(100f))
            ),
            listOf(child0, child1)
        )
        .unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(child0).unwrap().size.width, 100f)
        assertEq(taffy.layout(child0).unwrap().size.height, 100f)
        assertEq(taffy.layout(child1).unwrap().size.width, 100f)
        assertEq(taffy.layout(child1).unwrap().size.height, 100f)
    }

    @Test
    fun stretch_overrides_measure() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child = taffy.newLeafWithContext(Style.default(), FixedMeasure(width = 50f, height = 50f)).unwrap()

        val node = taffy.newWithChildren(
            Style(
                size = Size(width = Dimension.Length(100f), height = Dimension.Length(100f))
            ),
            listOf(child)
        )
        .unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(child).unwrap().size.width, 50f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)
    }

    @Test
    fun measure_absolute_child() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child = taffy.newLeafWithContext(
            Style(position = Position.ABSOLUTE),
            FixedMeasure(width = 50f, height = 50f)
        ).unwrap()

        val node = taffy.newWithChildren(
            Style(
                size = Size(width = Dimension.Length(100f), height = Dimension.Length(100f))
            ),
            listOf(child),
        ).unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(child).unwrap().size.width, 50f)
        assertEq(taffy.layout(child).unwrap().size.height, 50f)
    }

    @Test
    fun ignore_invalid_measure() {
        val taffy: TaffyTree<FixedMeasure> = TaffyTree.new()
        val child = taffy.newLeaf(Style(flexGrow = 1f)).unwrap()

        val node = taffy.newWithChildren(
            Style(
                size = Size(width = Dimension.Length(100f), height = Dimension.Length(100f))
            ),
            listOf(child)
        )
        .unwrap()

        taffy.computeLayoutWithMeasure(node, Size.MAX_CONTENT, fixedMeasureFunction).unwrap()

        assertEq(taffy.layout(child).unwrap().size.width, 100f)
        assertEq(taffy.layout(child).unwrap().size.height, 100f)
    }
}
