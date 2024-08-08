package be.arby.taffy.geometry.extensions

import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.layout.SizeAndBaselines
import be.arby.taffy.style.dimension.AvailableSpace
import java.util.*
import be.arby.taffy.utils.f32Max as f32MaxFF

fun Size<Float>.add(rhs: Size<Float>): Size<Float> {
    return Size(width + rhs.width, height + rhs.height)
}

fun <T> Size<Option<T>>.unwrapOr(alt: Size<T>): Size<T> {
    return Size(width = width.unwrapOr(alt.width), height = height.unwrapOr(alt.height))
}

fun <T> Size<Option<T>>.or(alt: Size<Option<T>>): Size<Option<T>> {
    return Size(width = width.orElse { alt.width }, height = height.orElse { alt.height })
}

fun Size<AvailableSpace>.intoOptions(): Size<Option<Float>> {
    return Size(width = width.intoOption(), height = height.intoOption())
}

fun Size<Option<Float>>.bothAxisDefined(): Boolean {
    return width.isSome() && height.isSome()
}

fun Size<Float>.intoSB(): SizeAndBaselines {
    return SizeAndBaselines(size = this, firstBaselines = Point.NONE)
}

fun Size<AvailableSpace>.maybeSet(value: Size<Option<Float>>): Size<AvailableSpace> {
    return Size(width = width.maybeSet(value.width), height = height.maybeSet(value.height))
}

fun Size<Float>.f32Max(rhs: Size<Float>): Size<Float> {
    return Size(width= f32MaxFF(width, rhs.width), height = f32MaxFF(height, rhs.height))
}

operator fun Size<Float>.plus(rhs: Size<Float>): Size<Float> {
    return Size(width = width + rhs.width, height = height + rhs.height)
}

/**
 * Applies aspectRatio (if one is supplied) to the Size:
 *   * If width is `Some` but height is `None`, then height is computed from width and aspect_ratio
 *   * If height is `Some` but width is `None`, then width is computed from height and aspect_ratio
 *
 * If aspectRatio is `None` then this function simply returns this.
 */
fun Size<Option<Float>>.maybeApplyAspectRatio(aspectRatio: Option<Float>): Size<Option<Float>> {
    return when {
        aspectRatio.isSome() -> {
            when {
                width.isSome() && height.isNone() -> {
                    return Size(width = width, height = Option.Some(width.unwrap() / aspectRatio.unwrap()))
                }

                height.isSome() && width.isNone() -> {
                    return Size(width = Option.Some(height.unwrap() * aspectRatio.unwrap()), height = height)
                }

                else -> this
            }
        }

        else -> this
    }
}
