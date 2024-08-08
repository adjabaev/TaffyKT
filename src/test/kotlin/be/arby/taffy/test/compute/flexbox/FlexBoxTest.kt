package be.arby.taffy.test.compute.flexbox

import be.arby.taffy.Taffy
import net.asterium.taffy.compute.flexbox.FlexBox
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.geometry.extensions.horizontalAxisSum
import net.asterium.taffy.geometry.extensions.plus
import net.asterium.taffy.geometry.extensions.verticalAxisSum
import net.asterium.taffy.util.maybeSub
import net.asterium.taffy.resolve.resolveOrZeroStR
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.flex.FlexWrap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FlexBoxTest {

    @Test
    fun `Correct constants`() {
        val tree = be.arby.taffy.Taffy()
        val style = Style()
        val nodeId = tree.newLeaf(style)
        val nodeSize = Size.none()
        val parentSize = Size.none()
        val constants = FlexBox.computeConstants(tree.style(nodeId), nodeSize, parentSize)
        Assertions.assertTrue(constants.dir == style.flexDirection)
        Assertions.assertTrue(constants.isRow == style.flexDirection.isRow())
        Assertions.assertTrue(constants.isColumn == style.flexDirection.isColumn())
        Assertions.assertTrue(constants.isWrapReverse == (style.flexWrap == FlexWrap.WRAP_REVERSE))
        val margin = style.margin.resolveOrZeroStR(parentSize)
        Assertions.assertEquals(margin, constants.margin)
        val border = style.border.resolveOrZeroStR(parentSize)
        val padding = style.padding.resolveOrZeroStR(parentSize)
        val paddingBorder = padding + border
        Assertions.assertEquals(border, constants.border)
        Assertions.assertEquals(paddingBorder, constants.paddingBorder)
        val innerSize = Size(
            width = nodeSize.width.maybeSub(paddingBorder.horizontalAxisSum()),
            height = nodeSize.height.maybeSub(paddingBorder.verticalAxisSum()),
        )
        Assertions.assertEquals(innerSize, constants.nodeInnerSize)
        Assertions.assertEquals(Size.zeroF(), constants.containerSize)
        Assertions.assertEquals(Size.zeroF(), constants.innerContainerSize)
    }
}
