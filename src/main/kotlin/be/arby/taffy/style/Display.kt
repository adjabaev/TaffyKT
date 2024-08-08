package be.arby.taffy.style

import be.arby.taffy.lang.Default

/**
 * Sets the layout used for the children of this node
 *
 * The default values depends on on which feature flags are enabled. The order of precedence is: Flex, Grid, Block, None.
 */
enum class Display {
    /**
     * The children will follow the block layout algorithm
     */
    BLOCK,

    /**
     * The children will follow the flexbox layout algorithm
     */
    FLEX,

    /**
     * The children will follow the CSS Grid layout algorithm
     */
    GRID,

    /**
     * The node is hidden, and it's children will also be hidden
     */
    NONE;

    companion object: Default<Display> {
        override fun default(): Display {
            return BLOCK
        }
    }
}
