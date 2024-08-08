package be.arby.taffy.compute.grid.types

import be.arby.taffy.compute.GenericAlgorithm
import be.arby.taffy.geometry.Line
import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.geometry.extensions.maybeApplyAspectRatio
import be.arby.taffy.geometry.extensions.span
import be.arby.taffy.lang.Option
import be.arby.taffy.util.maybeMin
import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.layout.SizingMode
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.node.Node
import be.arby.taffy.resolve.maybeResolveStS
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.grid.MaxTrackSizingFunction
import be.arby.taffy.style.grid.MinTrackSizingFunction
import be.arby.taffy.utils.sum

class GridItem(
    /**
     * The id of the Node that this item represents
     */
    var node: Node,
    /**
     * The order of the item in the children array
     *
     * We sort the list of grid items during track sizing. This field allows us to sort back the original order for final positioning
     */
    var sourceOrder: Int,
    /**
     * The item's definite row-start and row-end, as resolved by the placement algorithm (in origin-zero coordinates)
     */
    var row: Line<OriginZeroLine>,
    /**
     * The items definite column-start and column-end, as resolved by the placement algorithm (in origin-zero coordinates)
     */
    var column: Line<OriginZeroLine>,
    /**
     * The item's margin style
     */
    var margin: Rect<LengthPercentageAuto>,
    /**
     * The item's align_self property, or the parent's align_items property is not set
     */
    var alignSelf: AlignSelf,
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
    var knownDimensionsCache: Option<Size<Option<Float>>>,
    /**
     * Cache for the known_dimensions input to intrinsic sizing computation
     */
    var minContentContributionCache: Size<Option<Float>>,
    /**
     * Cache for the min-content size
     */
    var minimumContributionCache: Size<Option<Float>>,
    /**
     * Cache for the minimum contribution
     */
    var maxContentContributionCache: Size<Option<Float>>
) {

    companion object {
        fun newWithPlacementStyleAndOrder(
            node: Node,
            colSpan: Line<OriginZeroLine>,
            rowSpan: Line<OriginZeroLine>,
            style: Style,
            parentAlignItems: AlignItems,
            sourceOrder: Int
        ): GridItem {
            return GridItem(
                node = node,
                sourceOrder = sourceOrder,
                row = rowSpan,
                column = colSpan,
                margin = style.margin,
                alignSelf = style.alignSelf.unwrapOr(parentAlignItems),
                baseline = Option.None,
                baselineShim = 0.0f,
                rowIndexes = Line(start = 0, end = 0), // Properly initialised later
                columnIndexes = Line(start = 0, end = 0), // Properly initialised later
                crossesFlexibleRow = false, // Properly initialised later
                crossesFlexibleColumn = false, // Properly initialised later
                crossesIntrinsicRow = false, // Properly initialised later
                crossesIntrinsicColumn = false, // Properly initialised later
                knownDimensionsCache = Option.None,
                minContentContributionCache = Size.none(),
                maxContentContributionCache = Size.none(),
                minimumContributionCache = Size.none()
            )
        }
    }

    fun placement(axis: AbstractAxis): Line<OriginZeroLine> {
        return when (axis) {
            AbstractAxis.BLOCK -> row
            AbstractAxis.INLINE -> column
        }
    }

    fun placementIndexes(axis: AbstractAxis): Line<Int> {
        return when (axis) {
            AbstractAxis.BLOCK -> rowIndexes
            AbstractAxis.INLINE -> columnIndexes
        }
    }

    fun trackRangeExcludingLines(axis: AbstractAxis): IntRange {
        val indexes = placementIndexes(axis)
        return (indexes.start + 1) until (indexes.end)
    }

    fun span(axis: AbstractAxis): Int {
        return when (axis) {
            AbstractAxis.BLOCK -> row.span()
            AbstractAxis.INLINE -> column.span()
        }
    }

    fun crossesFlexibleTrack(axis: AbstractAxis): Boolean {
        return when (axis) {
            AbstractAxis.INLINE -> crossesFlexibleColumn
            AbstractAxis.BLOCK -> crossesFlexibleRow
        }
    }

    /**
     * Returns the pre-computed value indicating whether the grid item crosses an intrinsic track in the specified axis
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
        axisTracks: List<GridTrack>,
        axisParentSize: Option<Float>
    ): Option<Float> {
        val spannedTracks = axisTracks.slice(trackRangeExcludingLines(axis))
        val tracksAllFixed =
            spannedTracks.all { track -> track.maxTrackSizingFunction.definiteLimit(axisParentSize).isSome() }
        return if (tracksAllFixed) {
            val limit =
                spannedTracks.map { track -> track.maxTrackSizingFunction.definiteLimit(axisParentSize).unwrap() }
                    .sum()
            Option.Some(limit)
        } else {
            Option.None
        }
    }

    fun spannedFixedTrackLimit(
        axis: AbstractAxis,
        axisTracks: List<GridTrack>,
        axisParentSize: Option<Float>,
    ): Option<Float> {
        val spannedTracks = axisTracks.slice(trackRangeExcludingLines(axis))
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
     * These are estimates based on either the max track sizing function or the provisional base size in the opposite
     * axis to the one currently being sized.
     * https://www.w3.org/TR/css-grid-1/#algo-overview
     */
    fun knownDimensions(
        axis: AbstractAxis,
        otherAxisTracks: List<GridTrack>,
        otherAxisAvailableSpace: Option<Float>,
        getTrackSizeEstimate: (GridTrack, Option<Float>) -> Option<Float>,
    ): Size<Option<Float>> {
        val itemOtherAxisSize = otherAxisTracks.slice(trackRangeExcludingLines(axis.other()))
            .map { track ->
                getTrackSizeEstimate(
                    track,
                    otherAxisAvailableSpace
                ).map { size -> size + track.contentAlignmentAdjustment }
            }
            .sum()

        val size = Size.none()
        size.set(axis.other(), itemOtherAxisSize)
        return size
    }

    /**
     * Retrieve the known_dimensions from the cache or compute them using the passed parameters
     */
    fun knownDimensionsCached(
        axis: AbstractAxis,
        otherAxisTracks: List<GridTrack>,
        otherAxisAvailableSpace: Option<Float>,
        getTrackSizeEstimate: (GridTrack, Option<Float>) -> Option<Float>,
    ): Size<Option<Float>> {
        if (knownDimensionsCache.isNone()) {
            val knownDimensions = knownDimensions(axis, otherAxisTracks, otherAxisAvailableSpace, getTrackSizeEstimate)
            knownDimensionsCache = Option.Some(knownDimensions)
        }
        return knownDimensionsCache.unwrap()
    }

    /**
     * Compute the item's min content contribution from the provided parameters
     */
    fun minContentContribution(
        axis: AbstractAxis,
        tree: LayoutTree,
        knownDimensions: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        return GenericAlgorithm.measureSize(
            tree,
            node,
            knownDimensions,
            innerNodeSize,
            Size.MIN_CONTENT,
            SizingMode.INHERENT_SIZE
        ).get(axis)
    }

    /**
     * Retrieve the item's min content contribution from the cache or compute it using the provided parameters
     */
    fun minContentContributionCached(
        axis: AbstractAxis,
        tree: LayoutTree,
        knownDimensions: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        if (minContentContributionCache.get(axis).isNone()) {
            val size = minContentContribution(axis, tree, knownDimensions, innerNodeSize)
            minContentContributionCache.set(axis, Option.Some(size))
        }

        return minContentContributionCache.get(axis).unwrap()
    }

    /**
     * Compute the item's max content contribution from the provided parameters
     */
    fun maxContentContribution(
        axis: AbstractAxis,
        tree: LayoutTree,
        knownDimensions: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        return GenericAlgorithm.measureSize(
            tree,
            node,
            knownDimensions,
            innerNodeSize,
            Size.MAX_CONTENT,
            SizingMode.INHERENT_SIZE
        ).get(axis)
    }

    /**
     * Retrieve the item's max content contribution from the cache or compute it using the provided parameters
     */
    fun maxContentContributionCached(
        axis: AbstractAxis,
        tree: LayoutTree,
        knownDimensions: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        if (maxContentContributionCache.get(axis).isNone()) {
            val size = maxContentContribution(axis, tree, knownDimensions, innerNodeSize)
            maxContentContributionCache.set(axis, Option.Some(size))
        }

        return maxContentContributionCache.get(axis).unwrap()
    }

    fun minimumContribution(
        tree: LayoutTree,
        axis: AbstractAxis,
        axisTracks: List<GridTrack>,
        knownDimensions: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        val style = tree.style(node)

        val size = style
            .size
            .maybeResolveStS(innerNodeSize)
            .maybeApplyAspectRatio(style.aspectRatio)
            .get(axis)
            .orElse {
                style
                    .minSize
                    .maybeResolveStS(innerNodeSize)
                    .maybeApplyAspectRatio(style.aspectRatio)
                    .get(axis)
            }.unwrapOrElse {
                // Automatic minimum size. See https://www.w3.org/TR/css-grid-1/#min-size-auto

                // To provide a more reasonable default minimum size for grid items, the used value of its automatic minimum size
                // in a given axis is the content-based minimum size if all of the following are true:
                val itemAxisTracks = axisTracks.slice(trackRangeExcludingLines(axis))

                // it is not a scroll container
                // TODO: support overflow property

                // it spans at least one track in that axis whose min track sizing function is auto
                val spansAutoMinTrack = axisTracks
                    // TODO: should this be 'behaves as auto' rather than just literal auto?
                    .any { track -> track.minTrackSizingFunction == MinTrackSizingFunction.Auto }

                // if it spans more than one track in that axis, none of those tracks are flexible
                val onlySpanOneTrack = itemAxisTracks.size == 1;
                val spansAFlexibleTrack =
                    axisTracks.any { track -> track.maxTrackSizingFunction is MaxTrackSizingFunction.Flex }

                val useContentBasedMinimum = spansAutoMinTrack && (onlySpanOneTrack || !spansAFlexibleTrack);

                // Otherwise, the automatic minimum size is zero, as usual.
                if (useContentBasedMinimum) {
                    minContentContributionCached(axis, tree, knownDimensions, innerNodeSize)
                } else {
                    0.0f
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
        tree: LayoutTree,
        axis: AbstractAxis,
        axisTracks: List<GridTrack>,
        knownDimensions: Size<Option<Float>>,
        innerNodeSize: Size<Option<Float>>
    ): Float {
        if (minimumContributionCache.get(axis).isNone()) {
            val size = minimumContribution(tree, axis, axisTracks, knownDimensions, innerNodeSize)
            minimumContributionCache.set(axis, Option.Some(size))
        }
        return minimumContributionCache.get(axis).unwrap()
    }
}
