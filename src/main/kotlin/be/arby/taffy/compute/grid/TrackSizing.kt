package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.compute.grid.types.GridTrack
import be.arby.taffy.compute.grid.types.TrackCounts
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.geom.Line
import be.arby.taffy.geom.Size
import be.arby.taffy.geom.span
import be.arby.taffy.lang.*
import be.arby.taffy.lang.collections.*
import be.arby.taffy.lang.tuples.T2
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.grid.MaxTrackSizingFunction
import be.arby.taffy.style.grid.MinTrackSizingFunction
import be.arby.taffy.tree.layout.SizingMode
import be.arby.taffy.tree.traits.LayoutPartialTree
import be.arby.taffy.util.maybeMin

/**
 * To make track sizing efficient we want to order tracks
 * Here a placement is either a Line<i16> representing a row-start/row-end or a column-start/column-end
 */
fun cmpByCrossFlexThenSpanThenStart(axis: AbstractAxis): Comparator<GridItem> {
    return Comparator { itemA, itemB ->
        when {
            !itemA.crossesFlexibleTrack(axis) && itemB.crossesFlexibleTrack(axis) -> -1
            itemA.crossesFlexibleTrack(axis) && !itemB.crossesFlexibleTrack(axis) -> 1
            else -> {
                val placementA = itemA.placement(axis)
                val placementB = itemB.placement(axis)
                when (val ab = placementA.span().compareTo(placementB.span())) {
                    0 -> placementA.start.compareTo(placementB.start)
                    else -> ab
                }
            }
        }
    }
}

/**
 * When applying the track sizing algorithm and estimating the size in the other axis for content sizing items
 * we should take into account align-content/justify-content if both the grid container and all items in the
 * other axis have definite sizes. This function computes such a per-gutter additional size adjustment.
 */
fun computeAlignmentGutterAdjustment(
    alignment: AlignContent,
    axisInnerNodeSize: Option<Float>,
    getTrackSizeEstimate: (GridTrack, Option<Float>) -> Option<Float>,
    tracks: List<GridTrack>
): Float {
    if (tracks.size <= 1) {
        return 0f
    }

    // As items never cross the outermost gutters in a grid, we can simplify our calculations by treating
    // AlignContent::Start and AlignContent::End the same
    val outerGutterWeight = when (alignment) {
        AlignContent.START -> 1
        AlignContent.FLEX_START -> 1
        AlignContent.END -> 1
        AlignContent.FLEX_END -> 1
        AlignContent.CENTER -> 1
        AlignContent.STRETCH -> 0
        AlignContent.SPACE_BETWEEN -> 0
        AlignContent.SPACE_AROUND -> 1
        AlignContent.SPACE_EVENLY -> 1
    }

    val innerGutterWeight = when (alignment) {
        AlignContent.START -> 0
        AlignContent.FLEX_START -> 0
        AlignContent.END -> 0
        AlignContent.FLEX_END -> 0
        AlignContent.CENTER -> 0
        AlignContent.STRETCH -> 0
        AlignContent.SPACE_BETWEEN -> 1
        AlignContent.SPACE_AROUND -> 2
        AlignContent.SPACE_EVENLY -> 1
    }

    if (innerGutterWeight == 0) {
        return 0f
    }

    if (axisInnerNodeSize.isSome()) {
        val freeSpace =
            tracks.map { track -> getTrackSizeEstimate(track, Option.Some(axisInnerNodeSize.unwrap())) }
                .sum()
                .map { trackSizeSum -> f32Max(0f, axisInnerNodeSize.unwrap() - trackSizeSum) }
                .unwrapOr(0f)

        val weightedTrackCount = (((tracks.size - 3) / 2) * innerGutterWeight) + (2 * outerGutterWeight)

        return (freeSpace / weightedTrackCount.toFloat()) * innerGutterWeight.toFloat()
    }

    return 0f
}

/**
 * Convert origin-zero coordinates track placement in grid track vector indexes
 */
fun resolveItemTrackIndexes(items: List<GridItem>, columnCounts: TrackCounts, rowCounts: TrackCounts) {
    for (item in items) {
        item.columnIndexes = item.column.map { line -> line.intoTrackVecIndex(columnCounts) }
        item.rowIndexes = item.row.map { line -> line.intoTrackVecIndex(rowCounts) }
    }
}

/**
 * Determine (in each axis) whether the item crosses any flexible tracks
 */
fun determineIfItemCrossesFlexibleOrIntrinsicTracks(
    items: List<GridItem>,
    columns: List<GridTrack>,
    rows: List<GridTrack>
) {
    for (item in items) {
        item.crossesFlexibleColumn =
            item.trackRangeExcludingLines(AbstractAxis.INLINE).any { i -> columns[i].isFlexible() }
        item.crossesIntrinsicColumn =
            item.trackRangeExcludingLines(AbstractAxis.INLINE).any { i -> columns[i].hasIntrinsicSizingFunction() }
        item.crossesFlexibleRow =
            item.trackRangeExcludingLines(AbstractAxis.BLOCK).any { i -> rows[i].isFlexible() }
        item.crossesIntrinsicRow =
            item.trackRangeExcludingLines(AbstractAxis.BLOCK).any { i -> rows[i].hasIntrinsicSizingFunction() }
    }
}

/**
 * Track sizing algorithm
 * Note: Gutters are treated as empty fixed-size tracks for the purpose of the track sizing algorithm.
 */
