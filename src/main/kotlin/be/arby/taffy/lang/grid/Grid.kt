package be.arby.taffy.lang.grid

import be.arby.taffy.lang.Option
import be.arby.taffy.lang.collections.*
import java.util.*
import kotlin.math.min

data class Grid<T>(
    val data: MutableList<T>,
    var cols: Int,
    var rows: Int,
    val order: Order
) {
    fun getIndex(row: Int, col: Int): Int {
        require(row in 0 until rows) { "Row index out of bounds" }
        require(col in 0 until cols) { "Column index out of bounds" }

        return when (order) {
            Order.ROW_MAJOR -> row * cols + col
            Order.COLUMN_MAJOR -> col * rows + row
        }
    }

    /**
     * Access a certain element in the grid. Returns None if an element beyond the grid bounds is tried to be accessed.
     */
    fun getUnchecked(row: Int, col: Int): T {
        require(row in 0 until rows) { "Row index out of bounds" }
        require(col in 0 until cols) { "Column index out of bounds" }

        return data[getIndex(row, col)]
    }

    /**
     * Access a certain element in the grid. Returns [Option.None] if an element beyond the grid bounds is tried to be accessed.
     */
    operator fun get(row: Int, col: Int): Option<T> {
        require(row in 0 until rows) { "Row index out of bounds" }
        require(col in 0 until cols) { "Column index out of bounds" }

        return Option.from(data[getIndex(row, col)])
    }

    operator fun set(row: Int, col: Int, value: T) {
        require(row in 0 until rows) { "Row index out of bounds" }
        require(col in 0 until cols) { "Column index out of bounds" }

        data[getIndex(row, col)] = value
    }

    /**
     * Returns the size of the grid as a two element tuple. First element are the number of rows and the second the columns.
     */
    fun size(): Pair<Int, Int> {
        return Pair(rows, cols)
    }

    /**
     * Returns the number of rows of the grid.
     */
    fun rows(): Int {
        return rows
    }

    /**
     * Returns the number of columns of the grid.
     */
    fun cols(): Int {
        return cols
    }

    /**
     * Returns the internal memory layout of the grid.
     */
    fun order(): Order {
        return order
    }

    /**
     * Returns true if the grid contains no elements. For example:
     */
    fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    /**
     * Clears the grid.
     *
     * This doesn't change the grid order.
     */
    fun clear() {
        rows = 0
        cols = 0
        data.clear()
    }

    /**
     * Returns an iterator over a column.
     */
    fun iter(): Iterator<T> {
        return data.iterator()
    }

    /**
     * Returns an iterator over a column.
     */
    fun iterCol(col: Int): List<T> {
        require(col in 0 until cols) { "Column index out of bounds" }
        return when (order) {
            Order.ROW_MAJOR -> {
                data.slice(col until data.size step cols)
            }

            Order.COLUMN_MAJOR -> {
                val start = col * rows
                data.slice(start until (start + rows) step 1)
            }
        }
    }

    /**
     * Returns an iterator over a row.
     */
    fun iterRow(row: Int): List<T> {
        require(row in 0 until rows) { "Row index out of bounds" }
        return when (order) {
            Order.ROW_MAJOR -> {
                val start = row * cols
                data.slice(start until (start + cols) step 1)
            }

            Order.COLUMN_MAJOR -> {
                data.slice(row until data.size step rows)
            }
        }
    }

    companion object {
        fun <T> new(rows: Int, cols: Int): Grid<T> {
            return newWithOrder(rows, cols, Order.ROW_MAJOR)
        }

        fun <T> newWithOrder(rows: Int, cols: Int, order: Order): Grid<T> {
            require(rows >= 0) { "Rows must be greater than or equal to 0" }
            require(cols >= 0) { "Columns must be greater than or equal to 0" }

            return Grid(
                data = mutableListOf(),
                rows = rows,
                cols = cols,
                order = order
            )
        }

        fun <T> init(rows: Int, cols: Int, data: T): Grid<T> {
            return initWithOrder(rows, cols, Order.ROW_MAJOR, data)
        }

        fun <T> initWithOrder(rows: Int, cols: Int, order: Order, data: T): Grid<T> {
            require(rows >= 0) { "Rows must be greater than or equal to 0" }
            require(cols >= 0) { "Columns must be greater than or equal to 0" }

            return Grid(
                data = MutableList(rows * cols) { data },
                rows = rows,
                cols = cols,
                order = order
            )
        }

        fun <T> fromList(list: List<T>, cols: Int): Grid<T> {
            return fromListWithOrder(list, cols, Order.ROW_MAJOR)
        }

        fun <T> fromListWithOrder(list: List<T>, cols: Int, order: Order): Grid<T> {
            require(cols >= 0) { "Columns must be greater than or equal to 0" }
            require(list.isNotEmpty()) { "List must not be empty" }
            require(list.size % cols == 0) { "List size must be a multiple of the number of columns" }

            val mutList = mutableListOf<T>()
            mutList.addAll(list)

            return Grid(
                data = mutList,
                rows = list.size / cols,
                cols = cols,
                order = order
            )
        }
    }
}
