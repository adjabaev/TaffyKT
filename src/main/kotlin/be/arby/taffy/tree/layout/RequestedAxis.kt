package be.arby.taffy.tree.layout

import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.lang.From

/**
 * An axis that layout algorithms can be requested to compute a size for
 */
enum class RequestedAxis {
    /**
     * The horizontal axis
     */
    HORIZONTAL,

    /**
     * The vertical axis
     */
    VERTICAL,

    /**
     * Both axes
     */
    BOTH;

    companion object: From<AbsoluteAxis, RequestedAxis> {
        override fun from(value: AbsoluteAxis): RequestedAxis {
            return when (value) {
                AbsoluteAxis.HORIZONTAL -> HORIZONTAL
                AbsoluteAxis.VERTICAL -> VERTICAL
            }
        }
    }
}
