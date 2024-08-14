package be.arby.taffy.tests.util

import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.geom.maybeResolve
import be.arby.taffy.geom.resolveOrZero
import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.tests.assertEq
import org.junit.jupiter.api.Test

fun mrCase(input: Dimension, context: Option<Float>, expected: Option<Float>) {
    assertEq(input.maybeResolve(context), expected)
}

fun mrCase(input: Size<Dimension>, context: Size<Option<Float>>, expected: Size<Option<Float>>) {
    assertEq(input.maybeResolve(context), expected)
}

@JvmName("rozCaseD_OF_F")
fun rozCase(input: Dimension, context: Option<Float>, expected: Float) {
    assertEq(input.resolveOrZero(context), expected)
}

@JvmName("rozCaseRD_OF_RF")
fun rozCase(input: Rect<Dimension>, context: Option<Float>, expected: Rect<Float>) {
    assertEq(input.resolveOrZero(context), expected)
}

@JvmName("rozCaseRD_OF_ROF")
fun rozCase(input: Rect<Dimension>, context: Option<Float>, expected: Rect<Option<Float>>) {
    assertEq(input.resolveOrZero(context), expected)
}

@JvmName("rozCaseRD_SOF_ROF")
fun rozCase(input: Rect<Dimension>, context: Size<Option<Float>>, expected: Rect<Option<Float>>) {
    assertEq(input.resolveOrZero(context), expected)
}

@JvmName("rozCaseRD_SOF_RF")
fun rozCase(input: Rect<Dimension>, context: Size<Option<Float>>, expected: Rect<Float>) {
    assertEq(input.resolveOrZero(context), expected)
}

class MaybeResolveDimension {
    /**
     * `Dimension.AUTO` should always return `None`
     *
     * The parent / context should not affect the outcome.
     */
    @Test
    fun `resolve auto`() {
        mrCase(Dimension.AUTO, Option.None, Option.None)
        mrCase(Dimension.AUTO, Option.Some(5f), Option.None)
        mrCase(Dimension.AUTO, Option.Some(-5f), Option.None)
        mrCase(Dimension.AUTO, Option.Some(0f), Option.None)
    }

    /**
     * `Dimension.Length` should always return `Some(f32)`
     * where the f32 value is the inner absolute length.
     *
     * The parent / context should not affect the outcome.
     */
    @Test
    fun `resolve length`() {
        mrCase(Dimension.Length(1f), Option.None, Option.Some(1f))
        mrCase(Dimension.Length(1f), Option.Some(5f), Option.Some(1f))
        mrCase(Dimension.Length(1f), Option.Some(-5f), Option.Some(1f))
        mrCase(Dimension.Length(1f), Option.Some(0f), Option.Some(1f))
    }

    /**
     * `Dimension.Percent` should return `None` if context is  `None`.
     * Otherwise it should return `Some(f32)`
     * where the f32 value is the inner value of the percent * context value.
     *
     * The parent / context __should__ affect the outcome.
     */
    @Test
    fun `resolve percent`() {
        mrCase(Dimension.Percent(1f), Option.None, Option.None)
        mrCase(Dimension.Percent(1f), Option.Some(5f), Option.Some(5f))
        mrCase(Dimension.Percent(1f), Option.Some(-5f), Option.Some(-5f))
        mrCase(Dimension.Percent(1f), Option.Some(50f), Option.Some(50f))
    }
}

class MaybeResolveSizeDimension {
    /**
     * Size<Dimension.AUTO> should always return Size<None>
     *
     * The parent / context should not affect the outcome.
     */
    @Test
    fun `maybe resolve auto`() {
        mrCase(Size.autoD(), Size.NONE, Size.NONE)
        mrCase(Size.autoD(), Size.new(5f, 5f), Size.NONE)
        mrCase(Size.autoD(), Size.new(-5f, -5f), Size.NONE)
        mrCase(Size.autoD(), Size.new(0f, 0f), Size.NONE)
    }

    /**
     * Size<Dimension.Length> should always return a Size<Some(f32)>
     * where the f32 values are the absolute length.
     *
     * The parent / context should not affect the outcome.
     */
    @Test
    fun `maybe resolve length`() {
        mrCase(Size.fromLengths(5f, 5f), Size.NONE, Size.new(5f, 5f))
        mrCase(Size.fromLengths(5f, 5f), Size.new(5f, 5f), Size.new(5f, 5f))
        mrCase(Size.fromLengths(5f, 5f), Size.new(-5f, -5f), Size.new(5f, 5f))
        mrCase(Size.fromLengths(5f, 5f), Size.new(0f, 0f), Size.new(5f, 5f))
    }

    /**
     * `Size<Dimension.Percent>` should return `Size<None>` if context is `Size<None>`.
     * Otherwise it should return `Size<Some(f32)>`
     * where the f32 value is the inner value of the percent * context value.
     *
     * The context __should__ affect the outcome.
     */
    @Test
    fun `maybe resolve percent`() {
        mrCase(Size.fromPercent(5f, 5f), Size.NONE, Size.NONE)
        mrCase(Size.fromPercent(5f, 5f), Size.new(5f, 5f), Size.new(25f, 25f))
        mrCase(Size.fromPercent(5f, 5f), Size.new(-5f, -5f), Size.new(-25f, -25f))
        mrCase(Size.fromPercent(5f, 5f), Size.new(0f, 0f), Size.new(0f, 0f))
    }
}

