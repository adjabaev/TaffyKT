package be.arby.taffy.compute.grid.types

import be.arby.taffy.geom.*
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.collections.get
import be.arby.taffy.lang.collections.len
import be.arby.taffy.lang.collections.sum
import be.arby.taffy.style.BoxSizing
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.grid.GridItemStyle
import be.arby.taffy.style.grid.MinTrackSizingFunction
import be.arby.taffy.tree.layout.SizingMode
import be.arby.taffy.tree.traits.LayoutPartialTree
import be.arby.taffy.util.maybeAdd
import be.arby.taffy.util.maybeClamp
import be.arby.taffy.util.maybeMin
import be.arby.taffy.util.maybeSub

/**
 * Represents a single grid item
 */
data class GridItem(
    /**
     * The id of the node that this item represents
     */
    val node: Int,

    /**
     * The order of the item in the children array
     * We sort the list of grid items during track sizing. This field allows us to sort back the original order
     * for final positioning
     */
    val sourceOrder: Int,

    /**
     * The item's definite row-start and row-end, as resolved by the placement algorithm
     * (in origin-zero coordinates)
     */
    val row: Line<OriginZeroLine>,
    /**
     * The items definite column-start and column-end, as resolved by the placement algorithm
     * (in origin-zero coordinates)
     */
    val column: Line<OriginZeroLine>,

    /**
     * The item's overflow style
     */
    val overflow: Point<Overflow>,
    /**
     * The item's box_sizing style
     */
    val boxSizing: BoxSizing,
    /**
     * The item's size style
     */
    val size: Size<Dimension>,
    /**
     * The item's min_size style
     */
    val minSize: Size<Dimension>,
    /**
     * The item's max_size style
     */
    val maxSize: Size<Dimension>,
    /**
     * The item's aspect_ratio style
     */
    val aspectRatio: Option<Float>,
    /**
     * The item's padding style
     */
    val padding: Rect<LengthPercentage>,
    /**
     * The item's border style
     */
    val border: Rect<LengthPercentage>,
    /**
     * The item's margin style
     */
    val margin: Rect<LengthPercentageAuto>,
    /**
     * The item's align_self property, or the parent's align_items property is not set
     */
    val alignSelf: AlignSelf,
    /**
     * The item's justify_self property, or the parent's justify_items property is not set
     */
    val justifySelf: AlignSelf,
    /**
     * The items first baseline (horizontal)
     */
    var baseline: Option<Float>,
    /**
     * Shim for baseline alignment that acts like an extra top margin
     * TODO: Support last baseline and vertical text baselines
     */
    var baselineShim: Float,

    /**
     * The item's definite row-start and row-end (same as `row` field, except in a different coordinate system)
     * (as indexes into the Vec<GridTrack> stored in a grid's AbstractAxisTracks)
     */
    var rowIndexes: Line<Int>,
    /**
     * The items definite column-start and column-end (same as `column` field, except in a different coordinate system)
     * (as indexes into the Vec<GridTrack> stored in a grid's AbstractAxisTracks)
     */
    var columnIndexes: Line<Int>,

    /**
     * Whether the item crosses a flexible row
     */
    var crossesFlexibleRow: Boolean,
    /**
     * Whether the item crosses a flexible column
     */
    var crossesFlexibleColumn: Boolean,
    /**
     * Whether the item crosses a intrinsic row
     */
    var crossesIntrinsicRow: Boolean,
    /**
     * Whether the item crosses a intrinsic column
     */
    var crossesIntrinsicColumn: Boolean,

    // Caches for intrinsic size computation. These caches are only valid for a single run of the track-sizing algorithm.
    /**
     * Cache for the known_dimensions input to intrinsic sizing computation
     */
    var availableSpaceCache: Option<Size<Option<Float>>>,
    /**
     * Cache for the min-content size
     */
    val minContentContributionCache: Size<Option<Float>>,
    /**
     * Cache for the minimum contribution
     */
    val minimumContributionCache: Size<Option<Float>>,
    /**
     * Cache for the max-content size
     */
    val maxContentContributionCache: Size<Option<Float>>,

    /**
     * Final y position. Used to compute baseline alignment for the container.
     */
    var yPosition: Float,
    /**
     * Final height. Used to compute baseline alignment for the container.
     */
    var height: Float
) {
    /**
     * This item's placement in the specified axis in OriginZero coordinates
     */
    fun placement(axis: AbstractAxis): Line<OriginZeroLine> {
        return when (axis) {
            AbstractAxis.BLOCK -> row
            AbstractAxis.INLINE -> column
        }
    }

    /**
     * This item's placement in the specified axis as GridTrackVec indices
     */
    fun placementIndexes(axis: AbstractAxis): Line<Int> {
        return when (axis) {
            AbstractAxis.BLOCK -> rowIndexes
            AbstractAxis.INLINE -> columnIndexes
        }
    }

    /**
     * Returns a range which can be used as an index into the GridTrackVec in the specified axis
     * which will produce a sub-slice of covering all the tracks and lines that this item spans
     * excluding the lines that bound it.
     */
    fun trackRangeExcludingLines(axis: AbstractAxis): IntRange {
        val indexes = placementIndexes(axis)
        return indexes.start + 1 until indexes.end
    }

    /**
     * Returns the number of tracks that this item spans in the specified axis
     */
    fun span(axis: AbstractAxis): Int {
        return when (axis) {
            AbstractAxis.BLOCK -> row.span()
            AbstractAxis.INLINE -> column.span()
        }
    }

    /**
     * Returns the pre-computed value indicating whether the grid item crosses a flexible track in
     * the specified axis
     */
    fun crossesFlexibleTrack(axis: AbstractAxis): Boolean {
        return when (axis) {
            AbstractAxis.INLINE -> crossesFlexibleColumn
            AbstractAxis.BLOCK -> crossesFlexibleRow
        }
    }

    /**
     * Returns the pre-computed value indicating whether the grid item crosses an intrinsic track in
     * the specified axis
     */
    fun crossesIntrinsicTrack(axis: AbstractAxis): Boolean {
        return when (axis) {
            AbstractAxis.INLINE -> crossesIntrinsicColumn
            AbstractAxis.BLOCK -> crossesIntrinsicRow
        }
    }

    /**
     * For an item spanning multiple tracks, the upper limit used to calculate its limited min-/max-content contribution is the
     * sum of the fixed max track sizing functions of any tracks it spans, and is applied if it only spans such tracks.
     */
    fun spannedTrackLimit(
        axis: AbstractAxis,
        axisTracks: MutableList<GridTrack>,
        axisParentSize: Option<Float>
    ): Option<Float> {
        val spannedTracks = axisTracks[trackRangeExcludingLines(axis)]
        val tracksAllFixed = spannedTracks
            .all { track -> track.maxTrackSizingFunction.definiteLimit(axisParentSize).isSome() }
        return if (tracksAllFixed) {
            val limit: Float = spannedTracks
                .map { track -> track.maxTrackSizingFunction.definiteLimit(axisParentSize).unwrap() }
                .sum()
            Option.Some(limit)
        } else {
            Option.None
        }
    }

    /**
     * Similar to the spanned_track_limit, but excludes FitContent arguments from the limit.
     * Used to clamp the automatic minimum contributions of an item
     */
    fun spannedFixedTrackLimit(
        axis: AbstractAxis,
        axisTracks: MutableList<GridTrack>,
        axisParentSize: Option<Float>
    ): Option<Float> {
        val spannedTracks = axisTracks[trackRangeExcludingLines(axis)]
        val tracksAllFixed = spannedTracks
            .all { track -> track.maxTrackSizingFunction.definiteValue(axisParentSize).isSome() }
        return if (tracksAllFixed) {
            val limit: Float = spannedTracks
                .map { track -> track.maxTrackSizingFunction.definiteValue(axisParentSize).unwrap() }
                .sum()
            Option.Some(limit)
        } else {
            Option.None
        }
    }

    /**
     * Compute the known_dimensions to be passed to the child sizing functions
     * The key thing that is being done here is applying stretch alignment, which is necessary to
     * allow percentage sizes further down the tree to resolve properly in some cases
     */
    fun knownDimensions(
        innerNodeSize: Size<Option<Float>>,
        gridAreaSize: Size<Option<Float>>,
    ): Size<Option<Float>> {
        val margins = marginsAxisSumsWithBaselineShims(innerNodeSize.width)

        val aspectRatio = aspectRatio;
        val padding = padding.resolveOrZero(gridAreaSize);
        val border = border.resolveOrZero(gridAreaSize);
        val paddingBorderSize = (padding + border).sumAxes();
        val boxSizingAdjustment =
            if (boxSizing == BoxSizing.CONTENT_BOX) paddingBorderSize else Size.ZERO.clone()
        val inherentSize = size
            .maybeResolve(gridAreaSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
        val minSize = minSize
            .maybeResolve(gridAreaSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
        val maxSize = maxSize
            .maybeResolve(gridAreaSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)

        val gridAreaMinusItemMarginsSize = gridAreaSize.maybeSub(margins)

        // If node is absolutely positioned and width is not set explicitly, then deduce it
        // from left, right and container_content_box if both are set.
        val wdt = inherentSize.width.orElse {
            // Apply width based on stretch alignment if:
            //  - Alignment style is "stretch"
            //  - The node is not absolutely positioned
            //  - The node does not have auto margins in this axis.
            if (
                margin.left != LengthPercentageAuto.AUTO &&
                margin.right != LengthPercentageAuto.AUTO &&
                justifySelf == AlignSelf.STRETCH
            ) {
                gridAreaMinusItemMarginsSize.width
            }

            Option.None
        }

        // Reapply aspect ratio after stretch and absolute position width adjustments
        val (width, height) = Size(wdt, height = inherentSize.height).maybeApplyAspectRatio(aspectRatio).t2()

        val hgt = inherentSize.width.orElse {
            // Apply height based on stretch alignment if:
            //  - Alignment style is "stretch"
            //  - The node is not absolutely positioned
            //  - The node does not have auto margins in this axis.
            if (
                margin.top != LengthPercentageAuto.AUTO &&
                margin.bottom != LengthPercentageAuto.AUTO &&
                alignSelf == AlignSelf.STRETCH
            ) {
                gridAreaMinusItemMarginsSize.height
            }

            Option.None
        }

        // Reapply aspect ratio after stretch and absolute position height adjustments
        val (width2, height2) = Size(width, hgt).maybeApplyAspectRatio(aspectRatio)

        // Clamp size by min and max width/height
        val (width3, height3) = Size(width2, height2).maybeClamp(minSize, maxSize);

        return Size(width3, height3)
    }

    /**
     * Compute the available_space to be passed to the child sizing functions
     * These are estimates based on either the max track sizing function or the provisional base size in the opposite
     * axis to the one currently being sized.
     * https://www.w3.org/TR/css-grid-1/#algo-overview
     */
    fun availableSpace(
        axis: AbstractAxis,
        otherAxisTracks: MutableList<GridTrack>,
        otherAxisAvailableSpace: Option<Float>,
        getTrackSizeEstimate: (GridTrack, Option<Float>) -> Option<Float>
    ): Size<Option<Float>> {
        val itemOtherAxisSize: Option<Float> = run {
            otherAxisTracks[trackRangeExcludingLines(axis.other())]
                .map { track ->
                    getTrackSizeEstimate(track, otherAxisAvailableSpace)
                        .map { size -> size + track.contentAlignmentAdjustment }
                }
                .sum()
        }

        val size = Size.NONE.clone()
        size.set(axis.other(), itemOtherAxisSize)

        return size
    }

    /**
     * Retrieve the available_space from the cache or compute them using the passed parameters
     */
    fun availableSpaceCached(
        axis: AbstractAxis,
        otherAxisTracks: MutableList<GridTrack>,
        otherAxisAvailableSpace: Option<Float>,
        getTrackSizeEstimate: (GridTrack, Option<Float>) -> Option<Float>
    ): Size<Option<Float>> {
        return availableSpaceCache.unwrapOrElse {
            val availableSpaces = availableSpace(axis, otherAxisTracks, otherAxisAvailableSpace, getTrackSizeEstimate)
            availableSpaceCache = Option.Some(availableSpaces)

            availableSpaces
        }
    }

    /**
     * Compute the item's resolved margins for size contributions. Horizontal percentage margins always resolve
     * to zero if the container size is indefinite as otherwise this would introduce a cyclic dependency.
     */
    fun marginsAxisSumsWithBaselineShims(innerNodeWidth: Option<Float>): Size<Float> {
        return Rect(
            left = margin.left.resolveOrZero(Option.Some(0f)),
            right = margin.right.resolveOrZero(Option.Some(0f)),
            top = margin.top.resolveOrZero(innerNodeWidth) + baselineShim,
            bottom = margin.bottom.resolveOrZero(innerNodeWidth),
        ).sumAxes()
    }

    /**
     * Compute the item's min content contribution from the provided parameters
     */
    fun minContentContribution(
        axis: AbstractAxis,
        tree: LayoutPartialTree,
        availableSpace: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        val knownDimensions = knownDimensions(innerNodeSize, availableSpace)
        return tree.measureChildSize(
            node,
            knownDimensions,
            innerNodeSize,
            availableSpace.map { opt ->
                when (opt) {
                    is Option.Some -> AvailableSpace.Definite(opt.unwrap())
                    is Option.None -> AvailableSpace.MinContent
                }
            },
            SizingMode.INHERENT_SIZE,
            axis.asAbsNaive(),
            Line.FALSE
        )
    }

    /**
     * Retrieve the item's min content contribution from the cache or compute it using the provided parameters
     */
    fun minContentContributionCached(
        axis: AbstractAxis,
        tree: LayoutPartialTree,
        availableSpace: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        return minContentContributionCache.get(axis).unwrapOrElse {
            val size = minContentContribution(axis, tree, availableSpace, innerNodeSize)
            minContentContributionCache.set(axis, Option.Some(size))

            size
        }
    }

    /**
     * Compute the item's max content contribution from the provided parameters
     */
    fun maxContentContribution(
        axis: AbstractAxis,
        tree: LayoutPartialTree,
        availableSpace: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        val knownDimensions = knownDimensions(innerNodeSize, availableSpace)
        return tree.measureChildSize(
            node,
            knownDimensions,
            innerNodeSize,
            availableSpace.map { opt ->
                when (opt) {
                    is Option.Some -> AvailableSpace.Definite(opt.unwrap())
                    is Option.None -> AvailableSpace.MaxContent
                }
            },
            SizingMode.INHERENT_SIZE,
            axis.asAbsNaive(),
            Line.FALSE
        )
    }

    /**
     * Retrieve the item's max content contribution from the cache or compute it using the provided parameters
     */
    fun maxContentContributionCached(
        axis: AbstractAxis,
        tree: LayoutPartialTree,
        availableSpace: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        return maxContentContributionCache.get(axis).unwrapOrElse {
            val size = maxContentContribution(axis, tree, availableSpace, innerNodeSize)
            maxContentContributionCache.set(axis, Option.Some(size))

            size
        }
    }

    /**
     * The minimum contribution of an item is the smallest outer size it can have.
     * Specifically:
     *   - If the item’s computed preferred size behaves as auto or depends on the size of its containing block in the relevant axis:
     *     Its minimum contribution is the outer size that would result from assuming the item’s used minimum size as its preferred size;
     *   - Else the item’s minimum contribution is its min-content contribution.
     * Because the minimum contribution often depends on the size of the item’s content, it is considered a type of intrinsic size contribution.
     * See: https://www.w3.org/TR/css-grid-1/#min-size-auto
     */
    fun minimumContribution(
        tree: LayoutPartialTree,
        axis: AbstractAxis,
        axisTracks: MutableList<GridTrack>,
        knownDimensions: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        val padding = padding.resolveOrZero(innerNodeSize)
        val border = border.resolveOrZero(innerNodeSize)
        val paddingBorderSize = (padding + border).sumAxes()
        val boxSizingAdjustment = if (boxSizing == BoxSizing.CONTENT_BOX) paddingBorderSize else Size.ZERO.clone()
        val size = size
            .maybeResolve(innerNodeSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
            .get(axis)
            .orElse {
                minSize
                    .maybeResolve(innerNodeSize)
                    .maybeApplyAspectRatio(aspectRatio)
                    .maybeAdd(boxSizingAdjustment)
                    .get(axis)
            }
            .orElse {
                overflow.get(axis).maybeIntoAutomaticMinSize()
            }
            .unwrapOrElse {
                // Automatic minimum size. See https://www.w3.org/TR/css-grid-1/#min-size-auto

                // To provide a more reasonable default minimum size for grid items, the used value of its automatic minimum size
                // in a given axis is the content-based minimum size if all of the following are true:
                val itemAxisTracks = axisTracks[trackRangeExcludingLines(axis)]

                // it is not a scroll container
                // TODO: support overflow property

                // it spans at least one track in that axis whose min track sizing function is auto
                val spansAutoMinTrack = axisTracks
                    // TODO: should this be 'behaves as auto' rather than just literal auto?
                    .any { track -> track.minTrackSizingFunction is MinTrackSizingFunction.Auto }

                // if it spans more than one track in that axis, none of those tracks are flexible
                val onlySpanOneTrack = itemAxisTracks.len() == 1;
                val spansAFlexibleTrack = axisTracks
                    .any { track -> track.maxTrackSizingFunction.isFlexible() }

                val useContentBasedMinimum =
                    spansAutoMinTrack && (onlySpanOneTrack || !spansAFlexibleTrack);

                // Otherwise, the automatic minimum size is zero, as usual.
                if (useContentBasedMinimum) {
                    minContentContributionCached(axis, tree, knownDimensions, innerNodeSize)
                } else {
                    0f
                }
            }

        // In all cases, the size suggestion is additionally clamped by the maximum size in the affected axis, if it’s definite.
        // Note: The argument to fit-content() does not clamp the content-based minimum size in the same way as a fixed max track
        // sizing function.
        val limit = spannedFixedTrackLimit(axis, axisTracks, innerNodeSize.get(axis))

        return size.maybeMin(limit)
    }

    /**
     * Retrieve the item's minimum contribution from the cache or compute it using the provided parameters
     */
    fun minimumContributionCached(
        tree: LayoutPartialTree,
        axis: AbstractAxis,
        axisTracks: MutableList<GridTrack>,
        knownDimensions: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        return minimumContributionCache.get(axis).unwrapOrElse {
            val size = minimumContribution(tree, axis, axisTracks, knownDimensions, innerNodeSize)
            minimumContributionCache.set(axis, Option.Some(size))
            size
        }
    }

    companion object {
        /**
         * Create a new item given a concrete placement in both axes
         */
        fun <S : GridItemStyle> newWithPlacementStyleAndOrder(
            node: Int,
            colSpan: Line<OriginZeroLine>,
            rowSpan: Line<OriginZeroLine>,
            style: S,
            parentAlignItems: AlignItems,
            parentJustifyItems: AlignItems,
            sourceOrder: Int
        ): GridItem {
            return GridItem(
                node = node,
                sourceOrder = sourceOrder,
                row = rowSpan,
                column = colSpan,
                overflow = style.overflow(),
                boxSizing = style.boxSizing(),
                size = style.size(),
                minSize = style.minSize(),
                maxSize = style.maxSize(),
                aspectRatio = style.aspectRatio(),
                padding = style.padding(),
                border = style.border(),
                margin = style.margin(),
                alignSelf = style.alignSelf().unwrapOr(parentAlignItems),
                justifySelf = style.justifySelf().unwrapOr(parentJustifyItems),
                baseline = Option.None,
                baselineShim = 0f,
                rowIndexes = Line(start = 0, end = 0), // Properly initialised later
                columnIndexes = Line(start = 0, end = 0), // Properly initialised later
                crossesFlexibleRow = false,            // Properly initialised later
                crossesFlexibleColumn = false,         // Properly initialised later
                crossesIntrinsicRow = false,           // Properly initialised later
                crossesIntrinsicColumn = false,        // Properly initialised later
                availableSpaceCache = Option.None,
                minContentContributionCache = Size.NONE.clone(),
                maxContentContributionCache = Size.NONE.clone(),
                minimumContributionCache = Size.NONE.clone(),
                yPosition = 0f,
                height = 0f
            )
        }
    }
}
