package be.arby.taffy.lang.collections

import be.arby.taffy.lang.Option
import kotlin.collections.ArrayList

class Grid<T>(val width: Int, val height: Int, private val cells: MutableList<T>) {
    val area: Int get() = width * height

    operator fun get(y: Int, x: Int): Option<T> {
        return if (x < width && y < height) {
            Option.Some(cells[y * width + x])
        } else {
            Option.None
        }
    }

    operator fun set(y: Int, x: Int, value: T) {
        if (x < width && y < height) {
            val index = y * width + x
            cells[index] = value
        }
    }

    fun iterRow(row: Int): List<Option<T>> {
        val arr = ArrayList<Option<T>>()
        for (x in 0 until width) {
            arr.add(get(row, x))
        }
        return arr
    }

    fun iterCol(column: Int): List<Option<T>> {
        val arr = ArrayList<Option<T>>()
        for (y in 0 until height) {
            arr.add(get(y, column))
        }
        return arr
    }

    companion object {
        fun <T> make(height: Int, width: Int, sup: () -> T): Grid<T> {
            val g = Grid(width = width, height = height, ArrayList<T>())
            for (i in 0 until (width * height)) {
                g.cells.add(sup.invoke())
            }
            return g
        }

        fun <T> fromList(list: List<T>, width: Int): Grid<T> {
            require(list.size % width == 0) { "The list size must be a multiple of the number of columns." }
            val height = list.size / width
            return Grid(width, height, list.toMutableList())
        }
    }
}
