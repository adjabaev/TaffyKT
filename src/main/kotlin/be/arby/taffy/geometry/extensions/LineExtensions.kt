package be.arby.taffy.geometry.extensions

import be.arby.taffy.compute.grid.types.GridCoordinate
import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.geometry.Line
import be.arby.taffy.lang.Option
import be.arby.taffy.style.grid.GenericGridPlacement
import be.arby.taffy.style.grid.GridPlacement
import be.arby.taffy.style.grid.OriginZeroGridPlacement
import be.arby.taffy.utils.max
import be.arby.taffy.utils.min
import java.util.*
import kotlin.math.max

fun Line<Float>.sum(): Float {
    return start + end
}

fun <T : GridCoordinate> Line<GenericGridPlacement<T>>.isDefinite(): Boolean {
    return this.start is GenericGridPlacement.Line || this.end is GenericGridPlacement.Line
}

fun <T : GridCoordinate> Line<GenericGridPlacement<T>>.indefiniteSpan(): Int {
    return when {
        this.start.isLine() && this.end.isAuto() -> 1
        this.start.isAuto() && this.end.isLine() -> 1
        this.start.isAuto() && this.end.isAuto() -> 1
        this.start.isLine() && this.end.isSpan() -> this.end.getSpan()
        this.start.isSpan() && this.end.isLine() -> this.start.getSpan()
        this.start.isSpan() && this.end.isAuto() -> this.start.getSpan()
        this.start.isAuto() && this.end.isSpan() -> this.end.getSpan()
        this.start.isSpan() && this.end.isSpan() -> this.start.getSpan()
        else -> throw UnsupportedOperationException("indefinite_span should only be called on indefinite grid tracks")
    }
}

fun Line<OriginZeroLine>.span(): Int {
    return max(end.value - start.value, 0)
}

fun Line<GridPlacement>.intoOriginZero(explicitTrackCount: Int): Line<OriginZeroGridPlacement> {
    return Line(
        start = start.intoOriginZeroPlacement(explicitTrackCount),
        end = end.intoOriginZeroPlacement(explicitTrackCount)
    )
}

fun Line<OriginZeroGridPlacement>.resolveDefiniteGridLines(): Line<OriginZeroLine> {
    return when {
        this.start.isLine() && this.end.isLine() -> {
            val line1 = start.getLine()
            val line2 = end.getLine()
            if (line1 == line2) {
                Line(start = line1, end = line1 + 1)
            } else {
                Line(start = min(line1, line2), end = max(line1, line2))
            }
        }

        this.start.isLine() && this.end.isSpan() -> {
            val line = start.getLine()
            val span = end.getSpan()
            Line(start = line, end = line + span)
        }

        this.start.isLine() && this.end.isAuto() -> {
            val line = start.getLine()
            Line(start = line, end = line + 1)
        }

        this.start.isSpan() && this.end.isLine() -> {
            val span = start.getSpan()
            val line = end.getLine()
            Line(start = line - span, end = line)
        }

        this.start.isAuto() && this.end.isLine() -> {
            val line = end.getLine()
            Line(start = line - 1, end = line)
        }

        else -> throw UnsupportedOperationException("resolveDefiniteGridTracks should only be called on definite grid tracks")
    }
}

fun Line<OriginZeroGridPlacement>.resolveAbsolutelyPositionedGridTracks(): Line<Option<OriginZeroLine>> {
    return when {
        this.start.isLine() && this.end.isLine() -> {
            val track1 = start.getLine()
            val track2 = end.getLine()
            if (track1 == track2) {
                Line(start = Option.Some(track1), end = Option.Some(track1 + 1))
            } else {
                Line(start = Option.Some(min(track1, track2)), end = Option.Some(max(track1, track2)))
            }
        }

        this.start.isLine() && this.end.isSpan() -> {
            val track = start.getLine()
            val span = end.getSpan()
            Line(start = Option.Some(track), end = Option.Some(track + span))
        }

        this.start.isLine() && this.end.isAuto() -> {
            val track = start.getLine()
            Line(start = Option.Some(track), end = Option.None)
        }

        this.start.isSpan() && this.end.isLine() -> {
            val span = start.getSpan()
            val track = end.getLine()
            Line(start = Option.Some(track - span), end = Option.Some(track))
        }

        this.start.isAuto() && this.end.isLine() -> {
            val track = end.getLine()
            Line(start = Option.None, end = Option.Some(track))
        }

        else -> Line(start = Option.None, end = Option.None)
    }
}

fun Line<OriginZeroGridPlacement>.resolveIndefiniteGridTracks(start: OriginZeroLine): Line<OriginZeroLine> {
    return if (this.start.isAuto() && this.end.isAuto()) {
        Line(start = start, end = (start + 1))
    } else if (this.start.isSpan() && this.end.isAuto()) {
        val span = this.start.getSpan()
        Line(start = start, end = (start + span))
    } else if (this.start.isAuto() && this.end.isSpan()) {
        val span = this.start.getSpan()
        Line(start = start, end = (start + span))
    } else if (this.start.isSpan() && this.end.isSpan()) {
        val span = this.start.getSpan()
        Line(start = start, end = (start + span))
    } else {
        throw UnsupportedOperationException("resolveIndefiniteGridTracks should only be called on indefinite grid tracks")
    }
}

fun GridPlacement.intoOriginZeroPlacement(explicitTrackCount: Int): OriginZeroGridPlacement {
    return when (this) {
        is GenericGridPlacement.Auto -> GenericGridPlacement.Auto()
        is GenericGridPlacement.Span -> GenericGridPlacement.Span(this.i)
        // Grid line zero is an invalid index, so it gets treated as Auto
        // See: https://developer.mozilla.org/en-US/docs/Web/CSS/grid-row-start#values
        is GenericGridPlacement.Line -> when (this.s.asShort()) {
            0.toShort() -> GenericGridPlacement.Auto()
            else -> GenericGridPlacement.Line(this.s.intoOriginZeroLine(explicitTrackCount))
        }
    }
}
