package be.arby.taffy.geometry

import be.arby.taffy.lang.Option

data class Point<T>(var x: T, var y: T) {
    companion object {
        fun zeroF(): Point<Float> {
            return Point(x = 0f, y = 0f)
        }

        val ZERO: Point<Float> = Point(x = 0.0f, y = 0.0f)
        val NONE: Point<Option<Float>> = Point(x = Option.None, y = Option.None)
    }
}
