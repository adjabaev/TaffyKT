package be.arby.taffy.geom

/**
 * An abstract "line". Represents any type that has a start and an end
 */
data class Line<T>(
    /**
     * The start position of a line
     */
    var start: T,
    /**
     * The end position of a line
     */
    var end : T
) {
    /**
     * Applies the function `f` to both the width and height
     *
     * This is used to transform a `Line<T>` into a `Line<R>`.
     */
    fun <R> map(f: (T) -> R): Line<R> {
        return Line(
            start = f(start),
            end = f(end)
        )
    }

    /**
     * Adds the start and end values together and returns the result
     */
    fun Line<Float>.sum(): Float {
        return start + end
    }

    companion object {
        /**
         * A `Line<bool>` with both start and end set to `true`
         */
        val TRUE = Line(start = true, end = true)
        /**
         * A `Line<bool>` with both start and end set to `false`
         */
        val FALSE = Line(start = false, end = false)
    }
}
