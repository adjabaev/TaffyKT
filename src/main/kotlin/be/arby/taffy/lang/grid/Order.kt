package be.arby.taffy.lang.grid

/**
 * Define the internal memory layout of the grid.
 */
enum class Order {
    /**
     * The data is ordered row by row.
     */
    ROW_MAJOR,

    /**
     * The data is ordered column by column.
     */
    COLUMN_MAJOR;

    fun counterpart(): Order {
        return when (this) {
            ROW_MAJOR -> COLUMN_MAJOR
            COLUMN_MAJOR -> ROW_MAJOR
        }
    }
}
