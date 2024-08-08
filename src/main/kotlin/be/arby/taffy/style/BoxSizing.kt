package be.arby.taffy.style

import be.arby.taffy.lang.Default

/**
 * Specifies whether size styles for this node are assigned to the node's "content box" or "border box"
 *
 *  - The "content box" is the node's inner size excluding padding, border and margin
 *  - The "border box" is the node's outer size including padding and border (but still excluding margin)
 *
 * This property modifies the application of the following styles:
 *
 *   - `size`
 *   - `min_size`
 *   - `max_size`
 *   - `flex_basis`
 *
 * See <https://developer.mozilla.org/en-US/docs/Web/CSS/box-sizing>
 */
enum class BoxSizing {
    /**
     * Size styles such size, min_size, max_size specify the box's "content box" (the size excluding padding/border/margin)
     */
    BORDER_BOX,

    /**
     * Size styles such size, min_size, max_size specify the box's "border box" (the size excluding margin but including padding/border)
     */
    CONTENT_BOX;

    companion object: Default<BoxSizing> {
        override fun default(): BoxSizing {
            return BORDER_BOX
        }
    }
}