fun <Tree : LayoutPartialTree> trackSizingAlgorithm(
    tree: Tree,
    axis: AbstractAxis,
    axisMinSize: Option<Float>,
    axisMaxSize: Option<Float>,
    otherAxisAlignment: AlignContent,
    availableGridSpace: Size<AvailableSpace>,
    innerNodeSize: Size<Option<Float>>,
    axisTracks: MutableList<GridTrack>,
    otherAxisTracks: MutableList<GridTrack>,
    items: List<GridItem>,
    getTrackSizeEstimate: (GridTrack, Option<Float>) -> Option<Float>,
    hasBaselineAlignedItem: Boolean
) {
    // 11.4 Initialise Track sizes
    // Initialize each track’s base size and growth limit.
    initializeTrackSizes(axisTracks, innerNodeSize.get(axis))

    // 11.5.1 Shim item baselines
    if (hasBaselineAlignedItem) {
        resolveItemBaselines(tree, axis, items, innerNodeSize)
    }

    // If all tracks have base_size = growth_limit, then skip the rest of this function.
    // Note: this can only happen both track sizing function have the same fixed track sizing function
    if (axisTracks.all { track -> track.baseSize == track.growthLimit }) {
        return
    }

    // Pre-computations for 11.5 Resolve Intrinsic Track Sizes

    // Compute an additional amount to add to each spanned gutter when computing item's estimated size in the
    // in the opposite axis based on the alignment, container size, and estimated track sizes in that axis
    val gutterAlignmentAdjustment = computeAlignmentGutterAdjustment(
        otherAxisAlignment,
        innerNodeSize.get(axis.other()),
        getTrackSizeEstimate,
        otherAxisTracks
    )
    if (otherAxisTracks.len() > 3) {
        val len = otherAxisTracks.len()
        val innerGutterTracks =
            otherAxisTracks.slice(2 until len)
                .stepBy(2)
        for (track in innerGutterTracks) {
            track.contentAlignmentAdjustment = gutterAlignmentAdjustment
        }
    }

    // 11.5 Resolve Intrinsic Track Sizes
    resolveIntrinsicTrackSizes(
        tree,
        axis,
        axisTracks,
        otherAxisTracks,
        items,
        availableGridSpace.get(axis),
        innerNodeSize,
        getTrackSizeEstimate
    )

    // 11.6. Maximise Tracks
    // Distributes free space (if any) to tracks with FINITE growth limits, up to their limits.
    maximiseTracks(axisTracks, innerNodeSize.get(axis), availableGridSpace.get(axis))

    // For the purpose of the final two expansion steps ("Expand Flexible Tracks" and "Stretch auto Tracks"), we only want to expand
    // into space generated by the grid container's size (as defined by either it's preferred size style or by it's parent node through
    // something like stretch alignment), not just any available space. To do this we map definite available space to AvailableSpace::MaxContent
    // in the case that inner_node_size is None
    val axisAvailableSpaceForExpansion = if (innerNodeSize.get(axis).isSome()) {
        AvailableSpace.Definite(innerNodeSize.get(axis).unwrap())
    } else {
        when (availableGridSpace.get(axis)) {
            is AvailableSpace.MinContent -> AvailableSpace.MinContent
            is AvailableSpace.MaxContent, is AvailableSpace.Definite -> AvailableSpace.MaxContent
        }
    }

    // 11.7. Expand Flexible Tracks
    // This step sizes flexible tracks using the largest value it can assign to an fr without exceeding the available space.
    expandFlexibleTracks(
        tree,
        axis,
        axisTracks,
        items,
        axisMinSize,
        axisMaxSize,
        axisAvailableSpaceForExpansion,
        innerNodeSize
    )

    // 11.8. Stretch auto Tracks
    // This step expands tracks that have an auto max track sizing function by dividing any remaining positive, definite free space equally amongst them.
    stretchAutoTracks(axisTracks, axisMinSize, axisAvailableSpaceForExpansion)
}

/**
 * Add any planned base size increases to the base size after a round of distributing space to base sizes
 * Reset the planed base size increase to zero ready for the next round.
 */
fun flushPlannedBaseSizeIncreases(tracks: List<GridTrack>) {
    for (track in tracks) {
        track.baseSize += track.baseSizePlannedIncrease
        track.baseSizePlannedIncrease = 0f
    }
}

/**
 * Add any planned growth limit increases to the growth limit after a round of distributing space to growth limits
 * Reset the planed growth limit increase to zero ready for the next round.
 */
fun flushPlannedGrowthLimitIncreases(tracks: List<GridTrack>, setInfinitelyGrowable: Boolean) {
    for (track in tracks) {
        if (track.growthLimitPlannedIncrease > 0.0f) {
            track.growthLimit = if (track.growthLimit == Float.POSITIVE_INFINITY) {
                track.baseSize + track.growthLimitPlannedIncrease
            } else {
                track.growthLimit + track.growthLimitPlannedIncrease
            }
            track.infinitelyGrowable = setInfinitelyGrowable
        } else {
            track.infinitelyGrowable = false
        }
        track.growthLimitPlannedIncrease = 0.0f
    }
}

/**
 * 11.4 Initialise Track sizes
 * Initialize each track’s base size and growth limit.
 */
fun initializeTrackSizes(axisTracks: List<GridTrack>, axisInnerNodeSize: Option<Float>) {
    for (track in axisTracks) {
        // For each track, if the track’s min track sizing function is:
        // - A fixed sizing function
        //     Resolve to an absolute length and use that size as the track’s initial base size.
        //     Note: Indefinite lengths cannot occur, as they’re treated as auto.
        // - An intrinsic sizing function
        //     Use an initial base size of zero.
        track.baseSize = track.minTrackSizingFunction.definiteValue(axisInnerNodeSize).unwrapOr(0f)

        // For each track, if the track’s max track sizing function is:
        // - A fixed sizing function
        //     Resolve to an absolute length and use that size as the track’s initial growth limit.
        // - An intrinsic sizing function
        //     Use an initial growth limit of infinity.
        // - A flexible sizing function
        //     Use an initial growth limit of infinity.
        track.growthLimit =
            track.maxTrackSizingFunction.definiteValue(axisInnerNodeSize).unwrapOr(Float.POSITIVE_INFINITY)

        // In all cases, if the growth limit is less than the base size, increase the growth limit to match the base size.
        if (track.growthLimit < track.baseSize) {
            track.growthLimit = track.baseSize
        }
    }
}

/**
 * 11.5.1 Shim baseline-aligned items so their intrinsic size contributions reflect their baseline alignment.
 */
