package be.arby.taffy.tree.cache

import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.AvailableSpace

data class CacheEntry<T> (
    /**
     * The initial cached size of the node itself
     */
    val knownDimensions: Size<Option<Float>>,
    /**
     * The initial cached size of the parent's node
     */
    val availableSpace: Size<AvailableSpace>,
    /**
     * The cached size and baselines of the item
     */
    val content: T
)
