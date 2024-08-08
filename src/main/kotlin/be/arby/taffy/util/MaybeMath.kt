package be.arby.taffy.util

import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.maths.max
import be.arby.taffy.maths.min
import be.arby.taffy.style.dimension.AvailableSpace

/// Option<Float> -> Option<Float> on Option<Float>

fun Option<Float>.maybeMin(rhs: Option<Float>): Option<Float> {
    return when (this) {
        is Option.Some -> when (rhs) {
            is Option.Some -> Option.Some(value.min(rhs.value))
            is Option.None -> this
        }
        is Option.None -> Option.None
    }
}

fun Option<Float>.maybeMax(rhs: Option<Float>): Option<Float> {
    return when (this) {
        is Option.Some -> when (rhs) {
            is Option.Some -> Option.Some(value.max(rhs.value))
            is Option.None -> this
        }
        is Option.None -> Option.None
    }
}

fun Option<Float>.maybeClamp(min: Option<Float>, max: Option<Float>): Option<Float> {
    return when (this) {
        is Option.Some -> when (min) {
            is Option.Some -> when (max) {
                is Option.Some -> Option.Some(value.min(max.value).max(min.value))
                is Option.None -> Option.Some(value.max(min.value))
            }
            is Option.None -> when (max) {
                is Option.Some -> Option.Some(value.min(max.value))
                is Option.None -> this
            }
        }
        is Option.None -> Option.None
    }
}

fun Option<Float>.maybeAdd(rhs: Option<Float>): Option<Float> {
    return when (this) {
        is Option.Some -> when (rhs) {
            is Option.Some -> Option.Some(value + rhs.value)
            is Option.None -> this
        }
        is Option.None -> Option.None
    }
}

fun Option<Float>.maybeSub(rhs: Option<Float>): Option<Float> {
    return when (this) {
        is Option.Some -> when (rhs) {
            is Option.Some -> Option.Some(value - rhs.value)
            is Option.None -> this
        }
        is Option.None -> Option.None
    }
}

/// Float -> Option<Float> on Option<Float>

fun Option<Float>.maybeMin(rhs: Float): Option<Float> {
    return map { value -> value.min(rhs) }
}

fun Option<Float>.maybeMax(rhs: Float): Option<Float> {
    return map { value -> value.max(rhs) }
}

fun Option<Float>.maybeClamp(min: Float, max: Float): Option<Float> {
    return map { value -> value.min(max).max(min) }
}

fun Option<Float>.maybeAdd(rhs: Float): Option<Float> {
    return map { value -> value + rhs }
}

fun Option<Float>.maybeSub(rhs: Float): Option<Float> {
    return map { value -> value - rhs }
}

/// Option<Float> -> Float on Float

fun Float.maybeMin(rhs: Option<Float>): Float {
    return when (rhs) {
        is Option.Some -> this.min(rhs.value)
        is Option.None -> this
    }
}

fun Float.maybeMax(rhs: Option<Float>): Float {
    return when (rhs) {
        is Option.Some -> this.max(rhs.value)
        is Option.None -> this
    }
}

fun Float.maybeClamp(min: Option<Float>, max: Option<Float>): Float {
    return when (min) {
        is Option.Some -> when (max) {
            is Option.Some -> this.min(max.value).max(min.value)
            is Option.None -> this.max(min.value)
        }
        is Option.None -> when (max) {
            is Option.Some -> this.min(max.value)
            is Option.None -> this
        }
    }
}

fun Float.maybeAdd(rhs: Option<Float>): Float {
    return when (rhs) {
        is Option.Some -> this + rhs.value
        is Option.None -> this
    }
}

fun Float.maybeSub(rhs: Option<Float>): Float {
    return when (rhs) {
        is Option.Some -> this - rhs.value
        is Option.None -> this
    }
}

/// Float -> AvailableSpace on AvailableSpace

fun AvailableSpace.maybeMin(rhs: Float): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> AvailableSpace.Definite(availableSpace.min(rhs))
        is AvailableSpace.MinContent -> AvailableSpace.Definite(rhs)
        is AvailableSpace.MaxContent -> AvailableSpace.Definite(rhs)
    }
}

fun AvailableSpace.maybeMax(rhs: Float): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> AvailableSpace.Definite(availableSpace.max(rhs))
        is AvailableSpace.MinContent -> AvailableSpace.MinContent
        is AvailableSpace.MaxContent -> AvailableSpace.MaxContent
    }
}