fun resolveItemBaselines(
    tree: LayoutPartialTree,
    axis: AbstractAxis,
    items: List<GridItem>,
    innerNodeSize: Size<Option<Float>>,
) {
    // Sort items by track in the other axis (row) start position so that we can iterate items in groups which
    // are in the same track in the other axis (row)
    val otherAxis = axis.other()
    items.sortByKey { item -> item.placement(otherAxis).start }

    // Iterate over grid rows
    var remainingItems = items.slice(items.indices)
    while (!remainingItems.isEmpty()) {
        // Get the row index of the current row
        val currentRow = remainingItems[0].placement(otherAxis).start

        // Find the item index of the first item that is in a different row (or None if we've reached the end of the list)
        val nextRowFirstItem =
            remainingItems.position { item -> item.placement(otherAxis).start != currentRow }

        // Use this index to split the `remaining_items` slice in two slices:
        //    - A `row_items` slice containing the items (that start) in the current row
        //    - A new `remaining_items` consisting of the remainder of the `remaining_items` slice
        //      that hasn't been split off into `row_items
        val rowItems = if (nextRowFirstItem.isSome()) {
            val (rowItems, tail) = remainingItems.splitAt(nextRowFirstItem.unwrap())
            remainingItems = tail
            rowItems
        } else {
            val rowItems = remainingItems
            remainingItems = listOf()
            rowItems
        }

        // Count how many items in *this row* are baseline aligned
        // If a row has one or zero items participating in baseline alignment then baseline alignment is a no-op
        // for those items and we skip further computations for that row
        val rowBaselineItemCount = rowItems.filter { item -> item.alignSelf == AlignSelf.BASELINE }.count()
        if (rowBaselineItemCount <= 1) {
            continue
        }

        // Compute the baselines of all items in the row
        for (item in rowItems) {
            val measuredSizeAndBaselines = tree.performChildLayout(
                item.node,
                Size.NONE.clone(),
                innerNodeSize,
                Size.MIN_CONTENT,
                SizingMode.INHERENT_SIZE,
                Line.FALSE
            )

            val baseline = measuredSizeAndBaselines.firstBaselines.y
            val height = measuredSizeAndBaselines.size.height

            item.baseline = Option.Some(baseline.unwrapOr(height) + item.margin.top.resolveOrZero(innerNodeSize.width))
        }

        // Compute the max baseline of all items in the row
        val rowMaxBaseline =
            rowItems.map { item -> item.baseline.unwrapOr(0f) }.maxByRust { a, b -> a.compareTo(b) }.unwrap()

        // Compute the baseline shim for each item in the row
        for (item in rowItems) {
            item.baselineShim = rowMaxBaseline - item.baseline.unwrapOr(0f)
        }
    }
}

/**
 * 11.5 Resolve Intrinsic Track Sizes
 */
