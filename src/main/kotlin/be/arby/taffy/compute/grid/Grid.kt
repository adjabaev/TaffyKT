package be.arby.taffy.compute.grid

import be.arby.taffy.compute.GenericAlgorithm
import be.arby.taffy.compute.grid.types.CellOccupancyMatrix
import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.compute.grid.types.GridTrack
import be.arby.taffy.compute.grid.types.OriginZeroLine
import be.arby.taffy.geometry.Line
import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.geometry.extensions.*
import be.arby.taffy.layout.*
import be.arby.taffy.maths.axis.AbsoluteAxis
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.geom.InBothAbsAxis
import be.arby.taffy.maths.into
import be.arby.taffy.lang.*
import be.arby.taffy.node.Node
import be.arby.taffy.resolve.maybeResolveStS
import be.arby.taffy.resolve.resolveOrZeroOtRlp
import be.arby.taffy.resolve.resolveOrZeroOtRlpa
import be.arby.taffy.style.Display
import be.arby.taffy.style.Position
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.alignment.*
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.tree.layout.RunMode
import be.arby.taffy.util.maybeClamp
import be.arby.taffy.utils.findOptional
import be.arby.taffy.utils.next
import be.arby.taffy.utils.split
import java.util.*

class Grid {
    companion object {
        fun compute(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            runMode: RunMode
        ): SizeAndBaselines {
            val getChildStylesIter =
                { node: Node -> tree.children(node).map { childNode: Node -> tree.style(childNode) } }
            val style = tree.style(node)
            val childStylesIter = getChildStylesIter(node).iterator()

            // 1. Resolve the explicit grid
            // Exactly compute the number of rows and columns in the explicit grid.
            val explicitColCount = ExplicitGrid.computeExplicitGridSizeInAxis(style, AbsoluteAxis.HORIZONTAL)
            val explicitRowCount = ExplicitGrid.computeExplicitGridSizeInAxis(style, AbsoluteAxis.VERTICAL)

            // 2. Implicit Grid: Estimate Track Counts
            // Estimate the number of rows and columns in the implicit grid (= the entire grid)
            // This is necessary as part of placement. Doing it early here is a perf optimisation to reduce allocations.
            val (estColCounts, estRowCounts) =
                ImplicitGrid.computeGridSizeEstimate(explicitColCount, explicitRowCount, childStylesIter)

            // 2. Grid Item Placement
            // Match items (children) to a definite grid position (row start/end and column start/end position)
            var items = ArrayList<GridItem>()
            var cellOccupancyMatrix = CellOccupancyMatrix.withTrackCounts(estColCounts, estRowCounts)
            val inFlowChildrenIter = tree.children(node)
                .withIndex()
                .map { (index, childNode) -> Triple(index, childNode, tree.style(childNode)) }
                .filter { (_, _, style) -> style.display != Display.NONE && style.position != Position.ABSOLUTE }
            Placement.placeGridItems(
                cellOccupancyMatrix,
                items,
                inFlowChildrenIter,
                style.gridAutoFlow,
                style.alignItems.unwrapOr(AlignItems.STRETCH)
            )

            // Extract track counts from previous step (auto-placement can expand the number of tracks)
            val finalColCounts = cellOccupancyMatrix.trackCounts(AbsoluteAxis.HORIZONTAL)
            val finalRowCounts = cellOccupancyMatrix.trackCounts(AbsoluteAxis.VERTICAL)

            // 3. Initialize Tracks
            // Initialize (explicit and implicit) grid tracks (and gutters)
            // This resolves the min and max track sizing functions for all tracks and gutters
            val columns = ArrayList<GridTrack>()
            val rows = ArrayList<GridTrack>()
            ExplicitGrid.initializeGridTracks(
                columns, finalColCounts, style.gridTemplateColumns, style.gridAutoColumns, style.gap.width
            )
            { columnIndex -> cellOccupancyMatrix.columnIsOccupied(columnIndex) }

            ExplicitGrid.initializeGridTracks(
                rows, finalRowCounts, style.gridTemplateRows, style.gridAutoRows, style.gap.height
            )
            { rowIndex -> cellOccupancyMatrix.rowIsOccupied(rowIndex) }

            // 4. Compute "available grid space"
            // https://www.w3.org/TR/css-grid-1/#available-grid-space
            val padding = style.padding.resolveOrZeroOtRlp(parentSize.width)
            val border = style.border.resolveOrZeroOtRlp(parentSize.width)
            val margin = style.margin.resolveOrZeroOtRlpa(parentSize.width)
            val aspectRatio = style.aspectRatio
            val minSize = style.minSize.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)
            val maxSize = style.maxSize.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)
            val size = style.size.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)

            val constrainedAvailableSpace = size
                .maybeClamp(minSize, maxSize)
                .map { size -> AvailableSpace.from(size) }
                .unwrapOr(availableSpace.maybeClamp(minSize, maxSize))

            val availableGridSpace = Size(
                width = constrainedAvailableSpace
                    .width
                    .mapDefiniteValue { space -> space - padding.horizontalAxisSum() - border.horizontalAxisSum() },
                height = constrainedAvailableSpace
                    .height
                    .mapDefiniteValue { space -> space - padding.verticalAxisSum() - border.verticalAxisSum() },
            )

            val outerNodeSize = size.maybeClamp(minSize, maxSize).or(parentSize.maybeSub(margin.sumAxes()))
            val innerNodeSize = Size(
                width = outerNodeSize.width.map { space -> space - padding.horizontalAxisSum() - border.horizontalAxisSum() },
                height = outerNodeSize.height.map { space -> space - padding.verticalAxisSum() - border.verticalAxisSum() }
            )

            // 5. Track Sizing

            // Convert grid placements in origin-zero coordinates to indexes into the GridTrack (rows and columns) vectors
            // This computation is relatively trivial, but it requires the final number of negative (implicit) tracks in
            // each axis, and doing it up-front here means we don't have to keep repeating that calculation
            TrackSizing.resolveItemTrackIndexes(items, finalColCounts, finalRowCounts)

            // For each item, and in each axis, determine whether the item crosses any flexible (fr) tracks
            // Record this as a boolean (per-axis) on each item for later use in the track-sizing algorithm
            TrackSizing.determineIfItemCrossesFlexibleOrIntrinsicTracks(items, columns, rows)

            // Determine if the grid has any baseline aligned items
            val hasBaselineAlignedItem = items.any{ item -> item.alignSelf == AlignSelf.BASELINE }

            // Run track sizing algorithm for Inline axis
            TrackSizing.trackSizingAlgorithm(
                tree,
                AbstractAxis.INLINE,
                minSize.get(AbstractAxis.INLINE),
                maxSize.get(AbstractAxis.INLINE),
                style.gridAlignContent(AbstractAxis.BLOCK),
                availableGridSpace,
                innerNodeSize,
                columns,
                rows,
                items,
                { track: GridTrack, parentSize: Option<Float> ->
                    track.maxTrackSizingFunction.definiteValue(parentSize)
                },
                hasBaselineAlignedItem
            )

            val initialColumnSum = columns.map{ track -> track.baseSize }.sum()
            innerNodeSize.width = innerNodeSize.width.orElse { initialColumnSum.into() }

            items.forEach { item -> item.knownDimensionsCache = Option.None }

            // Run track sizing algorithm for Block axis
            TrackSizing.trackSizingAlgorithm(
                tree,
                AbstractAxis.BLOCK,
                minSize.get(AbstractAxis.BLOCK),
                maxSize.get(AbstractAxis.BLOCK),
                style.gridAlignContent(AbstractAxis.INLINE),
                availableGridSpace,
                innerNodeSize,
                rows,
                columns,
                items,
                { track: GridTrack, _ -> Option.Some(track.baseSize) },
                false // TODO: Support baseline alignment in the vertical axis
            )


            val initialRowSum = rows.map{ track -> track.baseSize }.sum()
            innerNodeSize.height = innerNodeSize.height.orElse { initialRowSum.into() }

            // 6. Compute container size
            val resolvedStyleSize = knownDimensions.or(style.size.maybeResolveStS(parentSize))
            val containerBorderBox = Size(
                width = resolvedStyleSize.get(AbstractAxis.INLINE)
                    .unwrapOr(initialColumnSum + padding.horizontalAxisSum() + border.horizontalAxisSum() ),
                height = resolvedStyleSize.get(AbstractAxis.BLOCK)
                    .unwrapOr(initialRowSum + padding.verticalAxisSum() + border.verticalAxisSum() )
            )
            val containerContentBox = Size(
                width = containerBorderBox.width - padding.horizontalAxisSum() - border.horizontalAxisSum(),
                height = containerBorderBox.height - padding.verticalAxisSum() - border.verticalAxisSum(),
            )

            // If only the container's size has been requested
            if (runMode == RunMode.COMPUTE_SIZE) {
                return containerBorderBox.intoSB()
            }

            // 7. Resolve percentage track base sizes
            // In the case of an indefinitely sized container these resolve to zero during the "Initialise Tracks" step
            // and therefore need to be re-resolved here based on the content-sized content box of the container
            if (!availableGridSpace.width.isDefinite()) {
                for (column in columns) {
                    val min: Option<Float> = column.minTrackSizingFunction.resolvedPercentageSize(containerContentBox.width)
                    val max: Option<Float> = column.maxTrackSizingFunction.resolvedPercentageSize(containerContentBox.width)
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
                        val knownDimensions = item.knownDimensions(
                            AbstractAxis.INLINE,
                            rows,
                            innerNodeSize.height
                        ) { track: GridTrack, _ -> Option.Some(track.baseSize) }

                        val newMinContentContribution = item.minContentContribution(
                            AbstractAxis.INLINE, tree,
                            knownDimensions, innerNodeSize)

                        val hasChanged = Option.Some(newMinContentContribution) != item.minContentContributionCache.width

                        item.knownDimensionsCache = Option.Some(knownDimensions)
                        item.minContentContributionCache.width = Option.Some(newMinContentContribution)
                        item.maxContentContributionCache.width = Option.None
                        item.minimumContributionCache.width = Option.None

                        hasChanged
                    }
                    .any { hasChanged -> hasChanged }

                rerunColumnSizing = minContentContributionChanged
            } else {
                // Clear intrinsic width caches
                items.forEach { item ->
                    item.knownDimensionsCache = Option.None
                    item.minContentContributionCache.width = Option.None
                    item.maxContentContributionCache.width = Option.None
                    item.minimumContributionCache.width = Option.None
                }
            }

            if (rerunColumnSizing) {
                // Re-run track sizing algorithm for Inline axis
                TrackSizing.trackSizingAlgorithm(
                    tree,
                    AbstractAxis.INLINE,
                    minSize.get(AbstractAxis.INLINE),
                    maxSize.get(AbstractAxis.INLINE),
                    style.gridAlignContent(AbstractAxis.BLOCK),
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
                            val knownDimensions = item.knownDimensions(
                                AbstractAxis.BLOCK,
                                columns,
                                innerNodeSize.width
                            ) { track: GridTrack, _ -> Option.Some(track.baseSize) }

                            val newMinContentContribution = item.minContentContribution(
                                AbstractAxis.BLOCK, tree,
                                knownDimensions, innerNodeSize)

                            val hasChanged = Option.Some(newMinContentContribution) != item.minContentContributionCache.height

                            item.knownDimensionsCache = Option.Some(knownDimensions)
                            item.minContentContributionCache.height = Option.Some(newMinContentContribution)
                            item.maxContentContributionCache.height = Option.None
                            item.minimumContributionCache.height = Option.None

                            hasChanged
                        }
                        .any { hasChanged -> hasChanged }

                    rerunRowSizing = minContentContributionChanged
                } else {
                    items.forEach { item ->
                        // Clear intrinsic height caches
                        item.knownDimensionsCache = Option.None
                        item.minContentContributionCache.height = Option.None
                        item.maxContentContributionCache.height = Option.None
                        item.minimumContributionCache.height = Option.None
                    }
                }

                if (rerunRowSizing) {
                    // Re-run track sizing algorithm for Block axis
                    TrackSizing.trackSizingAlgorithm(
                        tree,
                        AbstractAxis.BLOCK,
                        minSize.get(AbstractAxis.BLOCK),
                        maxSize.get(AbstractAxis.BLOCK),
                        style.gridAlignContent(AbstractAxis.INLINE),
                        availableGridSpace,
                        innerNodeSize,
                        rows,
                        columns,
                        items,
                        { track: GridTrack, _ -> Option.Some(track.baseSize) },
                        false // TODO: Support baseline alignment in the vertical axis
                    )
                }
            }

            // 8. Track Alignment

            // Align columns
            Alignment.alignTracks(
                containerContentBox.get(AbstractAxis.INLINE),
                Line(start = padding.left, end = padding.right),
                Line(start = border.left, end = border.right),
                columns,
                style.justifyContent.unwrapOr(AlignContent.STRETCH),
            )
            // Align rows
            Alignment.alignTracks(
                containerContentBox.get(AbstractAxis.BLOCK),
                Line(start = padding.top, end = padding.bottom),
                Line(start = border.top, end = border.bottom),
                rows,
                style.alignContent.unwrapOr(AlignContent.STRETCH),
            )

            // 9. Size, Align, and Position Grid Items

            // Sort items back into original order to allow them to be matched up with styles
            items.sortBy { item -> item.sourceOrder }

            val containerAlignmentStyles = InBothAbsAxis(horizontal = style.justifyItems, vertical = style.alignItems)

            // Position in-flow children (stored in items vector)
            for ((index, item) in items.withIndex()) {
                val gridArea = Rect(
                    top = rows[item.rowIndexes.start + 1].offset,
                    bottom = rows[item.rowIndexes.end].offset,
                    left = columns[item.columnIndexes.start + 1].offset,
                    right = columns[item.columnIndexes.end].offset,
                )
                Alignment.alignAndPositionItem(
                    tree,
                    item.node,
                    index,
                    gridArea,
                    containerAlignmentStyles,
                    item.baselineShim
                )
            }

            // Position hidden and absolutely positioned children
            var order = items.size
            (0 until tree.childCount(node)).forEach { index ->
                val child = tree.child(node, index)
                val childStyle = tree.style(child)

                // Position hidden child
                if (childStyle.display == Display.NONE) {
                    tree.layout(node, Layout.withOrder(order))
                    GenericAlgorithm.performLayout(
                        tree,
                        child,
                        Size.none(),
                        Size.none(),
                        Size.MAX_CONTENT,
                        SizingMode.INHERENT_SIZE
                    )
                    order += 1
                    return@forEach
                }

                // Position absolutely positioned child
                if (childStyle.position == Position.ABSOLUTE) {
                    // Convert grid-col-{start/end} into Option's of indexes into the columns vector
                    // The Option is None if the style property is Auto and an unresolvable Span
                    val maybeColIndexes = childStyle
                        .gridColumn
                        .intoOriginZero(finalColCounts.explicit)
                        .resolveAbsolutelyPositionedGridTracks()
                        .map { maybeGridLine -> maybeGridLine.map { line: OriginZeroLine -> line.intoTrackVecIndex(finalColCounts) } }
                    // Convert grid-row-{start/end} into Option's of indexes into the row vector
                    // The Option is None if the style property is Auto and an unresolvable Span
                    val maybeRowIndexes = childStyle
                        .gridRow
                        .intoOriginZero(finalRowCounts.explicit)
                        .resolveAbsolutelyPositionedGridTracks()
                        .map { maybeGridLine -> maybeGridLine.map { line: OriginZeroLine -> line.intoTrackVecIndex(finalRowCounts) } }

                    val gridArea = Rect(
                        top = maybeRowIndexes.start.map { index -> rows[index].offset }.unwrapOr(border.top),
                        bottom = maybeRowIndexes.end.map { index -> rows[index].offset }.unwrapOr(containerBorderBox.height - border.bottom),
                        left = maybeColIndexes.start.map { index -> columns[index].offset }.unwrapOr(border.left),
                        right = maybeColIndexes.end.map { index -> columns[index].offset }.unwrapOr(containerBorderBox.width - border.right),
                    )

                    // TODO: Baseline alignment support for absolutely positioned items (should check if is actually specified)
                    Alignment.alignAndPositionItem(
                        tree,
                        child,
                        order,
                        gridArea,
                        containerAlignmentStyles,
                        0f
                    )
                    order += 1
                }
            }

            // If there are not items then return just the container size (no baseline)
            if (items.isEmpty()) {
                return SizeAndBaselines(size = containerBorderBox, firstBaselines = Point.NONE)
            }

            // Determine the grid container baseline(s) (currently we only compute the first baseline)
            var gridContainerBaseline: Float

            // Sort items by row start position so that we can iterate items in groups which are in the same row
            items.sortBy { item -> item.rowIndexes.start }

            // Get the row index of the first row containing items
            val firstRow = items[0].rowIndexes.start

            // Create a slice of all of the items start in this row (taking advantage of the fact that we have just sorted the array)
            val firstRowItems = items.slice(items.indices).split { item -> item.rowIndexes.start != firstRow }.next().unwrap()

            // Check if any items in *this row* are baseline aligned
            val rowHasBaselineItem = firstRowItems.any { item -> item.alignSelf == AlignSelf.BASELINE }

            val item = if (rowHasBaselineItem) {
                firstRowItems.findOptional { item -> item.alignSelf == AlignSelf.BASELINE }.unwrap()
            } else {
                firstRowItems[0]
            }

            val layout = tree.layout(item.node)
            gridContainerBaseline = layout.location.y + item.baseline.unwrapOr(layout.size.height)

            return SizeAndBaselines (
                size = containerBorderBox,
                firstBaselines = Point(x = Option.None, y = Option.Some(gridContainerBaseline))
            )
        }
    }
}
