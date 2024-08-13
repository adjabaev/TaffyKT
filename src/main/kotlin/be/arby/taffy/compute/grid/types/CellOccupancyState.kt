package be.arby.taffy.compute.grid.types

/**
 * The occupancy state of a single grid cell
 */
enum class CellOccupancyState {
    /**
     * Indicates that a grid cell is unoccupied
     */
    UNOCCUPIED,

    /**
     * Indicates that a grid cell is occupied by a definitely placed item
     */
    DEFINITELY_PLACED,

    /**
     * Indicates that a grid cell is occupied by an item that was placed by the auto placement algorithm
     */
    AUTOPLACED
}
