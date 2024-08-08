package be.arby.taffy.tree.cache

import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Default
import be.arby.taffy.lang.Option
import be.arby.taffy.tree.layout.RunMode
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.utils.toInt

data class Cache(
    var finalLayoutEntry: Option<be.arby.taffy.tree.cache.CacheEntry<LayoutOutput>>,
    var measureEntries: Array<Option<be.arby.taffy.tree.cache.CacheEntry<Size<Float>>>>
) {
    /**
     * Return the cache slot to cache the current computed result in
     *
     * ## Caching Strategy
     *
     * We need multiple cache slots, because a node's size is often queried by it's parent multiple times in the course of the layout
     * process, and we don't want later results to clobber earlier ones.
     *
     * The two variables that we care about when determining cache slot are:
     *
     *   - How many "known_dimensions" are set. In the worst case, a node may be called first with neither dimension known, then with one
     *     dimension known (either width of height - which doesn't matter for our purposes here), and then with both dimensions known.
     *   - Whether unknown dimensions are being sized under a min-content or a max-content available space constraint (definite available space
     *     shares a cache slot with max-content because a node will generally be sized under one or the other but not both).
     *
     * ## Cache slots:
     *
     * - Slot 0: Both known_dimensions were set
     * - Slots 1-4: 1 of 2 known_dimensions were set and:
     *   - Slot 1: width but not height known_dimension was set and the other dimension was either a MaxContent or Definite available space constraintraint
     *   - Slot 2: width but not height known_dimension was set and the other dimension was a MinContent constraint
     *   - Slot 3: height but not width known_dimension was set and the other dimension was either a MaxContent or Definite available space constraintable space constraint
     *   - Slot 4: height but not width known_dimension was set and the other dimension was a MinContent constraint
     * - Slots 5-8: Neither known_dimensions were set and:
     *   - Slot 5: x-axis available space is MaxContent or Definite and y-axis available space is MaxContent or Definite
     *   - Slot 6: x-axis available space is MaxContent or Definite and y-axis available space is MinContent
     *   - Slot 7: x-axis available space is MinContent and y-axis available space is MaxContent or Definite
     *   - Slot 8: x-axis available space is MinContent and y-axis available space is MinContent
     */
    fun computeCacheSlot(
        knownDimensions: Size<Option<Float>>,
        availableSpace: Size<AvailableSpace>
    ): Int {
        val hasKnownWidth = knownDimensions.width.isSome()
        val hasKnownHeight = knownDimensions.height.isSome()

        // Slot 0: Both known_dimensions were set
        if (hasKnownWidth && hasKnownHeight) {
            return 0
        }

        // Slot 1: width but not height known_dimension was set and the other dimension was either a MaxContent or Definite available space constraint
        // Slot 2: width but not height known_dimension was set and the other dimension was a MinContent constraint
        if (hasKnownWidth && !hasKnownHeight) {
            return 1 + (availableSpace.height == AvailableSpace.MinContent).toInt()
        }

        // Slot 3: height but not width known_dimension was set and the other dimension was either a MaxContent or Definite available space constraint
        // Slot 4: height but not width known_dimension was set and the other dimension was a MinContent constraint
        if (hasKnownHeight && !hasKnownWidth) {
            return 3 + (availableSpace.width == AvailableSpace.MinContent).toInt()
        }

        // Slots 5-8: Neither known_dimensions were set and:
        return when (availableSpace.width) {
            is AvailableSpace.MaxContent, is AvailableSpace.Definite -> when (availableSpace.height) {
                // Slot 5: x-axis available space is MaxContent or Definite and y-axis available space is MaxContent or Definite
                is AvailableSpace.MaxContent, is AvailableSpace.Definite -> return 5
                // Slot 6: x-axis available space is MaxContent or Definite and y-axis available space is MinContent
                is AvailableSpace.MinContent -> return 6
            }
            is AvailableSpace.MinContent -> when (availableSpace.height) {
                // Slot 7: x-axis available space is MinContent and y-axis available space is MaxContent or Definite
                is AvailableSpace.MaxContent, is AvailableSpace.Definite -> return 7
                // Slot 8: x-axis available space is MinContent and y-axis available space is MinContent
                is AvailableSpace.MinContent -> return 8
            }
        }
    }

    /**
     * Try to retrieve a cached result from the cache
     */
    fun get(
        knownDimensions: Size<Option<Float>>,
        availableSpace: Size<AvailableSpace>,
        runMode: RunMode
    ): Option<LayoutOutput> {
        return when (runMode) {
            RunMode.PERFORM_LAYOUT -> finalLayoutEntry.filter { entry ->
                val cachedSize = entry.content.size

                (knownDimensions.width == entry.knownDimensions.width || knownDimensions.width == Option.Some(cachedSize.width))
                        && (knownDimensions.height == entry.knownDimensions.height
                        || knownDimensions.height == Option.Some(cachedSize.height))
                        && (knownDimensions.width.isSome()
                        || entry.availableSpace.width.isRoughlyEqual(availableSpace.width))
                        && (knownDimensions.height.isSome()
                        || entry.availableSpace.height.isRoughlyEqual(availableSpace.height))
            }
            RunMode.COMPUTE_SIZE -> {
                val cacheSlot = computeCacheSlot(knownDimensions, availableSpace)
                measureEntries[cacheSlot].map { it.content }
            }
            RunMode.PERFORM_HIDDEN_LAYOUT -> Option.None
        }
    }

    /**
     * Store a computed size in the cache
     */
    fun store(
        knownDimensions: Size<Option<Float>>,
        availableSpace: Size<AvailableSpace>,
        runMode: RunMode,
        layoutOutput: LayoutOutput,
    ) {
        when (runMode) {
            RunMode.PERFORM_LAYOUT -> {
                finalLayoutEntry = Option.Some(
                    be.arby.taffy.tree.cache.CacheEntry(
                        knownDimensions,
                        availableSpace,
                        layoutOutput
                    )
                )
            }
            RunMode.COMPUTE_SIZE -> {
                val cacheSlot = computeCacheSlot(knownDimensions, availableSpace)
                measureEntries[cacheSlot] = Option.Some(
                    be.arby.taffy.tree.cache.CacheEntry(
                        knownDimensions,
                        availableSpace,
                        layoutOutput.size
                    )
                )
            }
            RunMode.PERFORM_HIDDEN_LAYOUT -> {}
        }
    }

    /**
     * Clear all cache entries
     */
    fun clear() {
        finalLayoutEntry = Option.None
        measureEntries = Array(be.arby.taffy.tree.cache.Cache.Companion.CACHE_SIZE) { Option.None }
    }

    /**
     * Returns true if all cache entries are None, else false
     */
    fun isEmpty(): Boolean {
        return finalLayoutEntry.isNone() && !measureEntries.any { entry -> entry.isSome() }
    }

    companion object: Default<be.arby.taffy.tree.cache.Cache> {
        const val CACHE_SIZE = 9

        override fun default(): be.arby.taffy.tree.cache.Cache = be.arby.taffy.tree.cache.Cache.Companion.new()

        fun new(): be.arby.taffy.tree.cache.Cache {
            return be.arby.taffy.tree.cache.Cache(
                finalLayoutEntry = Option.None,
                measureEntries = Array(be.arby.taffy.tree.cache.Cache.Companion.CACHE_SIZE) { Option.None }
            )
        }
    }
}