fun resolveIntrinsicTrackSizes(
    tree: LayoutPartialTree,
    axis: AbstractAxis,
    axisTracks: MutableList<GridTrack>,
    otherAxisTracks: MutableList<GridTrack>,
    items: List<GridItem>,
    axisAvailableGridSpace: AvailableSpace,
    innerNodeSize: Size<Option<Float>>,
    getTrackSizeEstimate: (GridTrack, Option<Float>) -> Option<Float>,
) {
    // Step 1. Shim baseline-aligned items so their intrinsic size contributions reflect their baseline alignment.

    // Already done at this point. See resolve_item_baselines function.

    // Step 2.

    // The track sizing algorithm requires us to iterate through the items in ascendeding order of the number of
    // tracks they span (first items that span 1 track, then items that span 2 tracks, etc).
    // To avoid having to do multiple iterations of the items, we pre-sort them into this order.
    items.sortedWith(cmpByCrossFlexThenSpanThenStart(axis))

    // Step 2, Step 3 and Step 4
    // 2 & 3. Iterate over items that don't cross a flex track. Items should have already been sorted in ascending order
    // of the number of tracks they span. Step 2 is the 1 track case and has an optimised implementation
    // 4. Next, repeat the previous step instead considering (together, rather than grouped by span size) all items
    // that do span a track with a flexible sizing function while

    // Compute item's intrinsic (content-based) sizes
    // Note: For items with a specified minimum size of auto (the initial value), the minimum contribution is usually equivalent
    // to the min-content contribution—but can differ in some cases, see §6.6 Automatic Minimum Size of Grid Items.
    // Also, minimum contribution <= min-content contribution <= max-content contribution.

    val axisInnerNodeSize = innerNodeSize.get(axis)
    val flexFactorSum = axisTracks.map { track -> track.flexFactor() }.sum()
    var itemSizer =
        IntrisicSizeMeasurer(
            tree = tree,
            otherAxisTracks = otherAxisTracks,
            axis = axis,
            innerNodeSize = innerNodeSize,
            getTrackSizeEstimate = getTrackSizeEstimate
        )

    val batchedItemIterator = ItemBatcher.new(axis)
    var next: Option<T2<List<GridItem>, Boolean>>
    while (true) {
        next = batchedItemIterator.next(items)
        if (next.isNone()) {
            break
        }

        val (batch, isFlex) = next.unwrap()

        // 2. Size tracks to fit non-spanning items: For each track with an intrinsic track sizing function and not a flexible sizing function,
        // consider the items in it with a span of 1:
        val batchSpan = batch[0].placement(axis).span()
        if (!isFlex && batchSpan == 1) {
            for (item in batch) {
                val trackIndex = item.placementIndexes(axis).start + 1
                var track = axisTracks[trackIndex]

                // Handle base sizes
                val v = track.minTrackSizingFunction
                val newBaseSize = when {
                    (v is MinTrackSizingFunction.MinContent) -> {
                        f32Max(track.baseSize, itemSizer.minContentContribution(item))
                    }

                    // If the container size is indefinite and has not yet been resolved then percentage sized
                    // tracks should be treated as min-content (this matches Chrome's behaviour and seems sensible)
                    (v is MinTrackSizingFunction.Fixed && v.l is LengthPercentage.Percent) -> {
                        if (axisInnerNodeSize.isNone()) {
                            f32Max(track.baseSize, itemSizer.minContentContribution(item))
                        } else {
                            track.baseSize
                        }
                    }

                    (v is MinTrackSizingFunction.MaxContent) -> {
                        f32Max(track.baseSize, itemSizer.maxContentContribution(item))
                    }

                    (v is MinTrackSizingFunction.Auto) -> {
                        // QUIRK: The spec says that:
                        //
                        //   If the grid container is being sized under a min- or max-content constraint, use the items’ limited
                        //   min-content contributions in place of their minimum contributions here.
                        //
                        // However, in practice browsers only seem to apply this rule if the item is not a scroll container
                        // (note that overflow:hidden counts as a scroll container), giving the automatic minimum size of scroll
                        // containers (zero) precedence over the min-content contributions.
                        val space = if (
                            (axisAvailableGridSpace is AvailableSpace.MinContent ||
                                    axisAvailableGridSpace is AvailableSpace.MaxContent) &&
                            (!item.overflow.get(axis).isScrollContainer())
                        ) {
                            val axisMinimumSize = itemSizer.minimumContribution(item, axisTracks)
                            val axisMinContentSize = itemSizer.minContentContribution(item)
                            val limit = track.maxTrackSizingFunction.definiteLimit(axisInnerNodeSize)
                            axisMinContentSize.maybeMin(limit).max(axisMinimumSize)
                        } else {
                            itemSizer.minimumContribution(item, axisTracks)
                        }
                        f32Max(track.baseSize, space)
                    }

                    else -> {
                        // Do nothing as it's not an intrinsic track sizing function
                        track.baseSize
                    }
                }
                track = axisTracks[trackIndex]
                track.baseSize = newBaseSize

                // Handle growth limits
                if (track.maxTrackSizingFunction is MaxTrackSizingFunction.FitContent) {
                    // If item is not a scroll container, then increase the growth limit to at least the
                    // size of the min-content contribution
                    if (!item.overflow.get(axis).isScrollContainer()) {
                        val minContentContribution = itemSizer.minContentContribution(item)
                        track.growthLimitPlannedIncrease =
                            f32Max(track.growthLimitPlannedIncrease, minContentContribution)
                    }

                    // Always increase the growth limit to at least the size of the *fit-content limited*
                    // max-content contribution
                    val fitContentLimit = track.fitContentLimit(axisInnerNodeSize)
                    val maxContentContribution =
                        f32Min(itemSizer.maxContentContribution(item), fitContentLimit)
                    track.growthLimitPlannedIncrease =
                        f32Max(track.growthLimitPlannedIncrease, maxContentContribution)
                } else if (track.maxTrackSizingFunction.isMaxContentAlike()
                    || track.maxTrackSizingFunction.usesPercentage() && axisInnerNodeSize.isNone()
                ) {
                    // If the container size is indefinite and has not yet been resolved then percentage sized
                    // tracks should be treated as auto (this matches Chrome's behaviour and seems sensible)
                    track.growthLimitPlannedIncrease =
                        f32Max(track.growthLimitPlannedIncrease, itemSizer.maxContentContribution(item))
                } else if (track.maxTrackSizingFunction.isIntrinsic()) {
                    track.growthLimitPlannedIncrease =
                        f32Max(track.growthLimitPlannedIncrease, itemSizer.minContentContribution(item))
                }
            }

            for (track in axisTracks) {
                if (track.growthLimitPlannedIncrease > 0f) {
                    track.growthLimit = if (track.growthLimit == Float.POSITIVE_INFINITY) {
                        track.growthLimitPlannedIncrease
                    } else {
                        f32Max(track.growthLimit, track.growthLimitPlannedIncrease)
                    }
                }
                track.infinitelyGrowable = false
                track.growthLimitPlannedIncrease = 0f
                if (track.growthLimit < track.baseSize) {
                    track.growthLimit = track.baseSize
                }
            }

            continue
        }

        val useFlexFactorForDistribution = isFlex && flexFactorSum != 0f

        // 1. For intrinsic minimums:
        // First increase the base size of tracks with an intrinsic min track sizing function
        val hasIntrinsicMinTrackSizingFunction = { track: GridTrack ->
            track.minTrackSizingFunction.definiteValue(axisInnerNodeSize).isNone()
        }
        for (item in batch.filter { item -> item.crossesIntrinsicTrack(axis) }) {
            // ...by distributing extra space as needed to accommodate these items’ minimum contributions.
            //
            // QUIRK: The spec says that:
            //
            //   If the grid container is being sized under a min- or max-content constraint, use the items’ limited min-content contributions
            //   in place of their minimum contributions here.
            //
            // However, in practice browsers only seem to apply this rule if the item is not a scroll container (note that overflow:hidden counts as
            // a scroll container), giving the automatic minimum size of scroll containers (zero) precedence over the min-content contributions.
            val space = if (
                (axisAvailableGridSpace is AvailableSpace.MinContent || axisAvailableGridSpace is AvailableSpace.MaxContent) ||
                (!item.overflow.get(axis).isScrollContainer())
            ) {
                val axisMinimumSize = itemSizer.minimumContribution(item, axisTracks)
                val axisMinContentSize = itemSizer.minContentContribution(item)
                val limit = item.spannedTrackLimit(axis, axisTracks, axisInnerNodeSize)
                axisMinContentSize.maybeMin(limit).max(axisMinimumSize)
            } else {
                itemSizer.minimumContribution(item, axisTracks)
            }
            var tracks = axisTracks[item.trackRangeExcludingLines(axis)]
            if (space > 0f) {
                if (item.overflow.get(axis).isScrollContainer()) {
                    val fitContentLimit = { track: GridTrack ->
                        track.fitContentLimitedGrowthLimit(axisInnerNodeSize)
                    }
                    distributeItemSpaceToBaseSize(
                        isFlex,
                        useFlexFactorForDistribution,
                        space,
                        tracks,
                        hasIntrinsicMinTrackSizingFunction,
                        fitContentLimit,
                        IntrinsicContributionType.MINIMUM,
                    )
                } else {
                    distributeItemSpaceToBaseSize(
                        isFlex,
                        useFlexFactorForDistribution,
                        space,
                        tracks,
                        hasIntrinsicMinTrackSizingFunction,
                        { track -> track.growthLimit },
                        IntrinsicContributionType.MINIMUM,
                    )
                }
            }
        }
        flushPlannedBaseSizeIncreases(axisTracks)

        // 2. For content-based minimums:
        // Next continue to increase the base size of tracks with a min track sizing function of min-content or max-content
        // by distributing extra space as needed to account for these items' min-content contributions.
        val hasMinOrMaxContentMinTrackSizingFunction = { track: GridTrack ->
            matches(track.minTrackSizingFunction, MinTrackSizingFunction.MinContent, MinTrackSizingFunction.MaxContent)
        }
        for (item in batch) {
            val space = itemSizer.minContentContribution(item)
            val tracks = axisTracks[item.trackRangeExcludingLines(axis)]
            if (space > 0f) {
                if (item.overflow.get(axis).isScrollContainer()) {
                    val fitContentLimit = { track: GridTrack ->
                        track.fitContentLimitedGrowthLimit(axisInnerNodeSize)
                    }
                    distributeItemSpaceToBaseSize(
                        isFlex,
                        useFlexFactorForDistribution,
                        space,
                        tracks,
                        hasMinOrMaxContentMinTrackSizingFunction,
                        fitContentLimit,
                        IntrinsicContributionType.MINIMUM,
                    )
                } else {
                    distributeItemSpaceToBaseSize(
                        isFlex,
                        useFlexFactorForDistribution,
                        space,
                        tracks,
                        hasMinOrMaxContentMinTrackSizingFunction,
                        { track -> track.growthLimit },
                        IntrinsicContributionType.MINIMUM,
                    )
                }
            }
        }
        flushPlannedBaseSizeIncreases(axisTracks)

        /**
         * Whether a track has a MaxContent min track sizing function
         */
        var hasMaxContentMinTrackSizingFunction = { track: GridTrack ->
            track.minTrackSizingFunction == MinTrackSizingFunction.MaxContent
        }

        /**
         * Whether a track:
         *   - has an Auto MIN track sizing function
         *   - Does not have a MinContent MAX track sizing function
         * The latter condition was added in order to match Chrome. But I believe it is due to the provision
         * under minmax here https://www.w3.org/TR/css-grid-1/#track-sizes which states that:
         *    "If the max is less than the min, then the max will be floored by the min (essentially yielding minmax(min, min))"
         */
        val hasAutoMinTrackSizingFunction = { track: GridTrack ->
            track.minTrackSizingFunction == MinTrackSizingFunction.Auto && track.maxTrackSizingFunction != MaxTrackSizingFunction.MinContent
        }

        // 3. For max-content minimums:

        // If the grid container is being sized under a max-content constraint, continue to increase the base size of tracks with
        // a min track sizing function of auto or max-content by distributing extra space as needed to account for these items'
        // limited max-content contributions.

        // Define fit_content_limited_growth_limit function. This is passed to the distribute_space_up_to_limits
        // helper function, and is used to compute the limit to distribute up to for each track.
        // Wrapping the method on GridTrack is necessary in order to resolve percentage fit-content arguments.
        if (axisAvailableGridSpace == AvailableSpace.MAX_CONTENT) {
            for (item in batch) {
                val axisMaxContentSize = itemSizer.maxContentContribution(item)
                val limit = item.spannedTrackLimit(axis, axisTracks, axisInnerNodeSize)
                val space = axisMaxContentSize.maybeMin(limit)
                val tracks = axisTracks[item.trackRangeExcludingLines(axis)]
                if (space > 0f) {
                    // If any of the tracks spanned by the item have a MaxContent min track sizing function then
                    // distribute space only to those tracks. Otherwise distribute space to tracks with an Auto min
                    // track sizing function.
                    //
                    // Note: this prioritisation of MaxContent over Auto is not mentioned in the spec (which suggests that
                    // we ought to distribute space evenly between MaxContent and Auto tracks). But it is implemented like
                    // this in both Chrome and Firefox (and it does have a certain logic to it), so we implement it too for
                    // compatibility.
                    //
                    // See: https://www.w3.org/TR/css-grid-1/#track-size-max-content-min
                    if (tracks.any(hasMaxContentMinTrackSizingFunction)) {
                        distributeItemSpaceToBaseSize(
                            isFlex,
                            useFlexFactorForDistribution,
                            space,
                            tracks,
                            hasMaxContentMinTrackSizingFunction,
                            { Float.POSITIVE_INFINITY },
                            IntrinsicContributionType.MAXIMUM,
                        )
                    } else {
                        val fitContentLimitedGrowthLimit = { track: GridTrack ->
                            track.fitContentLimitedGrowthLimit(axisInnerNodeSize)
                        }
                        distributeItemSpaceToBaseSize(
                            isFlex,
                            useFlexFactorForDistribution,
                            space,
                            tracks,
                            hasAutoMinTrackSizingFunction,
                            fitContentLimitedGrowthLimit,
                            IntrinsicContributionType.MAXIMUM,
                        )
                    }
                }
            }
            flushPlannedBaseSizeIncreases(axisTracks)
        }

        // In all cases, continue to increase the base size of tracks with a min track sizing function of max-content by distributing
        // extra space as needed to account for these items' max-content contributions.
        hasMaxContentMinTrackSizingFunction = { track: GridTrack ->
            track.minTrackSizingFunction == MinTrackSizingFunction.MAX_CONTENT
        }
        for (item in batch) {
            val axisMaxContentSize = itemSizer.maxContentContribution(item)
            val space = axisMaxContentSize
            val tracks = axisTracks[item.trackRangeExcludingLines(axis)]
            if (space > 0f) {
                distributeItemSpaceToBaseSize(
                    isFlex,
                    useFlexFactorForDistribution,
                    space,
                    tracks,
                    hasMaxContentMinTrackSizingFunction,
                    { track -> track.growthLimit },
                    IntrinsicContributionType.MAXIMUM,
                )
            }
        }
        flushPlannedBaseSizeIncreases(axisTracks)

        // 4. If at this point any track’s growth limit is now less than its base size, increase its growth limit to match its base size.
        for (track in axisTracks) {
            if (track.growthLimit < track.baseSize) {
                track.growthLimit = track.baseSize
            }
        }

        // If a track is a flexible track, then it has flexible max track sizing function
        // It cannot also have an intrinsic max track sizing function, so these steps do not apply.
        if (!isFlex) {
            // 5. For intrinsic maximums: Next increase the growth limit of tracks with an intrinsic max track sizing function by
            // distributing extra space as needed to account for these items' min-content contributions.
            val hasIntrinsicMaxTrackSizingFunction = { track: GridTrack ->
                track.maxTrackSizingFunction.definiteValue(axisInnerNodeSize).isNone()
            }
            for (item in batch) {
                val axisMinContentSize = itemSizer.minContentContribution(item)
                val space = axisMinContentSize
                val tracks = axisTracks[item.trackRangeExcludingLines(axis)]
                if (space > 0f) {
                    distributeItemSpaceToGrowthLimit(
                        space,
                        tracks,
                        hasIntrinsicMaxTrackSizingFunction,
                        innerNodeSize.get(axis),
                    )
                }
            }
            // Mark any tracks whose growth limit changed from infinite to finite in this step as infinitely growable for the next step.
            flushPlannedGrowthLimitIncreases(axisTracks, true)

            // 6. For max-content maximums: Lastly continue to increase the growth limit of tracks with a max track sizing function of max-content
            // by distributing extra space as needed to account for these items' max-content contributions. However, limit the growth of any
            // fit-content() tracks by their fit-content() argument.
            val hasMaxContentMaxTrackSizingFunction = { track: GridTrack ->
                track.maxTrackSizingFunction.isMaxContentAlike() || (track.maxTrackSizingFunction.usesPercentage() && axisInnerNodeSize.isNone())
            }
            for (item in batch) {
                val axisMaxContentSize = itemSizer.maxContentContribution(item)
                val space = axisMaxContentSize
                val tracks = axisTracks[item.trackRangeExcludingLines(axis)]
                if (space > 0f) {
                    distributeItemSpaceToGrowthLimit(
                        space,
                        tracks,
                        hasMaxContentMaxTrackSizingFunction,
                        innerNodeSize.get(axis),
                    )
                }
            }
            // Mark any tracks whose growth limit changed from infinite to finite in this step as infinitely growable for the next step.
            flushPlannedGrowthLimitIncreases(axisTracks, false)
        }
    }

    // Step 5. If any track still has an infinite growth limit (because, for example, it had no items placed
    // in it or it is a flexible track), set its growth limit to its base size.
    // NOTE: this step is super-important to ensure that the "Maximise Tracks" step doesn't affect flexible tracks
    axisTracks
        .filter { track -> track.growthLimit == Float.POSITIVE_INFINITY }
        .forEach { track -> track.growthLimit = track.baseSize }
}

