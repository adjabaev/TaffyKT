package be.arby.taffy.style.grid

import be.arby.taffy.compute.grid.types.GridCoordinate
import be.arby.taffy.compute.grid.types.GridLine
import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.lang.Default
import be.arby.taffy.style.helpers.TaffyGridLine
import be.arby.taffy.style.helpers.TaffyGridSpan

/**
 * A grid line placement using the normalized OriginZero coordinates to specify line positions.
 */
typealias OriginZeroGridPlacement = GenericGridPlacement<OriginZeroLine>

/**
 * A grid line placement specification. Used for grid-[row/column]-[start/end]. Named tracks are not implemented.
 *
 * Defaults to `GridPlacement::Auto`
 *
 * [Specification](https://www.w3.org/TR/css3-grid-layout/#typedef-grid-row-start-grid-line)
 */
typealias GridPlacement = GenericGridPlacement<GridLine>

/**
 * A grid line placement specification which is generic over the coordinate system that it uses to define
 * grid line positions.
 *
 * GridPlacement is aliased as GridPlacement and is exposed to users of Taffy to define styles.
 * GenericGridPlacement<OriginZeroLine> is aliased as OriginZeroGridPlacement and is used internally for placement computations.
 *
 * See [`crate::compute::grid::type::coordinates`] for documentation on the different coordinate systems.
 */
sealed class GenericGridPlacement<LineType : GridCoordinate> : TaffyGridLine, TaffyGridSpan {
    /**
     * Place item according to the auto-placement algorithm, and the parent's grid_auto_flow property
     */
    class Auto<LineType : GridCoordinate> : GenericGridPlacement<LineType>()

    /**
     * Place item at specified line (column or row) index
     */
    data class Line<LineType : GridCoordinate>(val s: LineType) : GenericGridPlacement<LineType>()

    /**
     * Item should span specified number of tracks (columns or rows)
     */
    data class Span<LineType : GridCoordinate>(val i: Int) : GenericGridPlacement<LineType>()

    open fun isAuto(): Boolean {
        return this is Auto
    }

    open fun isLine(): Boolean {
        return this is Line
    }

    open fun isSpan(): Boolean {
        return this is Span
    }

    /**
     * Apply a mapping function if the [`GridPlacement`] is a `Track`. Otherwise return `self` unmodified.
     */
    fun intoOriginZeroPlacement(explicitTrackCount: Int): OriginZeroGridPlacement {
        return if (this is Auto) {
            Auto()
        } else if (this is Span) {
            Span(i)

            // Grid line zero is an invalid index, so it gets treated as Auto
            // See: https://developer.mozilla.org/en-US/docs/Web/CSS/grid-row-start#values
        } else {
            val s = ((this as Line).s) as GridLine
            if (s.asI16() == 0) Auto() else Line(s.intoOriginZeroLine(explicitTrackCount))
        }
    }

    fun <T> getLine(): T {
        return ((this as Line).s) as T
    }

    fun getSpan(): Int {
        return (this as Span).i
    }

    companion object : Default<GridPlacement> {
        val AUTO = Auto<GridLine>()

        fun fromLineIndex(index: Int): GridPlacement {
            return Line(GridLine.from(index))
        }

        fun fromSpan(span: Int): GridPlacement {
            return Span(span)
        }

        override fun default(): GridPlacement {
            return Auto()
        }
    }
}
