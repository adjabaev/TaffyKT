package be.arby.taffy.compute.grid.util

import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.fr
import be.arby.taffy.geom.Line
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.tuples.T4
import be.arby.taffy.style.Display
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.grid.GridPlacement

fun T4<Float, Float, Int, Int>.intoGrid(): Style {
    return Style.DEFAULT.copy(
        display = Display.GRID,
        size = Size(width = Dimension.Length(first), height = Dimension.Length(second)),
        gridTemplateColumns = MutableList(third) { fr(1f) },
        gridTemplateRows = MutableList(fourth) { fr(1f) }
    )
}

fun T4<Int, Int, Int, Int>.intoOz(): T4<OriginZeroLine, OriginZeroLine, OriginZeroLine, OriginZeroLine> {
    return T4(
        OriginZeroLine(first),
        OriginZeroLine(second),
        OriginZeroLine(third),
        OriginZeroLine(fourth)
    )
}

fun T4<GridPlacement, GridPlacement, GridPlacement, GridPlacement>.intoGridChild(): Style {
    return Style.DEFAULT.copy(
        display = Display.GRID,
        gridColumn = Line(start = first, end = second),
        gridRow = Line(start = third, end = fourth)
    )
}
