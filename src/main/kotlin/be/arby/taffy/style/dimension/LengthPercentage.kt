package be.arby.taffy.style.dimension

import be.arby.taffy.lang.Option

sealed class LengthPercentage {
    /**
     * An absolute length in some abstract units. Users of Taffy may define what they correspond
     * to in their application (pixels, logical pixels, mm, etc) as they see fit.
     */
    data class Length(val f: Float = 0f) : LengthPercentage()

    /**
     * The dimension is stored in percentage relative to the parent item.
     */
    data class Percent(val f: Float = 0f) : LengthPercentage()

    fun maybeResolve(context: Option<Float>): Option<Float> {
        return when (this) {
            is Length -> Option.Some(this.f)
            is Percent -> context.map { dim -> dim * this.f }
        }
    }

    fun maybeResolve(context: Float): Option<Float> {
        return maybeResolve(Option.Some(context))
    }

    /**
     * Will return a default value of result is evaluated to `None`
     */
    fun resolveOrZero(context: Option<Float>): Float {
        return maybeResolve(context).unwrapOr(0f)
    }

    companion object {
        val ZERO: LengthPercentage = Length(0f)

        fun fromLength(points: Float): Length {
            return Length(points)
        }

        fun fromPercent(percent: Float): Percent {
            return Percent(percent)
        }
    }
}
