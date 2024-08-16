package be.arby.taffy.geom

import be.arby.taffy.compute.grid.types.*
import be.arby.taffy.lang.Option
import be.arby.taffy.style.grid.GenericGridPlacement
import be.arby.taffy.style.grid.GridPlacement
import be.arby.taffy.style.grid.OriginZeroGridPlacement
import kotlin.math.max

/**
 * An abstract "line". Represents any type that has a start and an end
 */
data class Line<T>(
    /**
     * The start position of a line
     */
    var start: T,
    /**
     * The end position of a line
     */
    var end: T
): Cloneable {
    public override fun clone(): Line<T> {
        return Line(start = start, end = end)
    }

    /**
     * Applies the function `f` to both the width and height
     *
     * This is used to transform a `Line<T>` into a `Line<R>`.
     */
    fun <R> map(f: (T) -> R): Line<R> {
        return Line(
            start = f(start),
            end = f(end)
        )
    }

    /**
     * Adds the start and end values together and returns the result
     */
    fun Line<Float>.sum(): Float {
        return start + end
    }

    /**
     * Made to make code resemble the original Rust code
     */
    fun t2(): Pair<T, T> {
        return Pair(start, end)
    }

    companion object {
        /**
         * A `Line<bool>` with both start and end set to `true`
         */
        val TRUE = Line(start = true, end = true)

        /**
         * A `Line<bool>` with both start and end set to `false`
         */
        val FALSE = Line(start = false, end = false)
    }
}

/**
 * Adds the start and end values together and returns the result
 */
fun Line<Float>.sum(): Float {
    return start + end
}

/**
 * The number of tracks between the start and end lines
 */
fun Line<OriginZeroLine>.span(): Int {
    return max(end.value - start.value, 0)
}

/**
 * Resolves the span for an indefinite placement (a placement that does not consist of two `Track`s).
 * Panics if called on a definite placement
 */
fun <T : GridCoordinate> Line<GenericGridPlacement<T>>.indefiniteSpan(): Int {
    return when {
        start.isLine() && end.isAuto() -> 1
        start.isAuto() && end.isLine() -> 1
        start.isAuto() && end.isAuto() -> 1
        start.isLine() && end.isSpan() -> end.getSpan()
        start.isSpan() && end.isLine() -> start.getSpan()
        start.isSpan() && end.isAuto() -> start.getSpan()
        start.isAuto() && end.isSpan() -> end.getSpan()
        start.isSpan() && end.isSpan() -> start.getSpan()
        else -> {
            throw UnsupportedOperationException("indefiniteSpan should only be called on indefinite grid tracks")
        }
    }
}

/**
 * Whether the track position is definite in this axis (or the item will need auto placement)
 * The track position is definite if least one of the start and end positions is a NON-ZERO track index
 * (0 is an invalid line in GridLine coordinates, and falls back to "auto" which is indefinite)
 */
@JvmName("isDefiniteGridPlacement")
fun Line<GridPlacement>.isDefinite(): Boolean {
    if (start.isLine()) {
        if (start.getLine<GridLine>().asI16() != 0) {
            return true
        }
    } else if (end.isLine()) {
        if (end.getLine<GridLine>().asI16() != 0) {
            return true
        }
    }
    return false
}

/**
 * Apply a mapping function if the [`GridPlacement`] is a `Track`. Otherwise return `self` unmodified.
 */
fun Line<GridPlacement>.intoOriginZero(explicitTrackCount: Int): Line<OriginZeroGridPlacement> {
    return Line(
        start = start.intoOriginZeroPlacement(explicitTrackCount),
        end = end.intoOriginZeroPlacement(explicitTrackCount)
    )
}

/**
 * Whether the track position is definite in this axis (or the item will need auto placement)
 * The track position is definite if least one of the start and end positions is a track index
 */
@JvmName("isDefiniteOriginZeroGridPlacement")
fun Line<OriginZeroGridPlacement>.isDefinite(): Boolean {
    return start.isLine() || end.isLine()
}

