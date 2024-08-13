package be.arby.taffy.lang.tuples

data class T2<out A, out B>(
    val first: A,
    val second: B
) {
    override fun toString(): String = "($first, $second)"
}
