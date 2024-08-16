package be.arby.taffy.tree.layout

import be.arby.taffy.lang.f32Max
import be.arby.taffy.lang.f32Min

/**
 * A set of margins that are available for collapsing with for block layout's margin collapsing
 */
data class CollapsibleMarginSet(
    /**
     * The largest positive margin
     */
    var positive: Float,
    /**
     * The smallest negative margin (with largest absolute value)
     */
    var negative: Float
): Cloneable {
    public override fun clone(): CollapsibleMarginSet {
        return CollapsibleMarginSet(positive = positive, negative = negative)
    }

    /**
     * Collapse a single margin with this set
     */
    fun collapseWithMargin(margin: Float): CollapsibleMarginSet {
        if (margin >= 0.0) {
            positive = f32Max(positive, margin)
        } else {
            negative = f32Min(negative, margin)
        }
        return this
    }

    /**
     * Collapse another margin set with this set
     */
    fun collapseWithSet(other: CollapsibleMarginSet): CollapsibleMarginSet {
        positive = f32Max(positive, other.positive)
        negative = f32Min(negative, other.negative)
        return this
    }

    /**
     * Resolve the resultant margin from this set once all collapsible margins
     * have been collapsed into it
     */
    fun resolve(): Float {
        return positive + negative
    }

    companion object {
        /**
         * A default margin set with no collapsible margins
         */
        val ZERO = CollapsibleMarginSet(positive = 0.0f, negative = 0.0f)

        /**
         * Create a set from a single margin
         */
        fun fromMargin(margin: Float): CollapsibleMarginSet {
            return if (margin >= 0.0) {
                CollapsibleMarginSet(positive = margin, negative = 0.0f)
            } else {
                CollapsibleMarginSet(positive = 0.0f, negative = margin)
            }
        }
    }
}
