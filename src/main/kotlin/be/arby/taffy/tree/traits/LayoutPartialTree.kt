package be.arby.taffy.tree.traits

import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.geom.Line
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.CoreStyle
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.cache.Cache
import be.arby.taffy.tree.layout.*

/**
 * Any type that implements [`LayoutPartialTree`] can be laid out using [Taffy's algorithms](crate::compute)
 *
 * Note that this trait extends [`TraversePartialTree`] (not [`TraverseTree`]). Taffy's algorithm implementations have been designed such that they can be used for a laying out a single
 */
interface LayoutPartialTree: TraversePartialTree {

    /**
     * Get core style
     */
    fun getCoreContainerStyle(nodeId: Int): CoreStyle

    /**
     * Set the node's unrounded layout
     */
    fun setUnroundedLayout(nodeId: Int, layout: Layout)

    /**
     * Get a mutable reference to the [`Cache`] for this node.
     */
    fun getCache(nodeId: Int): Cache

    /**
     * Compute the specified node's size or full layout given the specified constraints
     */
    fun computeChildLayout(nodeId: Int, inputs: LayoutInput): LayoutOutput

    /**
     * Compute the size of the node given the specified constraints
     */
    fun measureChildSize(
        nodeId: Int,
        knownDimensions: Size<Option<Float>>,
        parentSize: Size<Option<Float>>,
        availableSpace: Size<AvailableSpace>,
        sizingMode: SizingMode,
        axis: AbsoluteAxis,
        verticalMarginsAreCollapsible: Line<Boolean>
    ): Float {
        return computeChildLayout(
            nodeId,
            LayoutInput(
                knownDimensions = knownDimensions,
                parentSize = parentSize,
                availableSpace = availableSpace,
                sizingMode = sizingMode,
                axis = axis.into(),
                runMode = RunMode.COMPUTE_SIZE,
                verticalMarginsAreCollapsible = verticalMarginsAreCollapsible
            ),
        ).size.getAbs(axis)
    }

    /**
     * Perform a full layout on the node given the specified constraints
     */
    fun performChildLayout(
        nodeId: Int,
        knownDimensions: Size<Option<Float>>,
        parentSize: Size<Option<Float>>,
        availableSpace: Size<AvailableSpace>,
        sizingMode: SizingMode,
        verticalMarginsAreCollapsible: Line<Boolean>,
    ): LayoutOutput {
        return computeChildLayout(
            nodeId,
            LayoutInput(
                knownDimensions = knownDimensions,
                parentSize = parentSize,
                availableSpace = availableSpace,
                sizingMode = sizingMode,
                axis = RequestedAxis.BOTH,
                runMode = RunMode.PERFORM_LAYOUT,
                verticalMarginsAreCollapsible = verticalMarginsAreCollapsible,
            ),
        )
    }
}
