package be.arby.taffy.style.alignment

/**
 * Style types for controlling alignment
 *
 * Used to control how child nodes are aligned.
 * For Flexbox it controls alignment in the cross axis
 * For Grid it controls alignment in the block axis
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/CSS/align-items)
 */
enum class AlignItems {
    /**
     * Items are packed toward the start of the axis
     */
    START,

    /**
     * Items are packed toward the end of the axis
     */
    END,

    /**
     * Items are packed towards the flex-relative start of the axis.
     *
     *  For flex containers with flex_direction RowReverse or ColumnReverse this is equivalent
     *  to End. In all other cases it is equivalent to Start.
     */
    FLEX_START,

    /**
     * Items are packed towards the flex-relative end of the axis.
     *
     * For flex containers with flex_direction RowReverse or ColumnReverse this is equivalent
     * to Start. In all other cases it is equivalent to End.
     */
    FLEX_END,

    /**
     * Items are packed along the center of the cross axis
     */
    CENTER,

    /**
     * Items are aligned such as their baselines align
     */
    BASELINE,

    /**
     * Stretch to fill the container
     */
    STRETCH
}

/**
 * Used to control how child nodes are aligned.
 * Does not apply to Flexbox, and will be ignored if specified on a flex container
 * For Grid it controls alignment in the inline axis
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/CSS/justify-items)
 */
typealias JustifyItems = AlignItems
/**
 * Used to control how the specified nodes is aligned.
 * Overrides the parent Node's `AlignItems` property.
 * For Flexbox it controls alignment in the cross axis
 * For Grid it controls alignment in the block axis
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/CSS/align-self)
 */
typealias AlignSelf = AlignItems
/**
 * Used to control how the specified nodes is aligned.
 * Overrides the parent Node's `JustifyItems` property.
 * Does not apply to Flexbox, and will be ignored if specified on a flex child
 * For Grid it controls alignment in the inline axis
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/CSS/justify-self)
 */
typealias JustifySelf = AlignItems
