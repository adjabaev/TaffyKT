package be.arby.taffy.style.block

import be.arby.taffy.style.CoreStyle
import be.arby.taffy.style.Style

/**
 * The set of styles required for a Block layout container
 */
interface BlockContainerStyle: CoreStyle {
    /**
     * Defines which row in the grid the item should start and end at
     */
    fun textAlign(): TextAlign
}
