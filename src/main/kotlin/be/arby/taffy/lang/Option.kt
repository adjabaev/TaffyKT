package be.arby.taffy.lang

sealed class Option<out T>: Cloneable {
    data class Some<out T>(val value: T) : Option<T>() {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is Some<*> -> value == other.value
                is None -> false
                else -> false
            }
        }

        override fun hashCode(): Int {
            return value?.hashCode() ?: 0
        }
    }
    object None : Option<Nothing>()

    fun isSome(): Boolean = this is Some
    fun isNone(): Boolean = this is None

    fun unwrap(): T {
        return when (this) {
            is Some -> value
            is None -> throw NoSuchElementException("Called unwrap on a None value")
        }
    }

    fun unwrapOr(default: @UnsafeVariance T): T {
        return when (this) {
            is Some -> value
            is None -> default
        }
    }

    fun unwrapOrElse(default: () -> @UnsafeVariance T): T {
        return when (this) {
            is Some -> value
            is None -> default()
        }
    }

    fun or(option: Option<@UnsafeVariance T>): Option<T> {
        return when (this) {
            is Some -> this
            is None -> option
        }
    }

    fun orElse(default: () -> Option<@UnsafeVariance T>): Option<T> {
        return when (this) {
            is Some -> this
            is None -> default()
        }
    }

    fun <U> map(f: (T) -> U): Option<U> {
        return when (this) {
            is Some -> from(f(value))
            is None -> None
        }
    }

    /**
     * Creates a copy of current object
     */
    fun copy(): Option<T> {
        return when (this) {
            is Some -> from(value)
            is None -> None
        }
    }

    /**
     * Apply a function to the value if it is present, otherwise return None
     */
    fun filter(predicate: (T) -> Boolean): Option<T> {
        return when (this) {
            is Some -> if (predicate(value)) this else None
            is None -> None
        }
    }

    override fun equals(other: Any?): Boolean {
        return when (this) {
            is Some -> when (other) {
                is Some<*> -> value == other.value
                is None -> false
                else -> false
            }
            is None -> when (other) {
                is Some<*> -> false
                is None -> true
                else -> false
            }
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    public override fun clone(): Option<T> {
        return when (this) {
            is Some -> Some(value)
            is None -> None
        }
    }

    companion object {
        @JvmStatic
        fun <T> from(value: @UnsafeVariance T?): Option<T> {
            return if (value == null) None else Some(value)
        }
    }
}

operator fun <T: Comparable<T>> Option<T>.compareTo(other: Option<T>): Int {
    return when (this) {
        is Option.Some -> when (other) {
            is Option.Some -> value.compareTo(other.value)
            is Option.None -> 1
        }
        is Option.None -> when (other) {
            is Option.Some -> -1
            is Option.None -> 0
        }
    }
}

operator fun <T: Comparable<T>> Option<T>.compareTo(other: T): Int {
    return when (this) {
        is Option.Some -> value.compareTo(other)
        is Option.None -> -1
    }
}
