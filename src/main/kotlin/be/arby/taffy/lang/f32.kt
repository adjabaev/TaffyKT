package be.arby.taffy.lang

import kotlin.math.ceil
import kotlin.math.floor

fun Float.max(rhs: Float): Float {
    return coerceAtLeast(rhs)
}

fun Float.min(rhs: Float): Float {
    return coerceAtMost(rhs)
}

fun Float.floor(): Float {
    return floor(this)
}

fun f32Max(a: Float, b: Float): Float {
    return a.max(b)
}

fun f32Min(a: Float, b: Float): Float {
    return a.min(b)
}

fun round(value: Float): Float {
    return when {
        value > 0f -> floor(value + 0.5f)
        value < 0f -> ceil(value - 0.5f)
        else -> 0f
    }
}
