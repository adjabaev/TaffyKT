package be.arby.taffy.test

import be.arby.taffy.Taffy
import net.asterium.taffy.geometry.Rect
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.lang.Option
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.Dimension
import net.asterium.taffy.style.dimension.LengthPercentage
import net.asterium.taffy.style.alignment.AlignItems
import net.asterium.taffy.style.Position
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class MeasureTest {

    @Test
    fun `Measure root`() {
        val taffy = be.arby.taffy.Taffy()

        val node = taffy.newLeafWithMeasure(Style()) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(100f),
                height = knownDimensions.height.unwrapOr(100f)
            )
        }

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100.0f, taffy.layout(node).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(node).size.height)
    }

    @Test
    fun `Measure child`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeafWithMeasure(Style()) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(100f),
                height = knownDimensions.height.unwrapOr(100f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100.0f, taffy.layout(node).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(node).size.height)

        Assertions.assertEquals(100.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Measure child constraint`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeafWithMeasure(Style()) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(100f),
                height = knownDimensions.height.unwrapOr(100f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(50f), height = Dimension.makeAuto())
        ), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(50.0f, taffy.layout(node).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(node).size.height)

        Assertions.assertEquals(100.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Measure child constraint padding parent`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeafWithMeasure(Style()) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(100f),
                height = knownDimensions.height.unwrapOr(100f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(50f), height = Dimension.makeAuto()),
            padding = Rect(
                left = LengthPercentage.fromLength(10f),
                right = LengthPercentage.fromLength(10f),
                top = LengthPercentage.fromLength(10f),
                bottom = LengthPercentage.fromLength(10f)
            )
        ), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(0.0f, taffy.layout(node).location.x)
        Assertions.assertEquals(0.0f, taffy.layout(node).location.y)
        Assertions.assertEquals(50.0f, taffy.layout(node).size.width)
        Assertions.assertEquals(120.0f, taffy.layout(node).size.height)

        Assertions.assertEquals(10.0f, taffy.layout(child).location.x)
        Assertions.assertEquals(10.0f, taffy.layout(child).location.y)
        Assertions.assertEquals(100.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Measure child with flex grow`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style(
            size = Size(width = Dimension.fromPoints(50f), height = Dimension.fromPoints(50f))
        ))

        val child1 = taffy.newLeafWithMeasure(Style(
            flexGrow = 1f
        )) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(10f),
                height = knownDimensions.height.unwrapOr(50f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(100f), height = Dimension.makeAuto())
        ), listOf(child0, child1))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(50.0f, taffy.layout(child1).size.width)
        Assertions.assertEquals(50.0f, taffy.layout(child1).size.height)
    }

    @Test
    fun `Measure child with flex shrink`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style(
            size = Size(width = Dimension.fromPoints(50f), height = Dimension.fromPoints(50f)),
            flexShrink = 0f
        ))

        val child1 = taffy.newLeafWithMeasure(Style()) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(100f),
                height = knownDimensions.height.unwrapOr(50f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(100f), height = Dimension.makeAuto())
        ), listOf(child0, child1))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100.0f, taffy.layout(child1).size.width)
        Assertions.assertEquals(50.0f, taffy.layout(child1).size.height)
    }

    @Test
    fun `Remeasure child after growing`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style(
            size = Size(width = Dimension.fromPoints(50f), height = Dimension.fromPoints(50f))
        ))

        val child1 = taffy.newLeafWithMeasure(Style(
            flexGrow = 1f
        )) { knownDimensions, _ ->
            val width = knownDimensions.width.unwrapOr(10f)
            val height = knownDimensions.height.unwrapOr(width * 2f)
            Size(
                width = width,
                height = height
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(100f), height = Dimension.makeAuto()),
            alignItems = Option.Some(AlignItems.START)
        ), listOf(child0, child1))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(50.0f, taffy.layout(child1).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child1).size.height)
    }

    @Test
    fun `Remeasure child after shrinking`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style(
            size = Size(width = Dimension.fromPoints(50f), height = Dimension.fromPoints(50f)),
            flexShrink = 0f
        ))

        val child1 = taffy.newLeafWithMeasure(Style(
            flexGrow = 1f
        )) { knownDimensions, _ ->
            val width = knownDimensions.width.unwrapOr(100f)
            val height = knownDimensions.height.unwrapOr(width * 2f)
            Size(
                width = width,
                height = height
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(100f), height = Dimension.makeAuto()),
            alignItems = Option.Some(AlignItems.START)
        ), listOf(child0, child1))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100.0f, taffy.layout(child1).size.width)
        Assertions.assertEquals(200.0f, taffy.layout(child1).size.height)
    }

    @Test
    fun `Remeasure child after stretching`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeafWithMeasure(Style()) { knownDimensions, _ ->
            val height = knownDimensions.height.unwrapOr(50f)
            val width = knownDimensions.width.unwrapOr(height)
            Size(
                width = width,
                height = height
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(100f), height = Dimension.fromPoints(100f))
        ), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Width overrides measure`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeafWithMeasure(Style(
            size = Size(width = Dimension.fromPoints(50f), height = Dimension.makeAuto())
        )) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(100f),
                height = knownDimensions.height.unwrapOr(100f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(50.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Height overrides measure`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeafWithMeasure(Style(
            size = Size(width = Dimension.makeAuto(), height = Dimension.fromPoints(50f))
        )) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(100f),
                height = knownDimensions.height.unwrapOr(100f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(50.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Flex basis overrides measure`() {
        val taffy = be.arby.taffy.Taffy()

        val child0 = taffy.newLeaf(Style(
            flexBasis = Dimension.fromPoints(50f),
            flexGrow = 1f
        ))

        val child1 = taffy.newLeafWithMeasure(Style(
            flexBasis = Dimension.fromPoints(50f),
            flexGrow = 1f
        )) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(100f),
                height = knownDimensions.height.unwrapOr(100f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(200f), height = Dimension.fromPoints(100f))
        ), listOf(child0, child1))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100.0f, taffy.layout(child0).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child0).size.height)
        Assertions.assertEquals(100.0f, taffy.layout(child1).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child1).size.height)
    }

    @Test
    fun `Stretch overrides measure`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeafWithMeasure(Style()) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(50f),
                height = knownDimensions.height.unwrapOr(50f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(100f), height = Dimension.fromPoints(100f))
        ), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(50.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Measure absolute child`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeafWithMeasure(Style(
            position = Position.ABSOLUTE
        )) { knownDimensions, _ ->
            Size(
                width = knownDimensions.width.unwrapOr(50f),
                height = knownDimensions.height.unwrapOr(50f)
            )
        }

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(100f), height = Dimension.fromPoints(100f))
        ), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(50.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(50.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Ignore invalid measure`() {
        val taffy = be.arby.taffy.Taffy()

        val child = taffy.newLeaf(Style(
            flexGrow = 1f
        ))

        val node = taffy.newLeafWithChildren(Style(
            size = Size(width = Dimension.fromPoints(100f), height = Dimension.fromPoints(100f))
        ), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(100.0f, taffy.layout(child).size.width)
        Assertions.assertEquals(100.0f, taffy.layout(child).size.height)
    }

    @Test
    fun `Only measure once`() {
        val taffy = be.arby.taffy.Taffy()
        val numMeasures = AtomicInteger(0)

        val grandchild = taffy.newLeafWithMeasure(Style()) { knownDimensions, _ ->
            numMeasures.getAndIncrement()
            Size(
                width = knownDimensions.width.unwrapOr(50f),
                height = knownDimensions.height.unwrapOr(50f)
            )
        }

        val child = taffy.newLeafWithChildren(Style(), listOf(grandchild))

        val node = taffy.newLeafWithChildren(Style(), listOf(child))

        taffy.computeLayout(node, Size.MAX_CONTENT)

        Assertions.assertEquals(2, numMeasures.get())
    }
}