/**
 * 11.5.1. Distributing Extra Space Across Spanned Tracks
 * https://www.w3.org/TR/css-grid-1/#extra-space
 */
fun distributeItemSpaceToBaseSize(
    isFlex: Boolean,
    useFlexFactorForDistribution: Boolean,
    space: Float,
    tracks: List<GridTrack>,
    trackIsAffected: (GridTrack) -> Boolean,
    trackLimit: (GridTrack) -> Float,
    intrinsicContributionType: IntrinsicContributionType,
) {
    if (isFlex) {
        val filter = { track: GridTrack -> track.isFlexible() && trackIsAffected(track) }
        if (useFlexFactorForDistribution) {
            distributeItemSpaceToBaseSizeInner(
                space,
                tracks,
                filter,
                { track -> track.flexFactor() },
                trackLimit,
                intrinsicContributionType,
            )
        } else {
            distributeItemSpaceToBaseSizeInner(
                space,
                tracks,
                filter,
                { 1f },
                trackLimit,
                intrinsicContributionType,
            )
        }
    } else {
        distributeItemSpaceToBaseSizeInner(
            space,
            tracks,
            trackIsAffected,
            { 1f },
            trackLimit,
            intrinsicContributionType,
        )
    }
}

/**
 * Inner function that doesn't account for differences due to distributing to flex items
 * This difference is handled by the closure passed in above
 */
