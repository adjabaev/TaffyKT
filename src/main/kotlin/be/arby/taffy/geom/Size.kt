package be.arby.taffy.geom

import be.arby.taffy.lang.Option
import be.arby.taffy.lang.f32Max
import be.arby.taffy.lang.f32Min
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.flex.FlexDirection

/**
 * The width and height of a [Rect]
 */
data class Size<T>(
    /**
     * The x extent of the rectangle
     */
    var width: T,
    /**
     * The y extent of the rectangle
     */
    var height: T
) {
    fun getAbs(axis: AbsoluteAxis): T {
        return when (axis) {
            AbsoluteAxis.HORIZONTAL -> width
            AbsoluteAxis.VERTICAL -> height
        }
    }

    /**
     * Made to make code resemble the original Rust code
     */
    fun t2(): Pair<T, T> {
        return Pair(width, height)
    }

    /**
     * Applies the function `f` to both the width and height
     *
     * This is used to transform a `Size<T>` into a `Size<R>`.
     */
    fun <R> map(f: (T) -> R): Size<R> {
        return Size(
            width = f(width), height = f(height)
        )
    }

    /**
     * Applies the function `f` to the width
     */
    fun mapWidth(f: (T) -> T): Size<T> {
        return Size(
            width = f(width), height = height
        )
    }

    /**
     * Applies the function `f` to the height
     */
    fun mapHeight(f: (T) -> T): Size<T> {
        return Size(
            width = width, height = f(height)
        )
    }

    /**
     * Applies the function `f` to both the width and height
     * of this value and another passed value
     */
    fun <U, R> zipMap(size: Size<U>, f: (T, U) -> R): Size<R> {
        return Size(
            width = f(width, size.width), height = f(height, size.height)
        )
    }

    /**
     * Sets the extent of the main layout axis
     *
     * Whether this is the width or height depends on the `direction` provided
     */
    fun setMain(direction: FlexDirection, value: T) {
        if (direction.isRow()) {
            width = value
        } else {
            height = value
        }
    }

    /**
     * Sets the extent of the cross layout axis
     *
     * Whether this is the width or height depends on the `direction` provided
     */
    fun setCross(direction: FlexDirection, value: T) {
        if (direction.isRow()) {
            height = value
        } else {
            width = value
        }
    }

    /**
     * Creates a new value of type Size<T> with the main axis set to value provided
     *
     * Whether this is the width or height depends on the `direction` provided
     */
    fun withMain(direction: FlexDirection, value: T): Size<T> {
        return if (direction.isRow()) {
            copy(width = value)
        } else {
            copy(height = value)
        }
    }

    /**
     * Creates a new value of type Self with the cross axis set to value provided
     *
     * Whether this is the width or height depends on the `direction` provided
     */
    fun withCross(direction: FlexDirection, value: T): Size<T> {
        return if (direction.isRow()) {
            copy(height = value)
        } else {
            copy(width = value)
        }
    }

    /**
     * Creates a new value of type Self with the main axis modified by the callback provided
     *
     * Whether this is the width or height depends on the `direction` provided
     */
    fun mapMain(direction: FlexDirection, mapper: (T) -> T): Size<T> {
        return if (direction.isRow()) {
            copy(width = mapper(width))
        } else {
            copy(height = mapper(height))
        }
    }

    /**
     * Creates a new value of type Self with the cross axis modified by the callback provided
     *
     * Whether this is the width or height depends on the `direction` provided
     */
    fun mapCross(direction: FlexDirection, mapper: (T) -> T): Size<T> {
        return if (direction.isRow()) {
            copy(height = mapper(height))
        } else {
            copy(width = mapper(width))
        }
    }

    /**
     * Gets the extent of the main layout axis
     *
     * Whether this is the width or height depends on the `direction` provided
     */
    fun main(direction: FlexDirection): T {
        return if (direction.isRow()) {
            width
        } else {
            height
        }
    }

    /**
     * Gets the extent of the cross layout axis
     *
     * Whether this is the width or height depends on the `direction` provided
     */
    fun cross(direction: FlexDirection): T {
        return if (direction.isRow()) {
            height
        } else {
            width
        }
    }

    /**
     * Gets the extent of the specified layout axis
     * Whether this is the width or height depends on the `GridAxis` provided
     */
    fun get(axis: AbstractAxis): T {
        return when (axis) {
            AbstractAxis.INLINE -> width
            AbstractAxis.BLOCK -> height
        }
    }

    /**
     * Sets the extent of the specified layout axis
     * Whether this is the width or height depends on the `GridAxis` provided
     */
    fun set(axis: AbstractAxis, value: T) {
        when (axis) {
            AbstractAxis.INLINE -> width = value
            AbstractAxis.BLOCK -> height = value
        }
    }

    companion object {
        /**
         * A [`Size`] with zero width and height
         */
        val ZERO = Size(width = 0f, height = 0f)

        /**
         * A [`Size`] with `None` width and height
         */
        val NONE: Size<Option<Float>> = Size(width = Option.None, height = Option.None)

        val MIN_CONTENT: Size<AvailableSpace> = Size(width = AvailableSpace.MIN_CONTENT, height = AvailableSpace.MIN_CONTENT)
        val MAX_CONTENT: Size<AvailableSpace> = Size(width = AvailableSpace.MAX_CONTENT, height = AvailableSpace.MAX_CONTENT)

        /**
         * A [`Size<Option<Float>>`] with `Some(width)` and `Some(height)` as parameters
         */
        fun new(width: Float, height: Float): Size<Option<Float>> {
            return Size(width = Option.Some(width), height = Option.Some(height))
        }

        fun autoD(): Size<Dimension> {
            return Size(
                width = Dimension.Auto,
                height = Dimension.Auto
            )
        }

        fun zeroF(): Size<Float> {
            return Size(
                width = 0f,
                height = 0f
            )
        }

        fun zeroOF(): Size<Option<Float>> {
            return Size(
                width = Option.Some(0f),
                height = Option.Some(0f)
            )
        }

        fun zeroLP(): Size<LengthPercentage> {
            return Size(
                width = LengthPercentage.Length(0f),
                height = LengthPercentage.Length(0f)
            )
        }

        /**
         * Creates a new [`Size<Option<f32>>`] with either the width or height set based on the provided `direction`
         */
        fun fromCross(direction: FlexDirection, value: Option<Float>): Size<Option<Float>> {
            val new = NONE.copy()
            if (direction.isRow()) {
                new.height = value
            } else {
                new.width = value
            }
            return new
        }

        /**
         * Generates a [`Size<Dimension>`] using [`Dimension::Length`] values
         */
        fun fromLengths(width: Float, height: Float): Size<Dimension> {
            return Size(width = Dimension.Length(width), height = Dimension.Length(height))
        }

        /**
         * Generates a [`Size<Dimension>`] using [`Dimension::Percent`] values
         */
        fun fromPercent(width: Float, height: Float): Size<Dimension> {
            return Size(width = Dimension.Percent(width), height = Dimension.Percent(height))
        }
    }
}