/**
 * If at least one of the of the start and end positions is a track index then the other end can be resolved
 * into a track index purely based on the information contained with the placement specification
 */
fun Line<OriginZeroGridPlacement>.resolveDefiniteGridLines(): Line<OriginZeroLine> {
    return if (start.isLine() && end.isLine()) {
        val sl = start.getLine<OriginZeroLine>()
        val el = end.getLine<OriginZeroLine>()
        if (sl == el) {
            Line(start = start.getLine(), end = start.getLine<OriginZeroLine>() + 1)
        } else {
            Line(start = minLine(sl, el), end = maxLine(sl, el))
        }
    } else if (start.isLine() && end.isSpan()) {
        val sl = start.getLine<OriginZeroLine>()
        Line(start = sl, end = sl + end.getSpan())
    } else if (start.isLine() && end.isAuto()) {
        val sl = start.getLine<OriginZeroLine>()
        Line(start = sl, end = sl + 1)
    } else if (start.isSpan() && end.isLine()) {
        val el = end.getLine<OriginZeroLine>()
        Line(start = el - start.getSpan(), end = el)
    } else if (start.isAuto() && end.isLine()) {
        val el = end.getLine<OriginZeroLine>()
        Line(start = el - 1, end = el)
    } else {
        throw UnsupportedOperationException("resolveDefiniteGridTracks should only be called on definite grid tracks")
    }
}

/**
 * If neither of the start and end positions is a track index then the other end can be resolved
 * into a track index if a definite start position is supplied externally
 */
fun Line<OriginZeroGridPlacement>.resolveIndefiniteGridTracks(start: OriginZeroLine) : Line<OriginZeroLine> {
    return if (this.start.isAuto() && this.end.isAuto()) {
        Line(start = start, end = start + 1)
    } else if (this.start.isSpan() && this.end.isAuto()) {
        Line(start = start, end = start + this.start.getSpan())
    } else if (this.start.isAuto() && this.end.isSpan()) {
        Line(start = start, end = start + this.start.getSpan())
    } else if (this.start.isSpan() && this.end.isSpan()) {
        Line(start = start, end = start + this.start.getSpan())
    } else {
        throw UnsupportedOperationException("resolveIndefiniteGridTracks should only be called on indefinite grid tracks")
    }
}

/**
 * For absolutely positioned items:
 *   - Tracks resolve to definite tracks
 *   - For Spans:
 *      - If the other position is a Track, they resolve to a definite track relative to the other track
 *      - Else resolve to None
 *   - Auto resolves to None
 * When finally positioning the item, a value of None means that the item's grid area is bounded by the grid
 * container's border box on that side.
 */
fun Line<OriginZeroGridPlacement>.resolveAbsolutelyPositionedGridTracks(): Line<Option<OriginZeroLine>> {
    return if (start.isLine() && end.isLine()) {
        val sl = start.getLine<OriginZeroLine>()
        val el = end.getLine<OriginZeroLine>()
        if (sl == el) {
            return Line(start = Option.Some(sl), end = Option.Some(sl + 1))
        } else {
            return Line(start = Option.Some(minLine(sl, el)), end = Option.Some(maxLine(sl, el)))
        }
    } else if (start.isLine() && end.isSpan()) {
        val sl = start.getLine<OriginZeroLine>()
        return Line(start = Option.Some(start.getLine()), end = Option.Some(sl + end.getSpan()))
    } else if (start.isLine() && end.isAuto()) {
        return Line(start = Option.Some(start.getLine()), end = Option.None)
    } else if (start.isSpan() && end.isLine()) {
        val el = end.getLine<OriginZeroLine>()
        return Line(start = Option.Some(el - start.getSpan()), end = Option.Some(el))
    } else if (start.isAuto() && end.isLine()) {
        return Line(start = Option.None, end = Option.Some(end.getLine()))
    } else {
        return Line(start = Option.None, end = Option.None)
    }
}
