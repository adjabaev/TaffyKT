package be.arby.taffy.test.compute.grid

import net.asterium.taffy.compute.grid.types.OriginZeroLine
import net.asterium.taffy.geometry.Line
import net.asterium.taffy.geometry.Size
import net.asterium.taffy.style.Style
import net.asterium.taffy.style.dimension.Dimension
import net.asterium.taffy.style.Display
import net.asterium.taffy.style.grid.GridPlacement
import net.asterium.taffy.style.grid.TrackSizingFunction
import net.asterium.taffy.utils.tuples.Quadruple

fun Quadruple<Float, Float, Int, Int>.intoGrid(): Style {
    return Style(
        display = Display.GRID,
        size = Size(width = Dimension.fromPoints(first), height = Dimension.fromPoints(second)),
        gridTemplateColumns = Array(third) { TrackSizingFunction.fromFlex(1f) }.asList(),
        gridTemplateRows = Array(fourth) { TrackSizingFunction.fromFlex(1f) }.asList()
    )
}

fun Quadruple<GridPlacement, GridPlacement, GridPlacement, GridPlacement>.intoGridChild(): Style {
    return Style(
        display = Display.GRID,
        gridColumn = Line(start = first, end = second),
        gridRow = Line(start = third, end = fourth)
    )
}

fun Quadruple<Short, Short, Short, Short>.intoOz(): Quadruple<OriginZeroLine, OriginZeroLine, OriginZeroLine, OriginZeroLine> {
    return Quadruple(OriginZeroLine(first), OriginZeroLine(second), OriginZeroLine(third), OriginZeroLine(fourth))
}
