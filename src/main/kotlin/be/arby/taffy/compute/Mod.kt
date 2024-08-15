package be.arby.taffy.compute

import be.arby.taffy.geom.*
import be.arby.taffy.lang.Option
import be.arby.taffy.style.BoxSizing
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.tree.layout.LayoutInput
import be.arby.taffy.tree.layout.LayoutOutput
import be.arby.taffy.tree.layout.SizingMode
import be.arby.taffy.tree.traits.LayoutPartialTree
import be.arby.taffy.tree.traits.RoundTree
import be.arby.taffy.util.maybeAdd
import be.arby.taffy.util.maybeClamp
import be.arby.taffy.util.maybeMax
import be.arby.taffy.util.maybeSub
import kotlin.math.round

/**
 * Compute layout for the root node in the tree
 */
fun computeRootLayout(tree: LayoutPartialTree, root: Int, availableSpace: Size<AvailableSpace>) {
    var knownDimensions = Size.NONE

    val parentSize = availableSpace.intoOptions()
    var style = tree.getCoreContainerStyle(root)

    if (style.isBlock()) {
        // Pull these out earlier to avoid borrowing issues
        val aspectRatio = style.aspectRatio()
        val margin = style.margin().resolveOrZero(parentSize.width)
        val padding = style.padding().resolveOrZero(parentSize.width)
        val border = style.border().resolveOrZero(parentSize.width)
        val paddingBorderSize = (padding + border).sumAxes()
        val boxSizingAdjustment =
            if (style.boxSizing() == BoxSizing.CONTENT_BOX) paddingBorderSize else Size.ZERO

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
        val clampedStyleSize = style
            .size()
            .maybeResolve(parentSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
            .maybeClamp(minSize, maxSize)

        // If both min and max in a given axis are set and max <= min then this determines the size in that axis
        val minMaxDefiniteSize = minSize.zipMap(maxSize) { min, max ->
            when {
                min.isSome() && max.isSome() && max.unwrap() <= min.unwrap() -> min.copy()
                else -> Option.None
            }
        }

        // Block nodes automatically stretch fit their width to fit available space if available space is definite
        val availableSpaceBasedSize = Size(
            width = availableSpace.width.intoOption().maybeSub(margin.horizontalAxisSum()),
            height = Option.None,
        )

        val styledBasedKnownDimensions = knownDimensions
            .or(minMaxDefiniteSize)
            .or(clampedStyleSize)
            .or(availableSpaceBasedSize)
            .maybeMax(paddingBorderSize)

        knownDimensions = styledBasedKnownDimensions
    }

    // Recursively compute node layout
    val output = tree.performChildLayout(
        root,
        knownDimensions,
        availableSpace.intoOptions(),
        availableSpace,
        SizingMode.INHERENT_SIZE,
        Line.FALSE,
    )

    style = tree.getCoreContainerStyle(root)
    val padding = style.padding().resolveOrZero(availableSpace.width.intoOption())
    val border = style.border().resolveOrZero(availableSpace.width.intoOption())
    val margin = style.margin().resolveOrZero(availableSpace.width.intoOption())
    val scrollbarSize = Size(
        width = if (style.overflow().y == Overflow.SCROLL) style.scrollbarWidth() else 0f,
        height = if (style.overflow().x == Overflow.SCROLL) style.scrollbarWidth() else 0f,
    )

    tree.setUnroundedLayout(
        root,
        Layout(
            order = 0,
            location = Point.ZERO,
            size = output.size,
            contentSize = output.contentSize,
            scrollbarSize = scrollbarSize,
            padding = padding,
            border = border,
            // TODO: support auto margins for root node?
            margin = margin
        )
    )
}

fun <Tree : LayoutPartialTree> computeCachedLayout(
    tree: Tree,
    node: Int,
    inputs: LayoutInput,
    computeUncached: (Tree, Int, LayoutInput) -> LayoutOutput
): LayoutOutput {
    val knownDimensions = inputs.knownDimensions
    val availableSpace = inputs.availableSpace
    val runMode = inputs.runMode

    // First we check if we have a cached result for the given input
    val cacheEntry = tree.getCache(node).get(knownDimensions, availableSpace, runMode)
    if (cacheEntry.isSome()) {
        return cacheEntry.unwrap()
    }

    val computedSizeAndBaselines = computeUncached(tree, node, inputs)

    // Cache result
    tree.getCache(node).store(knownDimensions, availableSpace, runMode, computedSizeAndBaselines)

    return computedSizeAndBaselines
}

fun roundLayout(tree: RoundTree, nodeId: Int) {
    return roundLayoutInner(tree, nodeId, 0f, 0f)
}

/**
 * Recursive function to apply rounding to all descendents
 */
private fun roundLayoutInner(tree: RoundTree, nodeId: Int, cumulativeX: Float, cumulativeY: Float) {
    val unroundedLayout = tree.getUnroundedLayout(nodeId)
    val layout = unroundedLayout

    val cumulativeX = cumulativeX + unroundedLayout.location.x;
    val cumulativeY = cumulativeY + unroundedLayout.location.y;

    layout.location.x = round(unroundedLayout.location.x)
    layout.location.y = round(unroundedLayout.location.y)
    layout.size.width = round(cumulativeX + unroundedLayout.size.width) - round(cumulativeX)
    layout.size.height = round(cumulativeY + unroundedLayout.size.height) - round(cumulativeY)
    layout.scrollbarSize.width = round(unroundedLayout.scrollbarSize.width)
    layout.scrollbarSize.height = round(unroundedLayout.scrollbarSize.height)
    layout.border.left = round(cumulativeX + unroundedLayout.border.left) - round(cumulativeX)
    layout.border.right =
        round(cumulativeX + unroundedLayout.size.width) - round(cumulativeX + unroundedLayout.size.width - unroundedLayout.border.right)
    layout.border.top = round(cumulativeY + unroundedLayout.border.top) - round(cumulativeY)
    layout.border.bottom =
        round(cumulativeY + unroundedLayout.size.height) - round(cumulativeY + unroundedLayout.size.height - unroundedLayout.border.bottom)
    layout.padding.left = round(cumulativeX + unroundedLayout.padding.left) - round(cumulativeX)
    layout.padding.right =
        round(cumulativeX + unroundedLayout.size.width) - round(cumulativeX + unroundedLayout.size.width - unroundedLayout.padding.right)
    layout.padding.top = round(cumulativeY + unroundedLayout.padding.top) - round(cumulativeY)
    layout.padding.bottom =
        round(cumulativeY + unroundedLayout.size.height) - round(cumulativeY + unroundedLayout.size.height - unroundedLayout.padding.bottom)

    roundContentSize(layout, unroundedLayout.contentSize, cumulativeX, cumulativeY)

    tree.setFinalLayout(nodeId, layout)

    val childCount = tree.childCount(nodeId)
    for (index in 0 until childCount) {
        val child = tree.getChildId(nodeId, index)
        roundLayoutInner(tree, child, cumulativeX, cumulativeY)
    }
}

/// Round content size variables.
/// This is split into a separate function to make it easier to feature flag.
private fun roundContentSize(
    layout: Layout,
    unroundedContentSize: Size<Float>,
    cumulativeX: Float,
    cumulativeY: Float,
) {
    layout.contentSize.width = round(cumulativeX + unroundedContentSize.width) - round(cumulativeX)
    layout.contentSize.height = round(cumulativeY + unroundedContentSize.height) - round(cumulativeY)
}

fun computeHiddenLayout(
    tree: LayoutPartialTree,
    node: Int
): LayoutOutput {
    // Clear cache and set zeroed-out layout for the node
    tree.getCache(node).clear()
    tree.setUnroundedLayout(node, Layout.withOrder(0))

    // Perform hidden layout on all children
    for (index in 0 until tree.childCount(node)) {
        val childId = tree.getChildId(node, index)
        tree.computeChildLayout(childId, LayoutInput.HIDDEN)
    }

    return LayoutOutput.HIDDEN
}
