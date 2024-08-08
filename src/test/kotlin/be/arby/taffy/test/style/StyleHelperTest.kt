package be.arby.taffy.test.style

import net.asterium.taffy.utils.StyleHelper
import net.asterium.taffy.style.grid.GridTrackRepetition
import net.asterium.taffy.style.grid.TrackSizingFunction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StyleHelperTest {
    @Test
    fun `Test repeat u16`() {
        Assertions.assertEquals(TrackSizingFunction.Repeat(GridTrackRepetition.Count(123),
            ArrayList()), StyleHelper.repeat(123, ArrayList())
        )
    }
}
