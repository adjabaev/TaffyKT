package be.arby.taffy.tree.layout

import be.arby.taffy.lang.From
import be.arby.taffy.lang.Result
import be.arby.taffy.lang.TryFrom
import be.arby.taffy.maths.axis.AbsoluteAxis

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
