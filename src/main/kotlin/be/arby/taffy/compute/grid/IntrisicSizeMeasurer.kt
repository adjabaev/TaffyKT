package be.arby.taffy.compute.grid

import be.arby.taffy.compute.grid.types.GridItem
import be.arby.taffy.compute.grid.types.GridTrack
import be.arby.taffy.geom.AbstractAxis
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.tree.traits.LayoutPartialTree

/**
 * This struct captures a bunch of variables which are used to compute the intrinsic sizes of children so that those variables
 * don't have to be passed around all over the place below. It then has methods that implement the intrinsic sizing computations
 */
data class IntrisicSizeMeasurer<Tree : LayoutPartialTree, EstimateFunction : (GridTrack, Option<Float>) -> Option<Float>>(
    /**
     * The layout tree
     */
    var tree: Tree,
    /**
     * The tracks in the opposite axis to the one we are currently sizing
     */
    val otherAxisTracks: MutableList<GridTrack>,
    /**
     * A function that computes an estimate of an other-axis track's size which is passed to
     * the child size measurement functions
     */
    val getTrackSizeEstimate: EstimateFunction,
    /**
     * The axis we are currently sizing
     */
    val axis: AbstractAxis,
    /**
     * The available grid space
     */
    val innerNodeSize: Size<Option<Float>>
) {
    /**
     * Compute the available_space to be passed to the child sizing functions
     * These are estimates based on either the max track sizing function or the provisional base size in the opposite
     * axis to the one currently being sized.
     * https://www.w3.org/TR/css-grid-1/#algo-overview
     */
    fun availableSpace(item: GridItem): Size<Option<Float>> {
        return item.availableSpaceCached(
            axis,
            otherAxisTracks,
            innerNodeSize.get(axis.other()),
            getTrackSizeEstimate,
        )
    }

    /**
     * Compute the item's resolved margins for size contributions. Horizontal percentage margins always resolve
     * to zero if the container size is indefinite as otherwise this would introduce a cyclic dependency.
     */
    fun marginsAxisSumsWithBaselineShims(item: GridItem): Size<Float> {
        return item.marginsAxisSumsWithBaselineShims(innerNodeSize.width)
    }

    /**
     * Retrieve the item's min content contribution from the cache or compute it using the provided parameters
     */
    fun minContentContribution(item: GridItem): Float {
        val availableSpace = availableSpace(item)
        val marginAxisSums = marginsAxisSumsWithBaselineShims(item)
        val contribution =
            item.minContentContributionCached(axis, tree, availableSpace, innerNodeSize)
        return contribution + marginAxisSums.get(axis)
    }

    /**
     * Retrieve the item's max content contribution from the cache or compute it using the provided parameters
     */
    fun maxContentContribution(item: GridItem): Float {
        val availableSpace = availableSpace(item)
        val marginAxisSums = marginsAxisSumsWithBaselineShims(item)
        val contribution =
            item.maxContentContributionCached(axis, tree, availableSpace, innerNodeSize)
        return contribution + marginAxisSums.get(axis)
    }

    /**
     * The minimum contribution of an item is the smallest outer size it can have.
     * Specifically:
     *   - If the item’s computed preferred size behaves as auto or depends on the size of its containing block in the relevant axis:
     *     Its minimum contribution is the outer size that would result from assuming the item’s used minimum size as its preferred size
     *   - Else the item’s minimum contribution is its min-content contribution.
     * Because the minimum contribution often depends on the size of the item’s content, it is considered a type of intrinsic size contribution.
     */
    fun minimumContribution(item: GridItem, axisTracks: MutableList<GridTrack>): Float {
        val availableSpace = availableSpace(item)
        val marginAxisSums = marginsAxisSumsWithBaselineShims(item)
        val contribution =
            item.minimumContributionCached(tree, axis, axisTracks, availableSpace, innerNodeSize)
        return contribution + marginAxisSums.get(axis)
    }
}
