package be.arby.taffy.lang

import java.lang.Exception

sealed class Result<T> {
    data class Ok<T>(val value: T) : Result<T>()

    data class Err<T>(val exception: Exception) : Result<T>() {
        init {
            throw exception
        }
    }

    fun unwrap(): T {
        return when (this) {
            is Ok -> value
            is Err -> throw exception
        }
    }
}
