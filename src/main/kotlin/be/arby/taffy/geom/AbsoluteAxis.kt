package be.arby.taffy.geom

import be.arby.taffy.lang.Result
import be.arby.taffy.lang.TryFrom
import be.arby.taffy.tree.layout.RequestedAxis

/**
 * The simple absolute horizontal and vertical axis
 */
enum class AbsoluteAxis {
    /**
     * The horizontal axis
     */
    HORIZONTAL,

    /**
     * The vertical axis
     */
    VERTICAL;

    /**
     * Returns the other variant of the enum
     */
    fun otherAxis(): AbsoluteAxis {
        return when(this) {
            HORIZONTAL -> VERTICAL
            VERTICAL -> HORIZONTAL
        }
    }

    companion object: TryFrom<RequestedAxis, AbsoluteAxis> {
        override fun tryFrom(value: RequestedAxis): Result<AbsoluteAxis> {
            return when (value) {
                RequestedAxis.HORIZONTAL -> Result.Ok(HORIZONTAL)
                RequestedAxis.VERTICAL -> Result.Ok(VERTICAL)
                RequestedAxis.BOTH -> Result.Err(Exception("Cannot convert BOTH to an AbsoluteAxis"))
            }
        }
    }
}