class ResolveOrZeroDimensionToOptionF32 {
    @Test
    fun `resolve or zero auto`() {
        rozCase(Dimension.AUTO, Option.None, 0f)
        rozCase(Dimension.AUTO, Option.Some(5f), 0f)
        rozCase(Dimension.AUTO, Option.Some(-5f), 0f)
        rozCase(Dimension.AUTO, Option.Some(0f), 0f)
    }

    @Test
    fun `resolve or zero length`() {
        rozCase(Dimension.Length(5f), Option.None, 5f)
        rozCase(Dimension.Length(5f), Option.Some(5f), 5f)
        rozCase(Dimension.Length(5f), Option.Some(-5f), 5f)
        rozCase(Dimension.Length(5f), Option.Some(0f), 5f)
    }

    @Test
    fun `resolve or zero percent`() {
        rozCase(Dimension.Percent(5f), Option.None, 0f)
        rozCase(Dimension.Percent(5f), Option.Some(5f), 25f)
        rozCase(Dimension.Percent(5f), Option.Some(-5f), -25f)
        rozCase(Dimension.Percent(5f), Option.Some(0f), 0f)
    }
}

class ResolveOrZeroRectDimensionToRect {
    @Test
    fun `resolve or zero auto`() {
        rozCase(Rect.auto<Dimension>(), Size.NONE, Rect.zero<Float>())
        rozCase(Rect.auto<Dimension>(), Size.new(5f, 5f), Rect.zero<Float>())
        rozCase(Rect.auto<Dimension>(), Size.new(-5f, -5f), Rect.zero<Float>())
        rozCase(Rect.auto<Dimension>(), Size.new(0f, 0f), Rect.zero<Float>())
    }

    @Test
    fun `resolve or zero length`() {
        rozCase(Rect.fromLength(5f, 5f, 5f, 5f), Size.NONE, Rect.new(5f, 5f, 5f, 5f))
        rozCase(Rect.fromLength(5f, 5f, 5f, 5f), Size.new(5f, 5f), Rect.new(5f, 5f, 5f, 5f))
        rozCase(Rect.fromLength(5f, 5f, 5f, 5f), Size.new(-5f, -5f), Rect.new(5f, 5f, 5f, 5f))
        rozCase(Rect.fromLength(5f, 5f, 5f, 5f), Size.new(0f, 0f), Rect.new(5f, 5f, 5f, 5f))
    }

    @Test
    fun `resolve or zero percent`() {
        rozCase(Rect.fromPercent(5f, 5f, 5f, 5f), Size.NONE, Rect.zero<Float>())
        rozCase(Rect.fromPercent(5f, 5f, 5f, 5f), Size.new(5f, 5f), Rect.new(25f, 25f, 25f, 25f))
        rozCase(
            Rect.fromPercent(5f, 5f, 5f, 5f),
            Size.new(-5f, -5f),
            Rect.new(-25f, -25f, -25f, -25f),
        )
        rozCase(Rect.fromPercent(5f, 5f, 5f, 5f), Size.new(0f, 0f), Rect.zero<Float>())
    }
}

class ResolveOrZeroRectDimensionToRectF32ViaOption {
    @Test
    fun `resolve or zero auto`() {
        rozCase(Rect.auto<Dimension>(), Option.None, Rect.zero<Float>())
        rozCase(Rect.auto<Dimension>(), Option.Some(5f), Rect.zero<Float>())
        rozCase(Rect.auto<Dimension>(), Option.Some(-5f), Rect.zero<Float>())
        rozCase(Rect.auto<Dimension>(), Option.Some(0f), Rect.zero<Float>())
    }

    @Test
    fun `resolve or zero length`() {
        rozCase(Rect.fromLength(5f, 5f, 5f, 5f), Option.None, Rect.new(5f, 5f, 5f, 5f))
        rozCase(Rect.fromLength(5f, 5f, 5f, 5f), Option.Some(5f), Rect.new(5f, 5f, 5f, 5f))
        rozCase(Rect.fromLength(5f, 5f, 5f, 5f), Option.Some(-5f), Rect.new(5f, 5f, 5f, 5f))
        rozCase(Rect.fromLength(5f, 5f, 5f, 5f), Option.Some(0f), Rect.new(5f, 5f, 5f, 5f))
    }

    @Test
    fun `resolve or zero percent`() {
        rozCase(Rect.fromPercent(5f, 5f, 5f, 5f), Option.None, Rect.zero<Float>())
        rozCase(Rect.fromPercent(5f, 5f, 5f, 5f), Option.Some(5f), Rect.new(25f, 25f, 25f, 25f))
        rozCase(Rect.fromPercent(5f, 5f, 5f, 5f), Option.Some(-5f), Rect.new(-25f, -25f, -25f, -25f))
        rozCase(Rect.fromPercent(5f, 5f, 5f, 5f), Option.Some(0f), Rect.zero<Float>())
    }
}
