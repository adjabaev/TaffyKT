package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.CellOccupancyMatrix
import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.compute.grid.types.GridTrack
import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.geom.*
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.collections.*
import be.arby.taffy.lang.f32Max
import be.arby.taffy.lang.max
import be.arby.taffy.lang.tuples.T3
import be.arby.taffy.maths.into
import be.arby.taffy.style.BoxGenerationMode
import be.arby.taffy.style.BoxSizing
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.Position
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.alignment.JustifyContent
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.layout.*
import be.arby.taffy.tree.traits.LayoutGridContainer
import be.arby.taffy.util.maybeAdd
import be.arby.taffy.util.maybeClamp
import be.arby.taffy.util.maybeMax
import be.arby.taffy.util.maybeSub

/**
 * Grid layout algorithm
 * This consists of a few phases:
 *   - Resolving the explicit grid
 *   - Placing items (which also resolves the implicit grid)
 *   - Track (row/column) sizing
 *   - Alignment & Final item placement
 */
fun computeGridLayout(tree: LayoutGridContainer, node: Int, inputs: LayoutInput): LayoutOutput {
    val knownDimensions = inputs.knownDimensions
    val parentSize = inputs.parentSize
    val availableSpace = inputs.availableSpace
    val runMode = inputs.runMode

    val style = tree.getGridContainerStyle(node)

    // 1. Compute "available grid space"
    // https://www.w3.org/TR/css-grid-1/#available-grid-space
    val aspectRatio = style.aspectRatio()
    val padding = style.padding().resolveOrZero(parentSize.width)
    val border = style.border().resolveOrZero(parentSize.width)
    val paddingBorder = padding + border
    val paddingBorderSize = paddingBorder.sumAxes()
    val boxSizingAdjustment =
    if (style.boxSizing() == BoxSizing.CONTENT_BOX) paddingBorderSize else Size.ZERO.clone()

    val minSize = style
            .minSize()
            .maybeResolve(parentSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
    val maxSize = style
            .maxSize()
        .maybeResolve(parentSize)
        .maybeApplyAspectRatio(aspectRatio)
        .maybeAdd(boxSizingAdjustment)
    val preferredSize = if (inputs.sizingMode == SizingMode.INHERENT_SIZE) {
        style
            .size()
            .maybeResolve(parentSize)
            .maybeApplyAspectRatio(style.aspectRatio())
            .maybeAdd(boxSizingAdjustment)
    } else {
        Size.NONE.clone()
    }

    // Scrollbar gutters are reserved when the `overflow` property is set to `Overflow.Scroll`.
    // However, the axis are switched (transposed) because a node that scrolls vertically needs
    // *horizontal* space to be reserved for a scrollbar
    val scrollbarGutter = style.overflow().transpose().map { overflow ->
        when (overflow) {
            Overflow.SCROLL -> style.scrollbarWidth()
            else -> 0f
        }
    }
    // TODO: make side configurable based on the `direction` property
    var contentBoxInset = paddingBorder
    contentBoxInset.right += scrollbarGutter.x
    contentBoxInset.bottom += scrollbarGutter.y

    val alignContent = style.alignContent().unwrapOr(AlignContent.STRETCH)
    val justifyContent = style.justifyContent().unwrapOr(JustifyContent.STRETCH)
    val alignItems = style.alignItems()
    val justifyItems = style.justifyItems()

    // Note: we avoid accessing the grid rows/columns methods more than once as this can
    // cause an expensive-ish computation
    val gridTemplateColumms = style.gridTemplateColumns()
    val gridTemplateRows = style.gridTemplateRows()
    val gridAutoColumms = style.gridAutoColumns()
    val gridAutoRows = style.gridAutoRows()

    val constrainedAvailableSpace = knownDimensions
            .or(preferredSize)
            .map { size -> size.map { v -> AvailableSpace.fromLength(v) } }
            .unwrapOr(availableSpace)
            .maybeClamp(minSize, maxSize)
            .maybeMax(paddingBorderSize)

    val availableGridSpace = Size(
        width = constrainedAvailableSpace
            .width
            .mapDefiniteValue { space -> space - contentBoxInset.horizontalAxisSum() },
        height = constrainedAvailableSpace
            .height
            .mapDefiniteValue { space -> space - contentBoxInset.verticalAxisSum() }
    )

    val outerNodeSize =
        knownDimensions.or(preferredSize).maybeClamp(minSize, maxSize).maybeMax(paddingBorderSize)
    var innerNodeSize = Size(
        width = outerNodeSize.width.map { space -> space - contentBoxInset.horizontalAxisSum() },
        height = outerNodeSize.height.map { space -> space - contentBoxInset.verticalAxisSum() }
    )

    if (runMode == RunMode.COMPUTE_SIZE && outerNodeSize.width.isSome() && outerNodeSize.height.isSome()) {
        return LayoutOutput.fromOuterSize(Size(outerNodeSize.width.unwrap(), outerNodeSize.height.unwrap()))
    }

    val getChildStylesIter = { node: Int ->
        tree.childIds(node).map { childNode: Int ->
            tree.getGridChildStyle(childNode)
        }
    }
    val childStylesIter = getChildStylesIter(node)

    // 2. Resolve the explicit grid

    // This is very similar to the inner_node_size except if the inner_node_size is not definite but the node
    // has a min- or max- size style then that will be used in it's place.
    val autoFitContainerSize = outerNodeSize
            .or(maxSize)
            .or(minSize)
            .maybeClamp(minSize, maxSize)
            .maybeMax(paddingBorderSize)
            .maybeSub(contentBoxInset.sumAxes())

    // Exactly compute the number of rows and columns in the explicit grid.
    val explicitColCount = computeExplicitGridSizeInAxis(
        style,
        gridTemplateColumms,
        autoFitContainerSize,
        AbsoluteAxis.HORIZONTAL
    )
    val explicitRowCount = computeExplicitGridSizeInAxis(
        style,
        gridTemplateRows,
        autoFitContainerSize,
        AbsoluteAxis.VERTICAL
    )

    // 3. Implicit Grid: Estimate Track Counts
    // Estimate the number of rows and columns in the implicit grid (= the entire grid)
    // This is necessary as part of placement. Doing it early here is a perf optimisation to reduce allocations.
    val (estColCounts, estRowCounts) = computeGridSizeEstimate(explicitColCount, explicitRowCount, childStylesIter.iter())

    // 4. Grid Item Placement
    // Match items (children) to a definite grid position (row start/end and column start/end position)
    val items = mutableListOf<GridItem>()
    var cellOccupancyMatrix = CellOccupancyMatrix.withTrackCounts(estColCounts, estRowCounts)
    val inFlowChildrenIter = {
        tree.childIds(node)
            .enumerate()
            .map { (index, childNode) ->
                T3(index, childNode, tree.getGridChildStyle(childNode))
            }
            .filter { (a, b, style) ->
                style.boxGenerationMode() != BoxGenerationMode.NONE && style.position() != Position.ABSOLUTE
            }
    }
    placeGridItems(
        cellOccupancyMatrix,
        items,
        inFlowChildrenIter,
        style.gridAutoFlow(),
        alignItems.unwrapOr(AlignItems.STRETCH),
        justifyItems.unwrapOr(AlignItems.STRETCH)
    )

    // Extract track counts from previous step (auto-placement can expand the number of tracks)
    val finalColCounts = cellOccupancyMatrix.trackCounts(AbsoluteAxis.HORIZONTAL)
    val finalRowCounts = cellOccupancyMatrix.trackCounts(AbsoluteAxis.VERTICAL)

    // 5. Initialize Tracks
    // Initialize (explicit and implicit) grid tracks (and gutters)
    // This resolves the min and max track sizing functions for all tracks and gutters
    val columns = mutableListOf<GridTrack>()
    val rows = mutableListOf<GridTrack>()
    initializeGridTracks(
        columns,
        finalColCounts,
        gridTemplateColumms,
        gridAutoColumms,
        style.gap().width,
        { columnIndex -> cellOccupancyMatrix.columnIsOccupied(columnIndex) }
    )
    initializeGridTracks(
        rows,
        finalRowCounts,
        gridTemplateRows,
        gridAutoRows,
        style.gap().height,
        { rowIndex -> cellOccupancyMatrix.rowIsOccupied(rowIndex) }
    )

    // 6. Track Sizing

    // Convert grid placements in origin-zero coordinates to indexes into the GridTrack (rows and columns) vectors
    // This computation is relatively trivial, but it requires the final number of negative (implicit) tracks in
    // each axis, and doing it up-front here means we don't have to keep repeating that calculation
    resolveItemTrackIndexes(items, finalColCounts, finalRowCounts)

    // For each item, and in each axis, determine whether the item crosses any flexible (fr) tracks
    // Record this as a boolean (per-axis) on each item for later use in the track-sizing algorithm
    determineIfItemCrossesFlexibleOrIntrinsicTracks(items, columns, rows)

    // Determine if the grid has any baseline aligned items
    val hasBaselineAlignedItem = items.any { item ->
        item.alignSelf == AlignSelf.BASELINE
    }

    // Run track sizing algorithm for Inline axis
    trackSizingAlgorithm(
        tree,
        AbstractAxis.INLINE,
        minSize.get(AbstractAxis.INLINE),
        maxSize.get(AbstractAxis.INLINE),
        alignContent,
        availableGridSpace,
        innerNodeSize,
        columns,
        rows,
        items,
        { track: GridTrack, parentSize: Option<Float> -> track.maxTrackSizingFunction.definiteValue(parentSize) },
        hasBaselineAlignedItem,
    )
    val initialColumnSum = columns.map { track -> track.baseSize }.sum()
    innerNodeSize.width = innerNodeSize.width.orElse { initialColumnSum.into() }

    items.forEach { item -> item.availableSpaceCache = Option.None }

    // Run track sizing algorithm for Block axis
    trackSizingAlgorithm(
        tree,
        AbstractAxis.BLOCK,
        minSize.get(AbstractAxis.BLOCK),
        maxSize.get(AbstractAxis.BLOCK),
        justifyContent,
        availableGridSpace,
        innerNodeSize,
        rows,
        columns,
        items,
        { track: GridTrack, _ -> Option.Some(track.baseSize) },
        false // TODO: Support baseline alignment in the vertical axis
    )
    val initialRowSum = rows.map { track -> track.baseSize }.sum()
    innerNodeSize.height = innerNodeSize.height.orElse { initialRowSum.into() }

    // 6. Compute container size
    val resolvedStyleSize = knownDimensions.or(preferredSize)
    val containerBorderBox = Size(
        width = resolvedStyleSize
        .get(AbstractAxis.INLINE)
        .unwrapOrElse { initialColumnSum + contentBoxInset.horizontalAxisSum() }
        .maybeClamp(minSize.width, maxSize.width)
        .max(paddingBorderSize.width),
        height = resolvedStyleSize
        .get(AbstractAxis.BLOCK)
        .unwrapOrElse { initialRowSum + contentBoxInset.verticalAxisSum() }
        .maybeClamp(minSize.height, maxSize.height)
        .max(paddingBorderSize.height)
    )
    val containerContentBox = Size(
        width = f32Max(0f, containerBorderBox.width - contentBoxInset.horizontalAxisSum()),
        height = f32Max(0f, containerBorderBox.height - contentBoxInset.verticalAxisSum())
    )

    // If only the container's size has been requested
    if (runMode == RunMode.COMPUTE_SIZE) {
        return LayoutOutput.fromOuterSize(containerBorderBox)
    }

    // 7. Resolve percentage track base sizes
    // In the case of an indefinitely sized container these resolve to zero during the "Initialise Tracks" step
    // and therefore need to be re-resolved here based on the content-sized content box of the container
    if (!availableGridSpace.width.isDefinite()) {
        for (column in columns) {
            val min: Option<Float> =
                column.minTrackSizingFunction.resolvedPercentageSize(containerContentBox.width)
            val max: Option<Float> =
                column.maxTrackSizingFunction.resolvedPercentageSize(containerContentBox.width)
            column.baseSize = column.baseSize.maybeClamp(min, max)
        }
    }
    if (!availableGridSpace.height.isDefinite()) {
        for (row in rows) {
            val min: Option<Float> = row.minTrackSizingFunction.resolvedPercentageSize(containerContentBox.height)
            val max: Option<Float> = row.maxTrackSizingFunction.resolvedPercentageSize(containerContentBox.height)
            row.baseSize = row.baseSize.maybeClamp(min, max)
        }
    }

    // Column sizing must be re-run (once) if:
    //   - The grid container's width was initially indefinite and there are any columns with percentage track sizing functions
    //   - Any grid item crossing an intrinsically sized track's min content contribution width has changed
    // TODO: Only rerun sizing for tracks that actually require it rather than for all tracks if any need it.
    var rerunColumnSizing: Boolean

    val hasPercentageColumn = columns.any { track -> track.usesPercentage() }
    val parentWidthIndefinite = !availableSpace.width.isDefinite()
    rerunColumnSizing = parentWidthIndefinite && hasPercentageColumn

    if (!rerunColumnSizing) {
        val minContentContributionChanged = items
            .filter { item -> item.crossesIntrinsicColumn }
            .map { item ->
                val availableSpace = item.availableSpace(
                        AbstractAxis.INLINE,
                        rows,
                innerNodeSize.height,
                    { track: GridTrack, _ -> Option.Some(track.baseSize) },
                )
                val newMinContentContribution =
                item.minContentContribution(AbstractAxis.INLINE, tree, availableSpace, innerNodeSize)

                val hasChanged = Option.Some(newMinContentContribution) != item.minContentContributionCache.width

                item.availableSpaceCache = Option.Some(availableSpace)
                item.minContentContributionCache.width = Option.Some(newMinContentContribution)
                item.maxContentContributionCache.width = Option.None
                item.minimumContributionCache.width = Option.None

                hasChanged
            }
        .any { hasChanged -> hasChanged }
        rerunColumnSizing = minContentContributionChanged
    } else {
        // Clear intrisic width caches
        items.forEach { item ->
            item.availableSpaceCache = Option.None
            item.minContentContributionCache.width = Option.None
            item.maxContentContributionCache.width = Option.None
            item.minimumContributionCache.width = Option.None
        }
    }

    if (rerunColumnSizing) {
        // Re-run track sizing algorithm for Inline axis
        trackSizingAlgorithm(
            tree,
            AbstractAxis.INLINE,
            minSize.get(AbstractAxis.INLINE),
            maxSize.get(AbstractAxis.INLINE),
            alignContent,
            availableGridSpace,
            innerNodeSize,
            columns,
            rows,
            items,
            { track: GridTrack, _ -> Option.Some(track.baseSize) },
            hasBaselineAlignedItem
        )

        // Row sizing must be re-run (once) if:
        //   - The grid container's height was initially indefinite and there are any rows with percentage track sizing functions
        //   - Any grid item crossing an intrinsically sized track's min content contribution height has changed
        // TODO: Only rerun sizing for tracks that actually require it rather than for all tracks if any need it.
        var rerunRowSizing: Boolean

        val hasPercentageRow = rows.any { track -> track.usesPercentage() }
        val parentHeightIndefinite = !availableSpace.height.isDefinite()
        rerunRowSizing = parentHeightIndefinite && hasPercentageRow

        if (!rerunRowSizing) {
            val minContentContributionChanged = items
                .filter { item -> item.crossesIntrinsicColumn }
                .map { item ->
                    val availableSpace = item.availableSpace(
                            AbstractAxis.BLOCK,
                            columns,
                    innerNodeSize.width,
                        { track: GridTrack, _ -> Option.Some(track.baseSize) },
                    )
                    val newMinContentContribution =
                    item.minContentContribution(AbstractAxis.BLOCK, tree, availableSpace, innerNodeSize)

                    val hasChanged = Option.Some(newMinContentContribution) != item.minContentContributionCache.height

                    item.availableSpaceCache = Option.Some(availableSpace)
                    item.minContentContributionCache.height = Option.Some(newMinContentContribution)
                    item.maxContentContributionCache.height = Option.None
                    item.minimumContributionCache.height = Option.None

                    hasChanged
                }
                .any { hasChanged -> hasChanged }
                rerunRowSizing = minContentContributionChanged
        } else {
            items.forEach { item ->
                // Clear intrisic height caches
                item.availableSpaceCache = Option.None
                item.minContentContributionCache.height = Option.None
                item.maxContentContributionCache.height = Option.None
                item.minimumContributionCache.height = Option.None
            }
        }

        if (rerunRowSizing) {
            // Re-run track sizing algorithm for Block axis
            trackSizingAlgorithm(
                tree,
                AbstractAxis.BLOCK,
                minSize.get(AbstractAxis.BLOCK),
                maxSize.get(AbstractAxis.BLOCK),
                justifyContent,
                availableGridSpace,
                innerNodeSize,
                rows,
                columns,
                items,
                { track: GridTrack, _ -> Option.Some(track.baseSize) },
                false, // TODO: Support baseline alignment in the vertical axis
            )
        }
    }

    // 8. Track Alignment

    // Align columns
    alignTracks(
        containerContentBox.get(AbstractAxis.INLINE),
        Line(start = padding.left, end = padding.right),
        Line(start = border.left, end = border.right),
         columns,
        justifyContent
    )
    // Align rows
    alignTracks(
        containerContentBox.get(AbstractAxis.BLOCK),
        Line(start = padding.top, end = padding.bottom),
        Line(start = border.top, end = border.bottom),
        rows,
        alignContent
    )

    // 9. Size, Align, and Position Grid Items

    var itemContentSizeContribution = Size.ZERO.clone()

    // Sort items back into original order to allow them to be matched up with styles
    items.sortByKey { item -> item.sourceOrder }

    val containerAlignmentStyles = InBothAbsAxis(horizontal = justifyItems, vertical = alignItems)

    // Position in-flow children (stored in items vector)
    for ((index, item) in items.enumerate()) {
        val gridArea = Rect(
            top = rows[item.rowIndexes.start + 1].offset,
            bottom = rows[item.rowIndexes.end].offset,
            left = columns[item.columnIndexes.start + 1].offset,
            right = columns[item.columnIndexes.end].offset,
        )
        val (contentSizeContribution, yPosition, height) = alignAndPositionItem(
            tree,
            item.node,
            index,
            gridArea,
            containerAlignmentStyles,
            item.baselineShim
        )
        item.yPosition = yPosition
        item.height = height

        itemContentSizeContribution = itemContentSizeContribution.f32Max(contentSizeContribution)
    }

    // Position hidden and absolutely positioned children
    var order = items.len()
    (0 until tree.childCount(node)).forEach { index ->
        val child = tree.getChildId(node, index)
        val childStyle = tree.getGridChildStyle(child)

        // Position hidden child
        if (childStyle.boxGenerationMode() == BoxGenerationMode.NONE) {
            tree.setUnroundedLayout(child, Layout.withOrder(order))
            tree.performChildLayout(
                child,
                Size.NONE.clone(),
                Size.NONE.clone(),
                Size.MAX_CONTENT,
                SizingMode.INHERENT_SIZE,
                Line.FALSE,
            )
            order += 1
            return@forEach
        }

        // Position absolutely positioned child
        if (childStyle.position() == Position.ABSOLUTE) {
            // Convert grid-col-{start/end} into Option's of indexes into the columns vector
            // The Option is None if the style property is Auto and an unresolvable Span
            val maybeColIndexes = childStyle
                    .gridColumn()
                .intoOriginZero(finalColCounts.explicit)
                .resolveAbsolutelyPositionedGridTracks()
                .map { maybeGridLine ->
                    maybeGridLine.map { line: OriginZeroLine -> line.intoTrackVecIndex(finalColCounts) }
                }
                // Convert grid-row-{start/end} into Option's of indexes into the row vector
                // The Option is None if the style property is Auto and an unresolvable Span
                val maybeRowIndexes = childStyle
                        .gridRow()
                    .intoOriginZero(finalRowCounts.explicit)
                    .resolveAbsolutelyPositionedGridTracks()
                    .map { maybeGridLine ->
                    maybeGridLine.map { line: OriginZeroLine -> line.intoTrackVecIndex(finalRowCounts) }
                }

                val gridArea = Rect (
                    top = maybeRowIndexes.start.map { index -> rows[index].offset }.unwrapOr(border.top),
                    bottom = maybeRowIndexes.end.map { index -> rows[index].offset }
                        .unwrapOr(containerBorderBox.height - border.bottom - scrollbarGutter.y),
                    left = maybeColIndexes.start.map { index -> columns[index].offset }.unwrapOr(border.left),
                    right = maybeColIndexes.end.map { index -> columns[index].offset }
                        .unwrapOr(containerBorderBox.width - border.right - scrollbarGutter.x)
                )

            // TODO: Baseline alignment support for absolutely positioned items (should check if is actuallty specified)
            val (content_size_contribution, _, _) =
                alignAndPositionItem(tree, child, order, gridArea, containerAlignmentStyles, 0f)
            itemContentSizeContribution = itemContentSizeContribution.f32Max(content_size_contribution)

            order += 1
        }
    }

    // If there are not items then return just the container size (no baseline)
    if (items.isEmpty()) {
        return LayoutOutput.fromOuterSize(containerBorderBox)
    }

    // Determine the grid container baseline(s) (currently we only compute the first baseline)
    val gridContainerBaseline: Float = run {
        // Sort items by row start position so that we can iterate items in groups which are in the same row
        items.sortByKey { item -> item.rowIndexes.start }

        // Get the row index of the first row containing items
        val firstRow = items[0].rowIndexes.start

        // Create a slice of all of the items start in this row (taking advantage of the fact that we have just sorted the array)
        val firstRowItems = items.slice(items.indices).split { item -> item.rowIndexes.start != firstRow }.next().unwrap()

        // Check if any items in *this row* are baseline aligned
        val rowHasBaselineItem = firstRowItems.any { item -> item.alignSelf == AlignSelf.BASELINE }

        val item = if (rowHasBaselineItem) {
        firstRowItems.findRust { item -> item.alignSelf == AlignSelf.BASELINE }.unwrap()
    } else {
        firstRowItems[0]
    }

        item.yPosition + item.baseline.unwrapOr(item.height)
    }

    return LayoutOutput.fromSizesAndBaselines(
        containerBorderBox,
        itemContentSizeContribution,
        Point( x = Option.None, y = Option.Some(gridContainerBaseline))
    )
}
