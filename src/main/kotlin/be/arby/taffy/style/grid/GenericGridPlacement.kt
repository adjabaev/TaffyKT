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
 * GenericGridPlacement<GridLine> is aliased as GridPlacement and is exposed to users of Taffy to define styles.
 * GenericGridPlacement<OriginZeroLine> is aliased as OriginZeroGridPlacement and is used internally for placement computations.
 *
 * See [`crate::compute::grid::type::coordinates`] for documentation on the different coordinate systems.
 */
sealed class GenericGridPlacement<LineType : GridCoordinate> : TaffyGridLine, TaffyGridSpan {
    /**
     * Place item according to the auto-placement algorithm, and the parent's grid_auto_flow property
     */
    object Auto: GenericGridPlacement<Nothing>() {
        override fun isAuto(): Boolean {
            return true
        }
    }

    /**
     * Place item at specified line (column or row) index
     */
    data class Line<LineType : GridCoordinate>(val s: LineType) : GenericGridPlacement<LineType>() {
        override fun isLine(): Boolean {
            return true
        }

        override fun getLine(): LineType {
            return s
        }
    }

    /**
     * Item should span specified number of tracks (columns or rows)
     */
    data class Span<LineType : GridCoordinate>(val i: Int) : GenericGridPlacement<LineType>() {
        override fun isSpan(): Boolean {
            return true
        }

        override fun getSpan(): Int {
            return i
        }
    }

    open fun isAuto(): Boolean {
        return false
    }

    open fun isLine(): Boolean {
        return false
    }

    open fun getLine(): LineType {
        throw UnsupportedOperationException("Raw usage")
    }

    open fun isSpan(): Boolean {
        return false
    }

    open fun getSpan(): Int {
        throw UnsupportedOperationException("Raw usage")
    }

    companion object: Default<GridPlacement> {
        val AUTO = Auto

        fun fromLineIndex(index: Short): GridPlacement {
            return Line(GridLine.from(index))
        }

        fun fromSpan(span: Int): GridPlacement {
            return Span(span)
        }

        override fun default(): GridPlacement {
            return Auto
        }
    }
}
