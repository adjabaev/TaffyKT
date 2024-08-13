package be.arby.taffy.lang

interface TryFrom<I, O> {
    fun tryFrom(value: I): Result<O>
}

interface DoubleTryFrom<I1, I2, O> {
    fun tryFrom1(value: I1): Result<O>

    fun tryFrom2(value: I2): Result<O>
}