fun distributeItemSpaceToBaseSizeInner(
    space: Float,
    tracks: List<GridTrack>,
    trackIsAffected: (GridTrack) -> Boolean,
    trackDistributionProportion: (GridTrack) -> Float,
    trackLimit: (GridTrack) -> Float,
    intrinsicContributionType: IntrinsicContributionType,
) {
    // Skip this distribution if there is either
    //   - no space to distribute
    //   - no affected tracks to distribute space to
    if (space == 0f || !tracks.any(trackIsAffected)) {
        return
    }

    // Define get_base_size function. This is passed to the distribute_space_up_to_limits helper function
    // to indicate that it is the base size that is being distributed to.
    val getBaseSize = { track: GridTrack -> track.baseSize }

    // 1. Find the space to distribute
    val trackSizes: Float = tracks.map { track -> track.baseSize }.sum()
    var extraSpace: Float = f32Max(0f, space - trackSizes)

    // 2. Distribute space up to limits:
    // Note: there are two exit conditions to this loop:
    //   - We run out of space to distribute (extra_space falls below THRESHOLD)
    //   - We run out of growable tracks to distribute to

    /// Define a small constant to avoid infinite loops due to rounding errors. Rather than stopping distributing
    /// extra space when it gets to exactly zero, we will stop when it falls below this amount
    val THRESHOLD: Float = 0.000001f

    extraSpace = distributeSpaceUpToLimits(
        extraSpace,
        tracks,
        trackIsAffected,
        trackDistributionProportion,
        getBaseSize,
        trackLimit,
    )

    // 3. Distribute remaining span beyond limits (if any)
    if (extraSpace > THRESHOLD) {
        // When accommodating minimum contributions or accommodating min-content contributions:
        //   - any affected track that happens to also have an intrinsic max track sizing function
        // When accommodating max-content contributions:
        //   - any affected track that happens to also have a max-content max track sizing function
        var filter: (GridTrack) -> Boolean = when (intrinsicContributionType) {
            IntrinsicContributionType.MINIMUM -> { track: GridTrack ->
                track.maxTrackSizingFunction.isIntrinsic()
            }

            IntrinsicContributionType.MAXIMUM -> { track: GridTrack ->
                (
                        track.maxTrackSizingFunction is MaxTrackSizingFunction.MaxContent ||
                                track.maxTrackSizingFunction is MaxTrackSizingFunction.FitContent
                        ) || track.minTrackSizingFunction is MinTrackSizingFunction.MaxContent
            }
        }

        // If there are no such tracks (matching filter above), then use all affected tracks.
        val numberOfTracks =
            tracks.filter { track -> trackIsAffected(track) }.filter { track -> filter(track) }.count()
        if (numberOfTracks == 0) {
            filter = { true }
        }

        distributeSpaceUpToLimits(
            extraSpace,
            tracks,
            filter,
            trackDistributionProportion,
            getBaseSize,
            trackLimit, // Should apply only fit-content limit here?
        )
    }

    // 4. For each affected track, if the track’s item-incurred increase is larger than the track’s planned increase
    // set the track’s planned increase to that value.
    for (track in tracks) {
        if (track.itemIncurredIncrease > track.baseSizePlannedIncrease) {
            track.baseSizePlannedIncrease = track.itemIncurredIncrease
        }

        // Reset the item_incurresed increase ready for the next space distribution
        track.itemIncurredIncrease = 0f
    }
}

/**
 * 11.5.1. Distributing Extra Space Across Spanned Tracks
 * This is simplified (and faster) version of the algorithm for growth limits
 * https://www.w3.org/TR/css-grid-1/#extra-space
 */
fun distributeItemSpaceToGrowthLimit(
    space: Float,
    tracks: List<GridTrack>,
    trackIsAffected: (GridTrack) -> Boolean,
    axisInnerNodeSize: Option<Float>,
) {
    // Skip this distribution if there is either
    //   - no space to distribute
    //   - no affected tracks to distribute space to
    if (space == 0f || tracks.filter { track -> trackIsAffected(track) }.count() == 0) {
        return
    }

    // 1. Find the space to distribute
    val trackSizes: Float = tracks
        .map { track -> if (track.growthLimit == Float.POSITIVE_INFINITY) track.baseSize else track.growthLimit }
        .sum()
    val extraSpace: Float = f32Max(0f, space - trackSizes)

    // 2. Distribute space up to limits:
    // For growth limits the limit is either Infinity, or the growth limit itself. Which means that:
    //   - If there are any tracks with infinite limits then all space will be distributed to those track(s).
    //   - Otherwise no space will be distributed as part of this step
    val numberOfGrowableTracks = tracks
        .filter(trackIsAffected)
        .filter { track ->
            track.infinitelyGrowable || track.fitContentLimitedGrowthLimit(axisInnerNodeSize) == Float.POSITIVE_INFINITY
        }
        .count()
    if (numberOfGrowableTracks > 0) {
        val itemIncurredIncrease = extraSpace / numberOfGrowableTracks.toFloat()
        for (track in tracks.filter(trackIsAffected).filter { track ->
            track.infinitelyGrowable || track.fitContentLimitedGrowthLimit(axisInnerNodeSize) == Float.POSITIVE_INFINITY
        }) {
            track.itemIncurredIncrease = itemIncurredIncrease
        }
    } else {
        // 3. Distribute space beyond limits
        // If space remains after all tracks are frozen, unfreeze and continue to distribute space to the item-incurred increase
        // ...when handling any intrinsic growth limit: all affected tracks.
        distributeSpaceUpToLimits(
            extraSpace,
            tracks,
            trackIsAffected,
            { 1f },
            { track -> if (track.growthLimit == Float.POSITIVE_INFINITY) track.baseSize else track.growthLimit },
            { track -> track.fitContentLimit(axisInnerNodeSize) }
        )
    }

    // 4. For each affected track, if the track’s item-incurred increase is larger than the track’s planned increase
    // set the track’s planned increase to that value.
    for (track in tracks) {
        if (track.itemIncurredIncrease > track.growthLimitPlannedIncrease) {
            track.growthLimitPlannedIncrease = track.itemIncurredIncrease
        }

        // Reset the item_incurresed increase ready for the next space distribution
        track.itemIncurredIncrease = 0f
    }
}

