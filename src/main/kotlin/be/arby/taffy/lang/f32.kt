package be.arby.taffy.lang

fun Float.max(rhs: Float): Float {
    return coerceAtLeast(rhs)
}

fun Float.min(rhs: Float): Float {
    return coerceAtMost(rhs)
}

fun f32Max(a: Float, b: Float): Float {
    return a.max(b)
}

fun f32Min(a: Float, b: Float): Float {
    return a.min(b)
}
