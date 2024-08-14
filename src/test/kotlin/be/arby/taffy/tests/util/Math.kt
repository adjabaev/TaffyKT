package be.arby.taffy.tests.util

import be.arby.taffy.lang.Option
import be.arby.taffy.tests.assertEq
import be.arby.taffy.util.maybeAdd
import be.arby.taffy.util.maybeMax
import be.arby.taffy.util.maybeMin
import be.arby.taffy.util.maybeSub
import org.junit.jupiter.api.Test

class LhsOptionF32RhsOptionF32 {
    @Test
    fun `test may be min`() {
        assertEq(Option.Some(3f).maybeMin(Option.Some(5f)), Option.Some(3f))
        assertEq(Option.Some(5f).maybeMin(Option.Some(3f)), Option.Some(3f))
        assertEq(Option.Some(3f).maybeMin(Option.None), Option.Some(3f))
        assertEq(Option.None.maybeMin(Option.Some(3f)), Option.None)
        assertEq(Option.None.maybeMin(Option.None), Option.None)
    }

    @Test
    fun `test maybe max`() {
        assertEq(Option.Some(3f).maybeMax(Option.Some(5f)), Option.Some(5f))
        assertEq(Option.Some(5f).maybeMax(Option.Some(3f)), Option.Some(5f))
        assertEq(Option.Some(3f).maybeMax(Option.None), Option.Some(3f))
        assertEq(Option.None.maybeMax(Option.Some(3f)), Option.None)
        assertEq(Option.None.maybeMax(Option.None), Option.None)
    }

    @Test
    fun `test maybe add`() {
        assertEq(Option.Some(3f).maybeAdd(Option.Some(5f)), Option.Some(8f))
        assertEq(Option.Some(5f).maybeAdd(Option.Some(3f)), Option.Some(8f))
        assertEq(Option.Some(3f).maybeAdd(Option.None), Option.Some(3f))
        assertEq(Option.None.maybeAdd(Option.Some(3f)), Option.None)
        assertEq(Option.None.maybeAdd(Option.None), Option.None)
    }

    @Test
    fun `test maybe sub`() {
        assertEq(Option.Some(3f).maybeSub(Option.Some(5f)), Option.Some(-2f))
        assertEq(Option.Some(5f).maybeSub(Option.Some(3f)), Option.Some(2f))
        assertEq(Option.Some(3f).maybeSub(Option.None), Option.Some(3f))
        assertEq(Option.None.maybeSub(Option.Some(3f)), Option.None)
        assertEq(Option.None.maybeSub(Option.None), Option.None)
    }
}

class LhsOptionF32RhsF32 {
    @Test
    fun `test maybe min`() {
        assertEq(Option.Some(3f).maybeMin(5f), Option.Some(3f))
        assertEq(Option.Some(5f).maybeMin(3f), Option.Some(3f))
        assertEq(Option.None.maybeMin(3f), Option.None)
    }

    @Test
    fun `test maybe max`() {
        assertEq(Option.Some(3f).maybeMax(5f), Option.Some(5f))
        assertEq(Option.Some(5f).maybeMax(3f), Option.Some(5f))
        assertEq(Option.None.maybeMax(3f), Option.None)
    }

    @Test
    fun `test maybe add`() {
        assertEq(Option.Some(3f).maybeAdd(5f), Option.Some(8f))
        assertEq(Option.Some(5f).maybeAdd(3f), Option.Some(8f))
        assertEq(Option.None.maybeAdd(3f), Option.None)
    }

    @Test
    fun `test maybe sub`() {
        assertEq(Option.Some(3f).maybeSub(5f), Option.Some(-2f))
        assertEq(Option.Some(5f).maybeSub(3f), Option.Some(2f))
        assertEq(Option.None.maybeSub(3f), Option.None)
    }
}

class LhsF32RhsOptionF32 {
    @Test
    fun `test maybe min`() {
        assertEq(3f.maybeMin(Option.Some(5f)), 3f)
        assertEq(5f.maybeMin(Option.Some(3f)), 3f)
        assertEq(3f.maybeMin(Option.None), 3f)
    }

    @Test
    fun `test maybe max`() {
        assertEq(3f.maybeMax(Option.Some(5f)), 5f)
        assertEq(5f.maybeMax(Option.Some(3f)), 5f)
        assertEq(3f.maybeMax(Option.None), 3f)
    }

    @Test
    fun `test maybe add`() {
        assertEq(3f.maybeAdd(Option.Some(5f)), 8f)
        assertEq(5f.maybeAdd(Option.Some(3f)), 8f)
        assertEq(3f.maybeAdd(Option.None), 3f)
    }

    @Test
    fun `test maybe sub`() {
        assertEq(3f.maybeSub(Option.Some(5f)), -2f)
        assertEq(5f.maybeSub(Option.Some(3f)), 2f)
        assertEq(3f.maybeSub(Option.None), 3f)
    }
}
