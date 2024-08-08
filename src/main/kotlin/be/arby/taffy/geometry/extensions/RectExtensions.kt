package be.arby.taffy.geometry.extensions

import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.maths.axis.AbsoluteAxis
import be.arby.taffy.style.flex.FlexDirection

fun Rect<Float>.horizontalAxisSum(): Float {
    return left + right
}

fun Rect<Float>.verticalAxisSum(): Float {
    return top + bottom
}

operator fun Rect<Float>.plus(rhs: Rect<Float>): Rect<Float> {
    return Rect(
        left = left + rhs.left,
        right = right + rhs.right,
        top = top + rhs.top,
        bottom = bottom + rhs.bottom,
    )
}

fun Rect<Float>.sumAxes(): Size<Float> {
    return Size(horizontalAxisSum(), verticalAxisSum())
}

fun Rect<Float>.mainAxisSum(direction: FlexDirection): Float {
    return if (direction.isRow()) horizontalAxisSum() else verticalAxisSum()
}

fun Rect<Float>.crossAxisSum(direction: FlexDirection): Float {
    return if (direction.isRow()) verticalAxisSum() else horizontalAxisSum()
}

fun Rect<Float>.gridAxisSum(axis: AbsoluteAxis): Float {
    return when (axis) {
        AbsoluteAxis.HORIZONTAL -> left + right
        AbsoluteAxis.VERTICAL -> top + bottom
    }
}
