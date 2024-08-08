package be.arby.taffy.utils

import be.arby.taffy.compute.grid.types.GridCoordinate

const val FLOAT_MIN: Float = -3.4028235E38F

fun min(a: Short, b: Short): Short {
    return if (a <= b) a else b
}

fun <T: GridCoordinate> min(a: T, b: T): T {
    return if (a.asShort() <= b.asShort()) a else b
}

fun <T: GridCoordinate> max(a: T, b: T): T {
    return if (a.asShort() >= b.asShort()) a else b
}

fun max(a: Short, b: Short): Short {
    return if (a >= b) a else b
}

fun min(a: Float, b: Float): Float {
    return if (a <= b) a else b
}

fun max(a: Float, b: Float): Float {
    return if (a >= b) a else b
}

fun f32Min(a: Float, b: Float): Float {
    val v = Ordering.totalCmp(a, b)
    return when {
        v > 0 -> b
        else -> a
    }
}

fun f32Max(a: Float, b: Float): Float {
    val v = Ordering.totalCmp(a, b)
    return when {
        v > 0 -> a
        else -> b
    }
}

fun Boolean.toInt(): Int = if (this) 1 else 0
