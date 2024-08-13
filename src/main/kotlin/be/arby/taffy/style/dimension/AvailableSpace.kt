package be.arby.taffy.style.dimension

import be.arby.taffy.lang.DoubleFrom
import be.arby.taffy.lang.Option

/**
 * The amount of space available to a node in a given axis
 * <https://www.w3.org/TR/css-sizing-3/#available>
 */
sealed class AvailableSpace {
    /**
     * The amount of space available is the specified number of pixels
     */
    data class Definite(val availableSpace: Float) : AvailableSpace()

    /**
     * The amount of space available is indefinite and the node should be laid out under a min-content constraint
     */
    data object MinContent : AvailableSpace()

    /**
     * The amount of space available is indefinite and the node should be laid out under a max-content constraint
     */
    data object MaxContent : AvailableSpace()

    /**
     * Returns true for definite values, else false
     */
    fun isDefinite(): Boolean {
        return this is Definite
    }

    /**
     * Convert to Option
     * Definite values become Some(value). Constraints become None.
     */
    fun intoOption(): Option<Float> {
        return when (this) {
            is Definite -> Option.Some(availableSpace)
            else -> Option.None
        }
    }

    /**
     * Return the definite value or a default value
     */
    fun unwrapOr(default: Float): Float {
        return intoOption().unwrapOr(default)
    }

    /**
     * Return the definite value. Panic is the value is not definite.
     */
    fun unwrap(): Float {
        return intoOption().unwrap()
    }

    /**
     * Return self if definite or a default value
     */
    fun or(default: AvailableSpace): AvailableSpace {
        return when (this) {
            is Definite -> this
            else -> default
        }
    }

    /**
     * Return self if definite or a the result of the default value callback
     */
    fun orElse(defaultCb: () -> AvailableSpace): AvailableSpace {
        return when (this) {
            is Definite -> this
            else -> defaultCb()
        }
    }

    /**
     * Return the definite value or the result of the default value callback
     */
    fun unwrapOrElse(defaultCb: () -> Float): Float {
        return intoOption().unwrapOrElse(defaultCb)
    }

    /**
     * If passed value is Some then return AvailableSpace::Definite containing that value, else return self
     */
    fun maybeSet(value: Option<Float>): AvailableSpace {
        return when (value) {
            is Option.Some -> Definite(value.unwrap())
            is Option.None -> this
        }
    }

    /**
     * If passed value is Some then return AvailableSpace::Definite containing that value, else return self
     */
    fun mapDefiniteValue(mapFunction: (Float) -> Float): AvailableSpace {
        return when (this) {
            is Definite -> Definite(mapFunction(this.availableSpace))
            else -> this
        }
    }

    /**
     * Compute free_space given the passed used_space
     */
    fun computeFreeSpace(usedSpace: Float): Float {
        return when (this) {
            is MaxContent -> Float.POSITIVE_INFINITY
            is MinContent -> 0.0f
            is Definite -> this.availableSpace - usedSpace
        }
    }

    /**
     * Compare equality with another AvailableSpace, treating definite values
     * that are within f32::EPSILON of each other as equal
     */
    fun isRoughlyEqual(other: AvailableSpace): Boolean {
        return if (this is Definite && other is Definite) {
            this.availableSpace.equals(other.availableSpace)
        } else ((this is MaxContent && other is MaxContent) || (this is MinContent && other is MinContent))
    }

    companion object: DoubleFrom<Float, Option<Float>, AvailableSpace> {
        val ZERO = Definite(0f)
        val MAX_CONTENT = MaxContent
        val MIN_CONTENT = MinContent

        fun fromLength(value: Float): AvailableSpace {
            return Definite(value)
        }

        override fun from1(value: Float): AvailableSpace {
            return Definite(value)
        }

        override fun from2(value: Option<Float>): AvailableSpace {
            return when (value) {
                is Option.Some -> Definite(value.unwrap())
                is Option.None -> MaxContent
            }
        }
    }
}
