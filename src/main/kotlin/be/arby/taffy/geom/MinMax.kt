package be.arby.taffy.geom

/**
 * Generic struct which holds a "min" value and a "max" value
 */
open class MinMax<N, X>(
    /**
     * The value representing the minimum
     */
    var min: N,
    /**
     * The value representing the maximum
     */
    var max: X
)