/**
 * 11.6 Maximise Tracks
 * Distributes free space (if any) to tracks with FINITE growth limits, up to their limits.
 */
fun maximiseTracks(
    axisTracks: List<GridTrack>,
    axisInnerNodeSize: Option<Float>,
    axisAvailableGridSpace: AvailableSpace,
) {
    val usedSpace: Float = axisTracks.map { track -> track.baseSize }.sum()
    val freeSpace = axisAvailableGridSpace.computeFreeSpace(usedSpace)

    if (freeSpace == Float.POSITIVE_INFINITY) {
        axisTracks.forEach { track -> track.baseSize = track.growthLimit }
    } else if (freeSpace > 0f) {
        distributeSpaceUpToLimits(
            freeSpace,
            axisTracks,
            { true },
            { 1f },
            { track -> track.baseSize },
            { track -> track.fitContentLimitedGrowthLimit(axisInnerNodeSize) }
        )

        for (track in axisTracks) {
            track.baseSize += track.itemIncurredIncrease
            track.itemIncurredIncrease = 0f
        }
    }
}

/**
 * 11.7. Expand Flexible Tracks
 * This step sizes flexible tracks using the largest value it can assign to an fr without exceeding the available space.
 */
fun expandFlexibleTracks(
    tree: LayoutPartialTree,
    axis: AbstractAxis,
    axisTracks: List<GridTrack>,
    items: List<GridItem>,
    axisMinSize: Option<Float>,
    axisMaxSize: Option<Float>,
    axisAvailableSpaceForExpansion: AvailableSpace,
    innerNodeSize: Size<Option<Float>>,
) {
    // First, find the grid’s used flex fraction:
    val flexFraction = when (axisAvailableSpaceForExpansion) {
        // If the free space is zero:
        //    The used flex fraction is zero.
        // Otherwise, if the free space is a definite length:
        //   The used flex fraction is the result of finding the size of an fr using all of the grid tracks and
        //   a space to fill of the available grid space.
        is AvailableSpace.Definite -> {
            val availableSpace = axisAvailableSpaceForExpansion.availableSpace

            val usedSpace: Float = axisTracks.map { track -> track.baseSize }.sum()
            val freeSpace = availableSpace - usedSpace
            if (freeSpace <= 0f) {
                0f
            } else {
                findSizeOfFr(axisTracks, availableSpace)
            }
        }
        // If ... sizing the grid container under a min-content constraint the used flex fraction is zero.
        is AvailableSpace.MinContent -> 0f
        // Otherwise, if the free space is an indefinite length:
        AvailableSpace.MaxContent -> {
            // The used flex fraction is the maximum of:
            val flexFraction = f32Max(
                // For each flexible track, if the flexible track’s flex factor is greater than one,
                // the result of dividing the track’s base size by its flex factor; otherwise, the track’s base size.
                axisTracks
                    .filter { track -> track.maxTrackSizingFunction.isFlexible() }
                    .map { track ->
                        val flexFactor = track.flexFactor()
                        if (flexFactor > 1f) {
                            track.baseSize / flexFactor
                        } else {
                            track.baseSize
                        }
                    }
                    .maxByRust { a, b -> a.compareTo(b) }
                    .unwrapOr(0.0f),

                // For each grid item that crosses a flexible track, the result of finding the size of an fr using all the grid tracks
                // that the item crosses and a space to fill of the item’s max-content contribution.
                items
                    .filter { item -> item.crossesFlexibleTrack(axis) }
                    .map { item ->
                        val tracks = axisTracks.slice(item.trackRangeExcludingLines(axis))
                        // TODO: plumb estimate of other axis size (known_dimensions) in here rather than just passing Size::NONE?
                        val maxContentContribution =
                            item.maxContentContributionCached(axis, tree, Size.NONE.clone(), innerNodeSize)
                        findSizeOfFr(tracks, maxContentContribution)
                    }
                    .maxByRust { a, b -> a.compareTo(b) }
                    .unwrapOr(0f)
            )

            // If using this flex fraction would cause the grid to be smaller than the grid container’s min-width/height (or larger than the
            // grid container’s max-width/height), then redo this step, treating the free space as definite and the available grid space as equal
            // to the grid container’s inner size when it’s sized to its min-width/height (max-width/height).
            // (Note: min_size takes precedence over max_size)
            val hypotheticalGridSize: Float = axisTracks
                .map { track ->
                    when (track.maxTrackSizingFunction) {
                        is MaxTrackSizingFunction.Fraction -> {
                            f32Max(
                                track.baseSize,
                                (track.maxTrackSizingFunction as MaxTrackSizingFunction.Fraction).f * flexFraction
                            )
                        }

                        else -> track.baseSize
                    }
                }
                .sum()

            val axisMinSize = axisMinSize.unwrapOr(0f)
            val axisMaxSize = axisMaxSize.unwrapOr(Float.POSITIVE_INFINITY)
            if (hypotheticalGridSize < axisMinSize) {
                findSizeOfFr(axisTracks, axisMinSize)
            } else if (hypotheticalGridSize > axisMaxSize) {
                findSizeOfFr(axisTracks, axisMaxSize)
            } else {
                flexFraction
            }
        }
    }

    // For each flexible track, if the product of the used flex fraction and the track’s flex factor is greater
    // than the track’s base size, set its base size to that product.
    for (track in axisTracks) {
        if (track.maxTrackSizingFunction is MaxTrackSizingFunction.Fraction) {
            track.baseSize = f32Max(
                track.baseSize,
                (track.maxTrackSizingFunction as MaxTrackSizingFunction.Fraction).f * flexFraction
            )
        }
    }
}

