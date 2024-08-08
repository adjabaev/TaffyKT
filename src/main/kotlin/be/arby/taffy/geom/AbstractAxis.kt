package be.arby.taffy.geom

/**
 * The CSS abstract axis
 * <https://www.w3.org/TR/css-writing-modes-3/#abstract-axes>
 */
enum class AbstractAxis {
    /**
     * The axis in the inline dimension, i.e. the horizontal axis in horizontal writing modes and the vertical axis in vertical writing modes.
     */
    INLINE,

    /**
     * The axis in the block dimension, i.e. the vertical axis in horizontal writing modes and the horizontal axis in vertical writing modes.
     */
    BLOCK;

    /**
     * Returns the other variant of the enum
     */
    fun other(): AbstractAxis {
        return when(this) {
            INLINE -> BLOCK
            BLOCK -> INLINE
        }
    }

    /**
     * Convert an `AbstractAxis` into an `AbsoluteAxis` naively assuming that the Inline axis is Horizontal
     * This is currently always true, but will change if Taffy ever implements the `writing_mode` property
     */
    fun asAbsNaive(): AbsoluteAxis {
        return when (this) {
            INLINE -> AbsoluteAxis.HORIZONTAL
            BLOCK -> AbsoluteAxis.VERTICAL
        }
    }
}
