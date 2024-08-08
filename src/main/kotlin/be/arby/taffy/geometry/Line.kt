package be.arby.taffy.geometry

import be.arby.taffy.style.grid.GenericGridPlacement
import be.arby.taffy.style.grid.GridPlacement
import be.arby.taffy.style.helpers.TaffyGridLine
import be.arby.taffy.style.helpers.TaffyGridSpan

data class Line<T>(var start: T, var end: T) : TaffyGridLine, TaffyGridSpan {
    internal fun <R> map(f: (T) -> R): Line<R> {
        return Line(
            start = f(start),
            end = f(end)
        )
    }

    companion object {
        @JvmStatic
        fun zero() = Line(0f, 0f)

        @JvmStatic
        fun fromLineIndex(index: Short): TaffyGridLine {
            return Line(start = GridPlacement.fromLineIndex(index), end = GenericGridPlacement.Auto())
        }

        @JvmStatic
        fun fromSpan(index: Int): TaffyGridSpan {
            return Line(start = GridPlacement.fromSpan(index), end = GenericGridPlacement.Auto())
        }

        @JvmStatic
        fun of(x: Float, y: Float) = Line(x, y)
        fun autoGP(): Line<GridPlacement> {
            return Line(GenericGridPlacement.Auto(), GenericGridPlacement.Auto())
        }
    }
}