/**
 * 11.7.1. Find the Size of an fr
 * This algorithm finds the largest size that an fr unit can be without exceeding the target size.
 * It must be called with a set of grid tracks and some quantity of space to fill.
 */
fun findSizeOfFr(tracks: List<GridTrack>, spaceToFill: Float): Float {
    // Handle the trivial case where there is no space to fill
    // Do not remove as otherwise the loop below will loop infinitely
    if (spaceToFill == 0f) {
        return 0f
    }

    // If the product of the hypothetical fr size (computed below) and any flexible track’s flex factor
    // is less than the track’s base size, then we must restart this algorithm treating all such tracks as inflexible.
    // We therefore wrap the entire algorithm in a loop, with an hypothetical_fr_size of INFINITY such that the above
    // condition can never be true for the first iteration.
    var hypotheticalFrSize = Float.POSITIVE_INFINITY
    var previousIterHypotheticalFrSize: Float
    while (true) {
        // Let leftover space be the space to fill minus the base sizes of the non-flexible grid tracks.
        // Let flex factor sum be the sum of the flex factors of the flexible tracks. If this value is less than 1, set it to 1 instead.
        // We compute both of these in a single loop to avoid iterating over the data twice
        var usedSpace = 0f
        var naiveFlexFactorSum = 0f
        for (track in tracks) {
            // Tracks for which flex_factor * hypothetical_fr_size < track.base_size are treated as inflexible
            if (track.maxTrackSizingFunction is MaxTrackSizingFunction.Fraction &&
                (track.maxTrackSizingFunction as MaxTrackSizingFunction.Fraction).f * hypotheticalFrSize >= track.baseSize
            ) {
                naiveFlexFactorSum += (track.maxTrackSizingFunction as MaxTrackSizingFunction.Fraction).f
            } else {
                usedSpace += track.baseSize
            }
        }
        val leftoverSpace = spaceToFill - usedSpace
        val flexFactor = f32Max(naiveFlexFactorSum, 1f)

        // Let the hypothetical fr size be the leftover space divided by the flex factor sum.
        previousIterHypotheticalFrSize = hypotheticalFrSize
        hypotheticalFrSize = leftoverSpace / flexFactor

        // If the product of the hypothetical fr size and a flexible track’s flex factor is less than the track’s base size,
        // restart this algorithm treating all such tracks as inflexible.
        // We keep track of the hypothetical_fr_size
        val hypotheticalFrSizeIsValid = tracks.all { track ->
            when (track.maxTrackSizingFunction) {
                is MaxTrackSizingFunction.Fraction -> {
                    flexFactor * hypotheticalFrSize >= track.baseSize
                            || flexFactor * previousIterHypotheticalFrSize < track.baseSize
                }

                else -> true
            }
        }
        if (hypotheticalFrSizeIsValid) {
            break
        }
    }

    // Return the hypothetical fr size.
    return hypotheticalFrSize
}

/**
 * 11.8. Stretch auto Tracks
 * This step expands tracks that have an auto max track sizing function by dividing any remaining positive, definite free space equally amongst them.
 */
fun stretchAutoTracks(
    axisTracks: List<GridTrack>,
    axisMinSize: Option<Float>,
    axisAvailableSpaceForExpansion: AvailableSpace,
) {
    val numAutoTracks =
        axisTracks.filter { track -> track.maxTrackSizingFunction is MaxTrackSizingFunction.Auto }.count()
    if (numAutoTracks > 0) {
        val usedSpace: Float = axisTracks.map { track -> track.baseSize }.sum()

        // If the free space is indefinite, but the grid container has a definite min-width/height
        // use that size to calculate the free space for this step instead.
        val freeSpace = if (axisAvailableSpaceForExpansion.isDefinite()) {
            axisAvailableSpaceForExpansion.computeFreeSpace(usedSpace)
        } else {
            when (axisMinSize) {
                is Option.Some -> axisMinSize.unwrap() - usedSpace
                is Option.None -> 0f
            }
        }
        if (freeSpace > 0f) {
            val extraSpacePerAutoTrack = freeSpace / numAutoTracks.toFloat()
            axisTracks
                .filter { track -> track.maxTrackSizingFunction == MaxTrackSizingFunction.Auto }
                .forEach { track -> track.baseSize += extraSpacePerAutoTrack }
        }
    }
}

/**
 * 11.7. Expand Flexible Tracks
 * This step sizes flexible tracks using the largest value it can assign to an fr without exceeding the available space.
 */
fun distributeSpaceUpToLimits(
    spaceToDistribute: Float,
    tracks: List<GridTrack>,
    trackIsAffected: (GridTrack) -> Boolean,
    trackDistributionProportion: (GridTrack) -> Float,
    trackAffectedProperty: (GridTrack) -> Float,
    trackLimit: (GridTrack) -> Float,
): Float {
    /// Define a small constant to avoid infinite loops due to rounding errors. Rather than stopping distributing
    /// extra space when it gets to exactly zero, we will stop when it falls below this amount
    val THRESHOLD = 0.000001f

    var spaceToDistribute = spaceToDistribute
    while (spaceToDistribute > THRESHOLD) {
        val trackDistributionProportionSum: Float = tracks

            .filter { track -> trackAffectedProperty(track) + track.itemIncurredIncrease < trackLimit(track) }
            .filter(trackIsAffected)
            .map(trackDistributionProportion)
            .sum()

        if (trackDistributionProportionSum == 0.0f) {
            break
        }

        // Compute item-incurred increase for this iteration
        val minIncreaseLimit = tracks
            .filter { track -> trackAffectedProperty(track) + track.itemIncurredIncrease < trackLimit(track) }
            .filter(trackIsAffected)
            .map { track -> (trackLimit(track) - trackAffectedProperty(track)) / trackDistributionProportion(track) }
            .minByRs { a, b -> a.compareTo(b) }
            .unwrap(); // We will never pass an empty track list to this function
        val iterationItemIncurredIncrease =
            f32Min(minIncreaseLimit, spaceToDistribute / trackDistributionProportionSum)

        for (track in tracks.filter(trackIsAffected)) {
            val increase = iterationItemIncurredIncrease * trackDistributionProportion(track)
            if (increase > 0f && trackAffectedProperty(track) + increase <= trackLimit(track)) {
                track.itemIncurredIncrease += increase
                spaceToDistribute -= increase
            }
        }
    }

    return spaceToDistribute
}
