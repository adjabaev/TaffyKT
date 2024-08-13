package be.arby.taffy.lang.tuples

data class T3<out A, out B, out C>(
    val first: A,
    val second: B,
    val third: C
) {
    override fun toString(): String = "($first, $second, $third)"
}
