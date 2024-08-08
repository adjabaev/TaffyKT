package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.compute.grid.types.GridTrack
import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.geometry.extensions.sumAxes
import be.arby.taffy.lang.Option
import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.geom.AbstractAxis

/**
 * This struct captures a bunch of variables which are used to compute the intrinsic sizes of children so that those variables
 * don't have to be passed around all over the place below. It then has methods that implement the intrinsic sizing computations
 */
class IntrisicSizeMeasurer<Tree: LayoutTree, EstimateFunction: (GridTrack, Option<Float>) -> Option<Float>>(
    /**
     * The layout tree
     */
    var tree: Tree,
    /**
     * The tracks in the opposite axis to the one we are currently sizing
     */
    var otherAxisTracks: List<GridTrack>,
    /**
     * A function that computes an estimate of an other-axis track's size which is passed to the child size measurement functions
     */
    var getTrackSizeEstimate: EstimateFunction,
    /**
     * The axis we are currently sizing
     */
    var axis: AbstractAxis,
    /**
     * The available grid space
     */
    var innerNodeSize: Size<Option<Float>>
) {

    /**
     * Compute the known_dimensions to be passed to the child sizing functions
     * These are estimates based on either the max track sizing function or the provisional base size in the opposite
     * axis to the one currently being sized.
     * https://www.w3.org/TR/css-grid-1/#algo-overview
     */
    fun knownDimensions(item: GridItem): Size<Option<Float>> {
        return item.knownDimensionsCached(
            axis = axis,
            otherAxisTracks = otherAxisTracks,
            otherAxisAvailableSpace = innerNodeSize.get(axis.other()),
            getTrackSizeEstimate = getTrackSizeEstimate
        )
    }

    /**
     * Compute the item's resolved margins for size contributions. Horizontal percentage margins always resolve
     * to zero if the container size is indefinite as otherwise this would introduce a cyclic dependency.
     */
    fun marginsAxisSumsWithBaselineShims(item: GridItem): Size<Float> {
        val parentWidth = innerNodeSize.width

        return Rect(
            left = item.margin.left.resolveOrZero(Option.Some(0f)),
            right = item.margin.right.resolveOrZero(Option.Some(0f)),
            top = item.margin.top.resolveOrZero(parentWidth) + item.baselineShim,
            bottom = item.margin.bottom.resolveOrZero(parentWidth)
        ).sumAxes()
    }

    /**
     * Retrieve the item's min content contribution from the cache or compute it using the provided parameters
     */
    fun minContentContribution(item: GridItem): Float {
        val knownDimensions = knownDimensions(item)
        val marginAxisSums = marginsAxisSumsWithBaselineShims(item)
        val contribution = item.minContentContributionCached(axis, tree, knownDimensions, innerNodeSize)
        return contribution + marginAxisSums.get(axis)
    }

    /**
     * Retrieve the item's max content contribution from the cache or compute it using the provided parameters
     */
    fun maxContentContribution(item: GridItem): Float {
        val knownDimensions = knownDimensions(item)
        val marginAxisSums = marginsAxisSumsWithBaselineShims(item)
        val contribution = item.maxContentContributionCached(axis, tree, knownDimensions, innerNodeSize)
        return contribution + marginAxisSums.get(axis)
    }

    /**
     * The minimum contribution of an item is the smallest outer size it can have.
     * Specifically:
     *
     * - If the item’s computed preferred size behaves as auto or depends on the size of its containing block in the relevant axis:
     * Its minimum contribution is the outer size that would result from assuming the item’s used minimum size as its preferred size;
     * - Else the item’s minimum contribution is its min-content contribution.
     * Because the minimum contribution often depends on the size of the item’s content, it is considered a type of intrinsic size contribution.
     */
    fun minimumContribution(item: GridItem, axisTracks: List<GridTrack>): Float {
        val knownDimensions = knownDimensions(item)
        val marginAxisSums = marginsAxisSumsWithBaselineShims(item)
        val contribution = item.minimumContributionCached(
                tree,
                axis,
                axisTracks,
                knownDimensions,
                innerNodeSize
        )
        return contribution + marginAxisSums.get(axis)
    }
}
