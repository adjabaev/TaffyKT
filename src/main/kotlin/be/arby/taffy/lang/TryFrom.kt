package be.arby.taffy.lang

interface TryFrom<I, O> {
    fun tryFrom(value: I): Result<O>
}
