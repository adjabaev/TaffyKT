package be.arby.taffy.lang

interface From<I, O> {
    fun from(value: I): O
}

interface DoubleFrom<I1, I2, O> {
    fun from(value: I1): O

    fun from(value: I2): O
}
