package be.arby.taffy.compute

import be.arby.taffy.geom.*
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.f32Max
import be.arby.taffy.lang.tuples.T4
import be.arby.taffy.style.BoxSizing
import be.arby.taffy.style.CoreStyle
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.Position
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.layout.*
import be.arby.taffy.util.maybeAdd
import be.arby.taffy.util.maybeClamp
import be.arby.taffy.util.maybeMax
import be.arby.taffy.util.maybeSub

/**
 * Compute the size of a leaf node (node with no children)
 */
fun <MeasureFunction : (Size<Option<Float>>, Size<AvailableSpace>) -> Size<Float>> computeLeafLayout(
    inputs: LayoutInput,
    style: CoreStyle,
    measureFunction: MeasureFunction
): LayoutOutput {
    val knownDimensions = inputs.knownDimensions
    val parentSize = inputs.parentSize
    var availableSpace = inputs.availableSpace
    val sizingMode = inputs.sizingMode
    val runMode = inputs.runMode

    // Note: both horizontal and vertical percentage padding/borders are resolved against the container's inline size (i.e. width).
    // This is not a bug, but is how CSS is specified (see: https://developer.mozilla.org/en-US/docs/Web/CSS/padding#values)
    val margin = style.margin().resolveOrZero(parentSize.width)
    val padding = style.padding().resolveOrZero(parentSize.width)
    val border = style.border().resolveOrZero(parentSize.width)
    val paddingBorder = padding + border
    val pbSum = paddingBorder.sumAxes()
    val boxSizingAdjustment = if (style.boxSizing() == BoxSizing.CONTENT_BOX) pbSum else Size.ZERO

    // Resolve node's preferred/min/max sizes (width/heights) against the available space (percentages resolve to pixel values)
    // For ContentSize mode, we pretend that the node has no size styles as these should be ignored.
    val (nodeSize, nodeMinSize, nodeMaxSize, aspectRatio) = when (sizingMode) {
        SizingMode.CONTENT_SIZE -> {
            val nodeSize = knownDimensions
            val nodeMinSize = Size.NONE
            val nodeMaxSize = Size.NONE
            T4(nodeSize, nodeMinSize, nodeMaxSize, Option.None)
        }

        SizingMode.INHERENT_SIZE -> {
            val aspectRatio = style.aspectRatio()
            val styleSize = style
                .size()
                .maybeResolve(parentSize)
                .maybeApplyAspectRatio(aspectRatio)
                .maybeAdd(boxSizingAdjustment)
            val styleMinSize = style
                .minSize()
                .maybeResolve(parentSize)
                .maybeApplyAspectRatio(aspectRatio)
                .maybeAdd(boxSizingAdjustment)
            val styleMaxSize = style.maxSize().maybeResolve(parentSize).maybeAdd(boxSizingAdjustment)

            val nodeSize = knownDimensions.or(styleSize)
            T4(nodeSize, styleMinSize, styleMaxSize, aspectRatio)
        }
    }

    // Scrollbar gutters are reserved when the `overflow` property is set to `Overflow::Scroll`.
    // However, the axis are switched (transposed) because a node that scrolls vertically needs
    // *horizontal* space to be reserved for a scrollbar
    val scrollbarGutter = style.overflow().transpose().map { overflow ->
        if (overflow == Overflow.SCROLL) style.scrollbarWidth() else 0f
    }
    // TODO: make side configurable based on the `direction` property
    val contentBoxInset = paddingBorder
    contentBoxInset.right += scrollbarGutter.x
    contentBoxInset.bottom += scrollbarGutter.y

    val hasStylesPreventingBeingCollapsedThrough = !style.isBlock()
            || style.overflow().x.isScrollContainer()
            || style.overflow().y.isScrollContainer()
            || style.position() == Position.ABSOLUTE
            || padding.top > 0f
            || padding.bottom > 0f
            || border.top > 0f
            || border.bottom > 0f
            || if (nodeSize.height.isSome()) nodeSize.height.unwrap() > 0f else
        if (nodeMinSize.height.isSome()) nodeMinSize.height.unwrap() > 0f else false

    // Return early if both width and height are known
    if (runMode == RunMode.COMPUTE_SIZE && hasStylesPreventingBeingCollapsedThrough) {
        if (nodeSize.width.isSome() && nodeSize.height.isSome()) {
            val width = nodeSize.width.unwrap()
            val height = nodeSize.height.unwrap()

            val size = Size(width, height)
                .maybeClamp(nodeMinSize, nodeMaxSize)
                .maybeMax(paddingBorder.sumAxes().map { v -> Option.Some(v) })
            return LayoutOutput(
                size,
                contentSize = Size.ZERO,
                firstBaselines = Point.NONE,
                topMargin = CollapsibleMarginSet.ZERO,
                bottomMargin = CollapsibleMarginSet.ZERO,
                marginsCanCollapseThrough = false
            )
        }
    }

    // Compute available space
    availableSpace = Size(
        width = knownDimensions
            .width
            .map(AvailableSpace::from1)
            .unwrapOr(availableSpace.width)
            .maybeSub(margin.horizontalAxisSum())
            .maybeSet(knownDimensions.width)
            .maybeSet(nodeSize.width)
            .maybeSet(nodeMaxSize.width)
            .mapDefiniteValue { size ->
                size.maybeClamp(nodeMinSize.width, nodeMaxSize.width) - contentBoxInset.horizontalAxisSum()
            },
        height = knownDimensions
            .height
            .map(AvailableSpace::from1)
            .unwrapOr(availableSpace.height)
            .maybeSub(margin.verticalAxisSum())
            .maybeSet(knownDimensions.height)
            .maybeSet(nodeSize.height)
            .maybeSet(nodeMaxSize.height)
            .mapDefiniteValue { size ->
                size.maybeClamp(nodeMinSize.height, nodeMaxSize.height) - contentBoxInset.verticalAxisSum()
            }
    )

    // Measure node
    val measuredSize = measureFunction(
        when (runMode) {
            RunMode.COMPUTE_SIZE -> knownDimensions
            RunMode.PERFORM_LAYOUT -> Size.NONE
            RunMode.PERFORM_HIDDEN_LAYOUT -> {
                throw Exception("Shouldn't happen")
            }
        },
        availableSpace
    )
    val clampedSize = knownDimensions
        .or(nodeSize)
        .unwrapOr(measuredSize + contentBoxInset.sumAxes())
        .maybeClamp(nodeMinSize, nodeMaxSize)
    var size = Size(
        width = clampedSize.width,
        height = f32Max(clampedSize.height, aspectRatio.map { ratio -> clampedSize.width / ratio }.unwrapOr(0f)),
    )
    size = size.maybeMax(paddingBorder.sumAxes().map { v -> Option.Some(v) })

    return LayoutOutput(
        size,
        contentSize = measuredSize + padding.sumAxes(),
        firstBaselines = Point.NONE,
        topMargin = CollapsibleMarginSet.ZERO,
        bottomMargin = CollapsibleMarginSet.ZERO,
        marginsCanCollapseThrough = !hasStylesPreventingBeingCollapsedThrough && size.height == 0f && measuredSize.height == 0f
    )
}
