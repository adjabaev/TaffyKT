package be.arby.taffy.test.style

import net.asterium.taffy.geometry.Line
import net.asterium.taffy.geometry.Rect
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.lang.Option
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.Dimension
import net.asterium.taffy.style.Display
import net.asterium.taffy.style.Position
import net.asterium.taffy.style.flex.FlexDirection
import net.asterium.taffy.style.flex.FlexWrap
import net.asterium.taffy.style.grid.GridAutoFlow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class StyleTest {
    @Test
    fun `Defaults match`() {
        val style = Style(
            display = Display.FLEX,
            // Position properties
            position = Position.RELATIVE,
            inset = Rect.autoLPA(),
            // Size properties
            size = Size.autoD(),
            minSize = Size.autoD(),
            maxSize = Size.autoD(),
            aspectRatio = Option.None,
            // Spacing Properties
            margin = Rect.zeroLPA(),
            padding = Rect.zeroLP(),
            border = Rect.zeroLP(),
            // Alignment properties
            alignItems = Option.None,
            alignSelf = Option.None,
            justifyItems = Option.None,
            justifySelf = Option.None,
            alignContent = Option.None,
            justifyContent = Option.None,
            gap = Size.zeroLP(),
            // Flexbox properties
            flexDirection = FlexDirection.ROW,
            flexWrap = FlexWrap.NO_WRAP,
            flexBasis = Dimension.Auto,
            flexGrow = 0.0f,
            flexShrink = 1.0f,
            // Grid container properties
            gridTemplateRows = ArrayList(),
            gridTemplateColumns = ArrayList(),
            gridAutoRows = ArrayList(),
            gridAutoColumns = ArrayList(),
            gridAutoFlow = GridAutoFlow.ROW,
            // Grid child properties
            gridRow = Line.autoGP(),
            gridColumn = Line.autoGP()
        )

        Assertions.assertEquals(Style(), style)
    }
}
