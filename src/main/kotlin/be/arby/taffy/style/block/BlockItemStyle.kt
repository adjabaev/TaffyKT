package be.arby.taffy.style.block

import be.arby.taffy.style.CoreStyle

/**
 * The set of styles required for a Block layout item (child of a Block container)
 */
interface BlockItemStyle: CoreStyle {
    /**
     * Whether the item is a table. Table children are handled specially in block layout.
     */
    fun isTable(): Boolean
}
