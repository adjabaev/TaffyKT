package be.arby.taffy.tests.style

import be.arby.taffy.geom.Line
import be.arby.taffy.geom.Point
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.*
import be.arby.taffy.style.block.TextAlign
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.flex.FlexDirection
import be.arby.taffy.style.flex.FlexWrap
import be.arby.taffy.style.grid.GenericGridPlacement
import be.arby.taffy.style.grid.GridAutoFlow
import be.arby.taffy.tests.assertEq
import be.arby.taffy.vec
import org.junit.jupiter.api.Test

class Mod {

    @Test
    fun `defaults match`() {
        val oldDefaults = Style(
            display = Display.default(),
            itemIsTable = false,
            boxSizing = BoxSizing.default(),
            overflow = Point(x = Overflow.VISIBLE, y = Overflow.VISIBLE),
            scrollbarWidth = 0f,
            position = Position.default(),
            flexDirection = FlexDirection.default(),
            flexWrap = FlexWrap.default(),
            alignItems = Option.None,
            alignSelf = Option.None,
            justifyItems = Option.None,
            justifySelf = Option.None,
            alignContent = Option.None,
            justifyContent = Option.None,
            inset = Rect.auto(),
            margin = Rect.zero(),
            padding = Rect.zero(),
            border = Rect.zero(),
            gap = Size.zeroLP(),
            textAlign = TextAlign.AUTO,
            flexGrow = 0f,
            flexShrink = 1f,
            flexBasis = Dimension.Auto,
            size = Size.autoD(),
            minSize = Size.autoD(),
            maxSize = Size.autoD(),
            aspectRatio = Option.None,
            gridTemplateRows = vec(),
            gridTemplateColumns = vec(),
            gridAutoRows = vec(),
            gridAutoColumns = vec(),
            gridAutoFlow = GridAutoFlow.ROW,
            gridRow = Line(start = GenericGridPlacement.Auto(), end = GenericGridPlacement.Auto()),
            gridColumn = Line(start = GenericGridPlacement.Auto(), end = GenericGridPlacement.Auto()),
        )

        assertEq(Style.DEFAULT, Style.default())
        assertEq(Style.DEFAULT, oldDefaults)
    }
}
