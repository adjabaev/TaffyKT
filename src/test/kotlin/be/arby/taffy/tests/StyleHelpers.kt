package be.arby.taffy.tests

import be.arby.taffy.rpt
import be.arby.taffy.style.grid.GridTrackRepetition
import be.arby.taffy.style.grid.NonRepeatedTrackSizingFunction
import be.arby.taffy.style.grid.TrackSizingFunction
import be.arby.taffy.vec
import org.junit.jupiter.api.Test

class RepeatFnTests {
    private val TEST_VEC = vec<NonRepeatedTrackSizingFunction>()

    @Test
    fun `test repeat u16`() {
        assertEq(rpt(GridTrackRepetition.tryFrom1(123).unwrap(), TEST_VEC), TrackSizingFunction.Repeat(GridTrackRepetition.Count(123), TEST_VEC))
    }

    @Test
    fun `test repeat auto fit str`() {
        assertEq(rpt(GridTrackRepetition.tryFrom2("auto-fit").unwrap(), TEST_VEC), TrackSizingFunction.Repeat(GridTrackRepetition.AutoFit, TEST_VEC))
    }

    @Test
    fun `test repeat auto fill str`() {
        assertEq(rpt(GridTrackRepetition.tryFrom2("auto-fill").unwrap(), TEST_VEC), TrackSizingFunction.Repeat(GridTrackRepetition.AutoFill, TEST_VEC))
    }
}