fun AvailableSpace.maybeClamp(min: Float, max: Float): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> AvailableSpace.Definite(availableSpace.min(max).max(min))
        is AvailableSpace.MinContent -> AvailableSpace.MinContent
        is AvailableSpace.MaxContent -> AvailableSpace.MaxContent
    }
}

fun AvailableSpace.maybeAdd(rhs: Float): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> AvailableSpace.Definite(availableSpace + rhs)
        is AvailableSpace.MinContent -> AvailableSpace.MinContent
        is AvailableSpace.MaxContent -> AvailableSpace.MaxContent
    }
}

fun AvailableSpace.maybeSub(rhs: Float): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> AvailableSpace.Definite(availableSpace - rhs)
        is AvailableSpace.MinContent -> AvailableSpace.MinContent
        is AvailableSpace.MaxContent -> AvailableSpace.MaxContent
    }
}

/// Option<Float> -> AvailableSpace on AvailableSpace

fun AvailableSpace.maybeMin(rhs: Option<Float>): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> when (rhs) {
            is Option.Some -> AvailableSpace.Definite(availableSpace.min(rhs.value))
            is Option.None -> AvailableSpace.Definite(availableSpace)
        }
        is AvailableSpace.MinContent -> when (rhs) {
            is Option.Some -> AvailableSpace.Definite(rhs.value)
            is Option.None -> AvailableSpace.MinContent
        }
        is AvailableSpace.MaxContent -> when (rhs) {
            is Option.Some -> AvailableSpace.Definite(rhs.value)
            is Option.None -> AvailableSpace.MaxContent
        }
    }
}

fun AvailableSpace.maybeMax(rhs: Option<Float>): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> when (rhs) {
            is Option.Some -> AvailableSpace.Definite(availableSpace.max(rhs.value))
            is Option.None -> AvailableSpace.Definite(availableSpace)
        }
        is AvailableSpace.MinContent -> AvailableSpace.MinContent
        is AvailableSpace.MaxContent -> AvailableSpace.MaxContent
    }
}

fun AvailableSpace.maybeClamp(min: Option<Float>, max: Option<Float>): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> when (min) {
            is Option.Some -> when (max) {
                is Option.Some -> AvailableSpace.Definite(availableSpace.min(max.value).max(min.value))
                is Option.None -> AvailableSpace.Definite(availableSpace.max(min.value))
            }
            is Option.None -> when (max) {
                is Option.Some -> AvailableSpace.Definite(availableSpace.min(max.value))
                is Option.None -> AvailableSpace.Definite(availableSpace)
            }
        }
        is AvailableSpace.MinContent -> AvailableSpace.MinContent
        is AvailableSpace.MaxContent -> AvailableSpace.MaxContent
    }
}

fun AvailableSpace.maybeAdd(rhs: Option<Float>): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> when (rhs) {
            is Option.Some -> AvailableSpace.Definite(availableSpace + rhs.value)
            is Option.None -> AvailableSpace.Definite(availableSpace)
        }
        is AvailableSpace.MinContent -> AvailableSpace.MinContent
        is AvailableSpace.MaxContent -> AvailableSpace.MaxContent
    }
}

fun AvailableSpace.maybeSub(rhs: Option<Float>): AvailableSpace {
    return when (this) {
        is AvailableSpace.Definite -> when (rhs) {
            is Option.Some -> AvailableSpace.Definite(availableSpace - rhs.value)
            is Option.None -> AvailableSpace.Definite(availableSpace)
        }
        is AvailableSpace.MinContent -> AvailableSpace.MinContent
        is AvailableSpace.MaxContent -> AvailableSpace.MaxContent
    }
}

/// Size<Option<Float>> -> Size<Option<Float>> on Size<Option<Float>>

@JvmName("SofSof_Min")
fun Size<Option<Float>>.maybeMin(rhs: Size<Option<Float>>): Size<Option<Float>> {
    return Size(width = width.maybeMin(rhs.width), height = height.maybeMin(rhs.height))
}

@JvmName("SofSof_Max")
fun Size<Option<Float>>.maybeMax(rhs: Size<Option<Float>>): Size<Option<Float>> {
    return Size(width = width.maybeMax(rhs.width), height = height.maybeMax(rhs.height))
}

@JvmName("SofSof_Clamp")
fun Size<Option<Float>>.maybeClamp(min: Size<Option<Float>>, max: Size<Option<Float>>): Size<Option<Float>> {
    return Size(
        width = width.maybeClamp(min.width, max.width),
        height = height.maybeClamp(min.height, max.height)
    )
}

