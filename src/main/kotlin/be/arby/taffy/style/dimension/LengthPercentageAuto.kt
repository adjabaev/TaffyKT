package be.arby.taffy.style.dimension

import be.arby.taffy.lang.Option

/**
 * A unit of linear measurement
 *
 * This is commonly combined with [`Rect`], [`Point`](crate::geometry::Point) and [`Size<T>`].
 */
sealed class LengthPercentageAuto {
    /**
     * An absolute length in some abstract units. Users of Taffy may define what they correspond
     * to in their application (pixels, logical pixels, mm, etc) as they see fit.
     */
    data class Length(val f: Float) : LengthPercentageAuto()

    /**
     * The dimension is stored in percentage relative to the parent item.
     */
    data class Percent(val f: Float) : LengthPercentageAuto()

    /**
     * The dimension should be automatically computed
     */
    data object Auto : LengthPercentageAuto()

    /**
     * Returns:
     *   - Some(length) for Length variants
     *   - Some(resolved) using the provided context for Percent variants
     *   - None for Auto variants
     */
    fun resolveToOption(context: Float): Option<Float> {
        return when (this) {
            is Length -> Option.Some(this.f)
            is Percent -> Option.Some(context * this.f)
            is Auto -> Option.None
        }
    }

    fun maybeResolve(context: Option<Float>): Option<Float> {
        return when (this) {
            is Length -> Option.Some(this.f)
            is Percent -> context.map { dim -> dim * this.f }
            is Auto -> Option.None
        }
    }

    fun maybeResolve(context: Float): Option<Float> {
        return maybeResolve(Option.from(context))
    }

    /**
     * Will return a default value of result is evaluated to `None`
     */
    fun resolveOrZero(context: Option<Float>): Float {
        return maybeResolve(context).unwrapOr(0f)
    }

    /**
     * Returns true if value is LengthPercentageAuto::Auto
     */
    fun isAuto(): Boolean {
        return this is Auto
    }

    companion object {
        val ZERO = Length(0f)
        val AUTO = Auto

        fun fromLength(value: Float): Length {
            return Length(value)
        }

        fun fromPercent(percent: Float): Percent {
            return Percent(percent)
        }

        @JvmStatic
        fun from(input: LengthPercentage): LengthPercentageAuto {
            return when (input) {
                is LengthPercentage.Length -> Length(input.f)
                is LengthPercentage.Percent -> Percent(input.f)
            }
        }
    }
}
