package be.arby.taffy.compute.grid.types

/**
 * Whether a GridTrack represents an actual track or a gutter.
 */
enum class GridTrackKind {
    /**
     * Track is an actual track
     */
    TRACK,
    /**
     * Track is a gutter (aka grid line) (aka gap)
     */
    GUTTER
}
