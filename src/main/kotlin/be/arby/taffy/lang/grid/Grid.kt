package be.arby.taffy.lang.grid

import be.arby.taffy.lang.Option
import be.arby.taffy.vec

data class Grid<T>(
    val data: MutableList<T>,
    var cols: Int,
    var rows: Int
) {
    /**
     * Access a certain element in the grid. Returns [Option.None] if an element beyond the grid bounds is tried to be accessed.
     */
    operator fun get(row: Int, col: Int): Option<T> {
        return if (col < cols && row < rows) {
            Option.Some(data[row * cols + col])
        } else {
            Option.None
        }
    }

    operator fun set(row: Int, col: Int, value: T) {
        if (col < cols && row < rows) {
            val index = row * cols + col
            data[index] = value
        }
    }

    /**
     * Returns an iterator over a row.
     */
    fun iterRow(row: Int): List<T> {
        val arr = vec<T>()
        for (col in 0 until cols) {
            arr.add(get(row, col).unwrap())
        }
        return arr
    }

    /**
     * Returns an iterator over a column.
     */
    fun iterCol(col: Int): List<T> {
        val arr = vec<T>()
        for (row in 0 until rows) {
            arr.add(get(row, col).unwrap())
        }
        return arr
    }

    companion object {
        fun <T> new(rows: Int, cols: Int, default: T): Grid<T> {
            require(rows >= 0) { "Rows must be greater than or equal to 0" }
            require(cols >= 0) { "Columns must be greater than or equal to 0" }

            return Grid(
                data = MutableList(rows * cols) { default },
                rows = rows,
                cols = cols
            )
        }

        fun <T> fromList(list: List<T>, cols: Int): Grid<T> {
            require(cols >= 0) { "Columns must be greater than or equal to 0" }
            require(list.isNotEmpty()) { "List must not be empty" }
            require(list.size % cols == 0) { "List size must be a multiple of the number of columns" }

            return Grid(
                data = list.toMutableList(),
                rows = list.size / cols,
                cols = cols
            )
        }
    }
}