@JvmName("SofSof_Add")
fun Size<Option<Float>>.maybeAdd(rhs: Size<Option<Float>>): Size<Option<Float>> {
    return Size(width = width.maybeAdd(rhs.width), height = height.maybeAdd(rhs.height))
}

@JvmName("SofSof_Sub")
fun Size<Option<Float>>.maybeSub(rhs: Size<Option<Float>>): Size<Option<Float>> {
    return Size(width = width.maybeSub(rhs.width), height = height.maybeSub(rhs.height))
}

/// Size<Float> -> Size<Option<Float>> on Size<Option<Float>>

@JvmName("SfSof_Min")
fun Size<Option<Float>>.maybeMin(rhs: Size<Float>): Size<Option<Float>> {
    return Size(width = width.maybeMin(rhs.width), height = height.maybeMin(rhs.height))
}

@JvmName("SfSof_Max")
fun Size<Option<Float>>.maybeMax(rhs: Size<Float>): Size<Option<Float>> {
    return Size(width = width.maybeMax(rhs.width), height = height.maybeMax(rhs.height))
}

@JvmName("SfSof_Clamp")
fun Size<Option<Float>>.maybeClamp(min: Size<Float>, max: Size<Float>): Size<Option<Float>> {
    return Size(
        width = width.maybeClamp(min.width, max.width),
        height = height.maybeClamp(min.height, max.height)
    )
}

@JvmName("SfSof_Add")
fun Size<Option<Float>>.maybeAdd(rhs: Size<Float>): Size<Option<Float>> {
    return Size(width = width.maybeAdd(rhs.width), height = height.maybeAdd(rhs.height))
}

@JvmName("SfSof_Sub")
fun Size<Option<Float>>.maybeSub(rhs: Size<Float>): Size<Option<Float>> {
    return Size(width = width.maybeSub(rhs.width), height = height.maybeSub(rhs.height))
}

/// Size<Float> -> Size<Option<Float>> on Size<Float>

@JvmName("SofSf_Min")
fun Size<Float>.maybeMin(rhs: Size<Option<Float>>): Size<Float> {
    return Size(width = width.maybeMin(rhs.width), height = height.maybeMin(rhs.height))
}

@JvmName("SofSf_Max")
fun Size<Float>.maybeMax(rhs: Size<Option<Float>>): Size<Float> {
    return Size(width = width.maybeMax(rhs.width), height = height.maybeMax(rhs.height))
}

@JvmName("SofSf_Clamp")
fun Size<Float>.maybeClamp(min: Size<Option<Float>>, max: Size<Option<Float>>): Size<Float> {
    return Size(
        width = width.maybeClamp(min.width, max.width),
        height = height.maybeClamp(min.height, max.height)
    )
}

@JvmName("SofSf_Add")
fun Size<Float>.maybeAdd(rhs: Size<Option<Float>>): Size<Float> {
    return Size(width = width.maybeAdd(rhs.width), height = height.maybeAdd(rhs.height))
}

@JvmName("SofSf_Sub")
fun Size<Float>.maybeSub(rhs: Size<Option<Float>>): Size<Float> {
    return Size(width = width.maybeSub(rhs.width), height = height.maybeSub(rhs.height))
}

/// Size<Option<Float>> -> Size<AvailableSpace> on Size<AvailableSpace>

@JvmName("SofSas_Min")
fun Size<AvailableSpace>.maybeMin(rhs: Size<Option<Float>>): Size<AvailableSpace> {
    return Size(width = width.maybeMin(rhs.width), height = height.maybeMin(rhs.height))
}

@JvmName("SofSas_Max")
fun Size<AvailableSpace>.maybeMax(rhs: Size<Option<Float>>): Size<AvailableSpace> {
    return Size(width = width.maybeMax(rhs.width), height = height.maybeMax(rhs.height))
}

@JvmName("SofSas_Clamp")
fun Size<AvailableSpace>.maybeClamp(min: Size<Option<Float>>, max: Size<Option<Float>>): Size<AvailableSpace> {
    return Size(
        width = width.maybeClamp(min.width, max.width),
        height = height.maybeClamp(min.height, max.height)
    )
}

@JvmName("SofSas_Add")
fun Size<AvailableSpace>.maybeAdd(rhs: Size<Option<Float>>): Size<AvailableSpace> {
    return Size(width = width.maybeAdd(rhs.width), height = height.maybeAdd(rhs.height))
}

@JvmName("SofSas_Sub")
fun Size<AvailableSpace>.maybeSub(rhs: Size<Option<Float>>): Size<AvailableSpace> {
    return Size(width = width.maybeSub(rhs.width), height = height.maybeSub(rhs.height))
}