/// DIMENSION VARIANTS

@JvmName("maybeResolveDimensionSOF")
fun Size<Dimension>.maybeResolve(context: Size<Option<Float>>): Size<Option<Float>> {
    return Size(width = width.maybeResolve(context.width), height = height.maybeResolve(context.height))
}

@JvmName("maybeResolveDimensionSF")
fun Size<Dimension>.maybeResolve(context: Size<Float>): Size<Option<Float>> {
    return Size(width = width.maybeResolve(context.width), height = height.maybeResolve(context.height))
}

/// FLOAT VARIANTS

operator fun Size<Float>.plus(rhs: Size<Float>): Size<Float> {
    return Size(width + rhs.width, height + rhs.height)
}

operator fun Size<Float>.minus(rhs: Size<Float>): Size<Float> {
    return Size(width - rhs.width, height - rhs.height)
}

/**
 * Applies f32Max to each component separately
 */
fun Size<Float>.f32Max(rhs: Size<Float>): Size<Float> {
    return Size(width = f32Max(width, rhs.width), height = f32Max(height, rhs.height))
}

/**
 * Applies f32Min to each component separately
 */
fun Size<Float>.f32Min(rhs: Size<Float>): Size<Float> {
    return Size(width = f32Min(width, rhs.width), height = f32Min(height, rhs.height))
}

/**
 * Return true if both width and height are greater than 0 else false
 */
fun Size<Float>.hasNonZeroArea(): Boolean {
    return width > 0.0 && height > 0.0
}

/// OPTION FLOAT VARIANTS

/**
 * Applies aspect_ratio (if one is supplied) to the Size:
 *   - If width is `Some` but height is `None`, then height is computed from width and aspect_ratio
 *   - If height is `Some` but width is `None`, then width is computed from height and aspect_ratio
 *
 * If aspect_ratio is `None` then this function simply returns self.
 */
fun Size<Option<Float>>.maybeApplyAspectRatio(aspectRatio: Option<Float>): Size<Option<Float>> {
    return when (aspectRatio) {
        is Option.Some -> when (width) {
            is Option.Some -> when (height) {
                is Option.Some -> this
                is Option.None -> Size(
                    width = Option.Some(width.unwrap()),
                    height = Option.Some(width.unwrap() / aspectRatio.value)
                )
            }

            is Option.None -> when (height) {
                is Option.Some -> Size(
                    width = Option.Some(height.unwrap() * aspectRatio.value),
                    height = Option.Some(height.unwrap())
                )

                is Option.None -> this
            }
        }

        is Option.None -> this
    }
}

/**
 * Performs Option::unwrap_or on each component separately
 */
fun <T> Size<Option<T>>.unwrapOr(alt: Size<T>): Size<T> {
    return Size(width = width.unwrapOr(alt.width), height = height.unwrapOr(alt.height))
}

/**
 * Performs Option::or on each component separately
 */
fun <T> Size<Option<T>>.or(alt: Size<Option<T>>): Size<Option<T>> {
    return Size(width = width.or(alt.width), height = height.or(alt.height))
}

/**
 * Return true if both components are Some, else false.
 */
fun <T> Size<Option<T>>.bothAxisDefined(): Boolean {
    return width.isSome() && height.isSome()
}

/// LENGTH_PERCENTAGE VARIANTS

/**
 * Converts any `parent`-relative values for Rect into an absolute Rect
 */
fun Size<LengthPercentage>.resolveOrZero(context: Size<Option<Float>>): Size<Float> {
    return Size(
        width = width.resolveOrZero(context.width),
        height = height.resolveOrZero(context.height)
    )
}

/// AVAILABLE_SPACE VARIANTS

/**
 * Convert `Size<AvailableSpace>` into `Size<Option<f32>>`
 */
fun Size<AvailableSpace>.intoOptions(): Size<Option<Float>> {
    return Size(
        width = width.intoOption(),
        height = height.intoOption()
    )
}

/**
 * If passed value is Some then return AvailableSpace::Definite containing that value, else return self
 */
fun Size<AvailableSpace>.maybeSet(value: Size<Option<Float>>): Size<AvailableSpace> {
    return Size(
        width = width.maybeSet(value.width),
        height = height.maybeSet(value.height)
    )
}
