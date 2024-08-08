package be.arby.taffy.maths

import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.utils.max
import be.arby.taffy.utils.min

fun Float.min(other: Float): Float {
    return min(this, other)
}

fun Float.max(other: Float): Float {
    return max(this, other)
}

fun Float.into(): Option<Float> {
    return Option.Some(this)
}

fun Float.intoAS(): AvailableSpace {
    return AvailableSpace.Definite(this)
}

fun Option<Float>.intoAS(): AvailableSpace {
    return if (isSome()) AvailableSpace.Definite(unwrap()) else AvailableSpace.MaxContent
}

fun Float.isNormal(): Boolean {
    return this.isFinite() && this != 0f
}

fun Int.saturatingSub(other: Int): Int {
    return this - other.coerceIn(Int.MIN_VALUE, Int.MAX_VALUE)
}
