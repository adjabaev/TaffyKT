package be.arby.taffy.util

const val FLOAT_MIN: Float = -3.4028235E38F

fun min(a: Short, b: Short): Short {
    return if (a <= b) a else b
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

fun Boolean.toInt(): Int = if (this) 1 else 0
