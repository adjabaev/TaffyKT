package be.arby.taffy.lang

interface From<I, O> {
    fun from(value: I): O
}

interface DoubleFrom<I1, I2, O> {
    fun from1(value: I1): O

    fun from2(value: I2): O
}
