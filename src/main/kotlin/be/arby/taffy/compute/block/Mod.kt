package be.arby.taffy.compute.block

import be.arby.taffy.compute.common.computeContentSizeContribution
import be.arby.taffy.geom.*
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.collections.enumerate
import be.arby.taffy.lang.compareTo
import be.arby.taffy.lang.f32Max
import be.arby.taffy.lang.tuples.T4
import be.arby.taffy.style.BoxGenerationMode
import be.arby.taffy.style.BoxSizing
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.Position
import be.arby.taffy.style.block.TextAlign
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.NodeId
import be.arby.taffy.tree.layout.*
import be.arby.taffy.tree.traits.LayoutBlockContainer
import be.arby.taffy.tree.traits.LayoutPartialTree
import be.arby.taffy.util.*

fun computeBlockLayout(
    tree: LayoutBlockContainer,
    nodeId: NodeId,
    inputs: LayoutInput,
): LayoutOutput {
    val knownDimensions = inputs.knownDimensions
    val parentSize = inputs.parentSize
    val runMode = inputs.runMode

    val style = tree.getBlockContainerStyle(nodeId)

    // Pull these out earlier to avoid borrowing issues
    val aspectRatio = style.aspectRatio()
    val padding = style.padding().resolveOrZero(parentSize.width)
    val border = style.border().resolveOrZero(parentSize.width)
    val paddingBorderSize = (padding + border).sumAxes()
    val boxSizingAdjustment = if (style.boxSizing() == BoxSizing.CONTENT_BOX) paddingBorderSize else Size.ZERO

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
    val clampedStyleSize = if (inputs.sizingMode == SizingMode.INHERENT_SIZE) {
        style
            .size()
            .maybeResolve(parentSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
            .maybeClamp(minSize, maxSize)
    } else {
        Size.NONE
    }

    // If both min and max in a given axis are set and max <= min then this determines the size in that axis
    val minMaxDefiniteSize = minSize.zipMap(maxSize) { min, max ->
        if (min.isSome() && max.isSome() && max <= min) min.copy() else Option.None
    }

    val styledBasedKnownDimensions =
        knownDimensions.or(minMaxDefiniteSize).or(clampedStyleSize).maybeMax(paddingBorderSize)

    // Short-circuit layout if the container's size is fully determined by the container's size and the run mode
    // is ComputeSize (and thus the container's size is all that we're interested in)
    if (runMode == RunMode.COMPUTE_SIZE && styledBasedKnownDimensions.bothAxisDefined()) {
        return LayoutOutput.fromOuterSize(
            Size(
                styledBasedKnownDimensions.width.unwrap(),
                styledBasedKnownDimensions.height.unwrap()
            )
        )
    }

    return computeInner(tree, nodeId, inputs.copy(knownDimensions = styledBasedKnownDimensions))
}

/**
 * Computes the layout of [LayoutBlockContainer] according to the block layout algorithm
 */
fun computeInner(tree: LayoutBlockContainer, nodeId: NodeId, inputs: LayoutInput): LayoutOutput {
    val knownDimensions = inputs.knownDimensions
    val parentSize = inputs.parentSize
    val availableSpace = inputs.availableSpace
    val runMode = inputs.runMode
    val verticalMarginsAreCollapsible = inputs.verticalMarginsAreCollapsible

    val style = tree.getBlockContainerStyle(nodeId)
    val rawPadding = style.padding()
    val rawBorder = style.border()
    val rawMargin = style.margin()
    val aspectRatio = style.aspectRatio()
    val padding = rawPadding.resolveOrZero(parentSize.width)
    val border = rawBorder.resolveOrZero(parentSize.width)

    // Scrollbar gutters are reserved when the `overflow` property is set to `Overflow::Scroll`.
    // However, the axis are switched (transposed) because a node that scrolls vertically needs
    // *horizontal* space to be reserved for a scrollbar
    val scrollbarGutter = run {
        val offsets = style.overflow().transpose().map { overflow ->
            if (overflow == Overflow.SCROLL) style.scrollbarWidth() else 0f
        }
        // TODO: make side configurable based on the `direction` property
        Rect(top = 0f, left = 0f, right = offsets.x, bottom = offsets.y)
    }
    val paddingBorder = padding + border
    val paddingBorderSize = paddingBorder.sumAxes()
    val contentBoxInset = paddingBorder + scrollbarGutter
    val containerContentBoxSize = knownDimensions.maybeSub(contentBoxInset.sumAxes())

    val boxSizingAdjustment =
        if (style.boxSizing() == BoxSizing.CONTENT_BOX) paddingBorderSize else Size.ZERO
    val size = style.size().maybeResolve(parentSize).maybeApplyAspectRatio(aspectRatio).maybeAdd(boxSizingAdjustment)
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

    // Determine margin collapsing behaviour
    val ownMarginsCollapseWithChildren = Line(
        start = verticalMarginsAreCollapsible.start
                && !style.overflow().x.isScrollContainer()
                && !style.overflow().y.isScrollContainer()
                && style.position() == Position.RELATIVE
                && padding.top == 0f
                && border.top == 0f,
        end = verticalMarginsAreCollapsible.end
                && !style.overflow().x.isScrollContainer()
                && !style.overflow().y.isScrollContainer()
                && style.position() == Position.RELATIVE
                && padding.bottom == 0f
                && border.bottom == 0f
                && size.height.isNone(),
    )
    val hasStylesPreventingBeingCollapsedThrough = !style.isBlock()
            || style.overflow().x.isScrollContainer()
            || style.overflow().y.isScrollContainer()
            || style.position() == Position.ABSOLUTE
            || padding.top > 0f
            || padding.bottom > 0f
            || border.top > 0f
            || border.bottom > 0f
            || if (size.height.isSome()) size.height > 0f else
        if (minSize.height.isSome()) minSize.height > 0f else false

    val textAlign = style.textAlign()

    // 1. Generate items
    var items = generateItemList(tree, nodeId, containerContentBoxSize)

    // 2. Compute container width
    val containerOuterWidth = knownDimensions.width.unwrapOrElse {
        val availableWidth = availableSpace.width.maybeSub(contentBoxInset.horizontalAxisSum())
        val intrinsicWidth =
            determineContentBasedContainerWidth(tree, items, availableWidth) + contentBoxInset.horizontalAxisSum()
        intrinsicWidth.maybeClamp(minSize.width, maxSize.width).maybeMax(Option.Some(paddingBorderSize.width))
    }

    // Short-circuit if computing size and both dimensions known
    if (runMode == RunMode.COMPUTE_SIZE && knownDimensions.height.isSome()) {
        return LayoutOutput.fromOuterSize(Size(width = containerOuterWidth, height = knownDimensions.height.unwrap()))
    }

    // 3. Perform final item layout and return content height
    val resolvedPadding = rawPadding.resolveOrZero(Option.Some(containerOuterWidth))
    val resolvedBorder = rawBorder.resolveOrZero(Option.Some(containerOuterWidth))
    val resolvedContentBoxInset = resolvedPadding + resolvedBorder + scrollbarGutter
    val (inflowContentSize, intrinsicOuterHeight, firstChildTopMarginSet, lastChildBottomMarginSet) =
        performFinalLayoutOnInFlowChildren(
            tree,
            items,
            containerOuterWidth,
            contentBoxInset,
            resolvedContentBoxInset,
            textAlign,
            ownMarginsCollapseWithChildren
        )
    val containerOuterHeight = knownDimensions
        .height
        .unwrapOr(intrinsicOuterHeight.maybeClamp(minSize.height, maxSize.height))
        .maybeMax(Option.Some(paddingBorderSize.height))
    val finalOuterSize = Size(width = containerOuterWidth, height = containerOuterHeight)

    // Short-circuit if computing size
    if (runMode == RunMode.COMPUTE_SIZE) {
        return LayoutOutput.fromOuterSize(finalOuterSize)
    }

    // 4. Layout absolutely positioned children
    val absolutePositionInset = resolvedBorder + scrollbarGutter
    val absolutePositionArea = finalOuterSize - absolutePositionInset.sumAxes()
    val absolutePositionOffset = Point(x = absolutePositionInset.left, y = absolutePositionInset.top)
    val absoluteContentSize =
        performAbsoluteLayoutOnAbsoluteChildren(tree, items, absolutePositionArea, absolutePositionOffset)

    // 5. Perform hidden layout on hidden children
    val len = tree.childCount(nodeId)
    for (order in 0 until len) {
        val child = tree.getChildId(nodeId, order)
        if (tree.getBlockChildStyle(child).boxGenerationMode() == BoxGenerationMode.NONE) {
            tree.setUnroundedLayout(child, Layout.withOrder(order))
            tree.performChildLayout(
                child,
                Size.NONE,
                Size.NONE,
                Size.MAX_CONTENT,
                SizingMode.INHERENT_SIZE,
                Line.FALSE
            )
        }
    }

    // 7. Determine whether this node can be collapsed through
    val allInFlowChildrenCanBeCollapsedThrough =
        items.all { item -> item.position == Position.ABSOLUTE || item.canBeCollapsedThrough }
    val canBeCollapsedThrough =
        !hasStylesPreventingBeingCollapsedThrough && allInFlowChildrenCanBeCollapsedThrough

    val contentSize = inflowContentSize.f32Max(absoluteContentSize)

    return LayoutOutput(
        size = finalOuterSize,
        contentSize = contentSize,
        firstBaselines = Point.NONE,
        topMargin = if (ownMarginsCollapseWithChildren.start) {
            firstChildTopMarginSet
        } else {
            val marginTop = rawMargin.top.resolveOrZero(parentSize.width)
            CollapsibleMarginSet.fromMargin(marginTop)
        },
        bottomMargin = if (ownMarginsCollapseWithChildren.end) {
            lastChildBottomMarginSet
        } else {
            val marginBottom = rawMargin.bottom.resolveOrZero(parentSize.width)
            CollapsibleMarginSet.fromMargin(marginBottom)
        },
        marginsCanCollapseThrough = canBeCollapsedThrough
    )
}

/**
 * Create a `Vec` of `BlockItem` structs where each item in the `Vec` represents a child of the current node
 */
fun generateItemList(
    tree: LayoutBlockContainer,
    node: NodeId,
    nodeInnerSize: Size<Option<Float>>,
): List<BlockItem> {
    return tree.childIds(node)
        .map { childNodeId -> Pair(childNodeId, tree.getBlockChildStyle(childNodeId)) }
        .filter { (_, style) -> style.boxGenerationMode() != BoxGenerationMode.NONE }
        .enumerate()
        .map { (order, pair) ->
            val (childNodeId, childStyle) = pair

            val aspectRatio = childStyle.aspectRatio()
            val padding = childStyle.padding().resolveOrZero(nodeInnerSize)
            val border = childStyle.border().resolveOrZero(nodeInnerSize)
            val pbSum = (padding + border).sumAxes()
            val boxSizingAdjustment =
                if (childStyle.boxSizing() == BoxSizing.CONTENT_BOX) {
                    pbSum
                } else {
                    Size.ZERO
                }
            BlockItem(
                nodeId = childNodeId,
                order = order,
                isTable = childStyle.isTable(),
                size = childStyle
                    .size()
                    .maybeResolve(nodeInnerSize)
                    .maybeApplyAspectRatio(aspectRatio)
                    .maybeAdd(boxSizingAdjustment),
                minSize = childStyle
                    .minSize()
                    .maybeResolve(nodeInnerSize)
                    .maybeApplyAspectRatio(aspectRatio)
                    .maybeAdd(boxSizingAdjustment),
                maxSize = childStyle
                    .maxSize()
                    .maybeResolve(nodeInnerSize)
                    .maybeApplyAspectRatio(aspectRatio)
                    .maybeAdd(boxSizingAdjustment),
                overflow = childStyle.overflow(),
                scrollbarWidth = childStyle.scrollbarWidth(),
                position = childStyle.position(),
                inset = childStyle.inset(),
                margin = childStyle.margin(),
                padding,
                border,
                paddingBorderSum = pbSum,

                // Fields to be computed later (for now we initialise with dummy values)
                computedSize = Size.zeroF(),
                staticPosition = Point.zeroF(),
                canBeCollapsedThrough = false
            )
        }
}

/**
 * Compute the content-based width in the case that the width of the container is not known
 */
fun determineContentBasedContainerWidth(
    tree: LayoutPartialTree,
    items: List<BlockItem>,
    availableWidth: AvailableSpace
): Float {
    val availableSpace = Size(width = availableWidth, height = AvailableSpace.MinContent)

    var maxChildWidth = 0f
    for (item in items.filter { item -> item.position != Position.ABSOLUTE }) {
        val knownDimensions = item.size.maybeClamp(item.minSize, item.maxSize)

        var width = knownDimensions.width.unwrapOrElse {
            val itemXMarginSum =
                item.margin.resolveOrZero(availableSpace.width.intoOption()).horizontalAxisSum()
            val sizeAndBaselines = tree.performChildLayout(
                item.nodeId,
                knownDimensions,
                Size.NONE,
                availableSpace.mapWidth { w -> w.maybeSub(itemXMarginSum) },
                SizingMode.INHERENT_SIZE,
                Line.TRUE
            )

            sizeAndBaselines.size.width + itemXMarginSum
        }
        width = f32Max(width, item.paddingBorderSum.width)

        maxChildWidth = f32Max(maxChildWidth, width)
    }

    return maxChildWidth
}

/**
 * Compute each child's final size and position
 */
fun performFinalLayoutOnInFlowChildren(
    tree: LayoutPartialTree,
    items: List<BlockItem>,
    containerOuterWidth: Float,
    contentBoxInset: Rect<Float>,
    resolvedContentBoxInset: Rect<Float>,
    textAlign: TextAlign,
    ownMarginsCollapseWithChildren: Line<Boolean>,
): T4<Size<Float>, Float, CollapsibleMarginSet, CollapsibleMarginSet> {
    // Resolve container_inner_width for sizing child nodes using initial content_box_inset
    val containerInnerWidth = containerOuterWidth - contentBoxInset.horizontalAxisSum()
    val parentSize = Size(width = Option.Some(containerOuterWidth), height = Option.None)
    val availableSpace =
        Size(width = AvailableSpace.Definite(containerInnerWidth), height = AvailableSpace.MinContent)

    var inflowContentSize = Size.ZERO
    var committedYOffset = resolvedContentBoxInset.top
    var firstChildTopMarginSet = CollapsibleMarginSet.ZERO
    var activeCollapsibleMarginSet = CollapsibleMarginSet.ZERO
    var isCollapsingWithFirstMarginSet = true
    for (item in items) {
        if (item.position == Position.ABSOLUTE) {
            item.staticPosition = Point(x = resolvedContentBoxInset.left, y = committedYOffset)
        } else {
            val itemMargin = item.margin.map { margin -> margin.resolveToOption(containerOuterWidth) }
            val itemNonAutoMargin = itemMargin.map { m -> m.unwrapOr(0f) }
            val itemNonAutoXMarginSum = itemNonAutoMargin.horizontalAxisSum()
            val knownDimensions = if (item.isTable) {
                Size.NONE
            } else {
                item.size
                    .mapWidth { width ->
                        // TODO: Allow stretch-sizing to be conditional, as there are exceptions.
                        // e.g. Table children of blocks do not stretch fit
                        Option.Some(
                            width
                                .unwrapOr(containerInnerWidth - itemNonAutoXMarginSum)
                                .maybeClamp(item.minSize.width, item.maxSize.width)
                        )
                    }
                    .maybeClamp(item.minSize, item.maxSize)
            }

            val itemLayout = tree.performChildLayout(
                item.nodeId,
                knownDimensions,
                parentSize,
                availableSpace.mapWidth { w -> w.maybeSub(itemNonAutoXMarginSum) },
                SizingMode.INHERENT_SIZE,
                Line.TRUE,
            )
            val finalSize = itemLayout.size

            val topMarginSet = itemLayout.topMargin.collapseWithMargin(itemMargin.top.unwrapOr(0f))
            val bottomMarginSet = itemLayout.bottomMargin.collapseWithMargin(itemMargin.bottom.unwrapOr(0f))

            // Expand auto margins to fill available space
            // Note: Vertical auto-margins for relatively positioned block items simply resolve to 0.
            // See: https://www.w3.org/TR/CSS21/visudet.html#abs-non-replaced-width
            val freeXSpace = f32Max(0f, containerInnerWidth - finalSize.width - itemNonAutoXMarginSum)
            val xAxisAutoMarginSize = run {
                val autoMarginCount = itemMargin.left.isNone().toInt() + itemMargin.right.isNone().toInt()
                if (autoMarginCount > 0) {
                    freeXSpace / autoMarginCount.toFloat()
                } else {
                    0f
                }
            }
            val resolvedMargin = Rect(
                left = itemMargin.left.unwrapOr(xAxisAutoMarginSize),
                right = itemMargin.right.unwrapOr(xAxisAutoMarginSize),
                top = topMarginSet.resolve(),
                bottom = bottomMarginSet.resolve(),
            )

            // Resolve item inset
            val inset =
                item.inset.zipSize(Size(width = containerInnerWidth, height = 0.0f)) { p, s -> p.maybeResolve(s) }
            val insetOffset = Point(
                x = inset.left.or(inset.right.map { x -> -x }).unwrapOr(0f),
                y = inset.top.or(inset.bottom.map { x -> -x }).unwrapOr(0f),
            )

            val yMarginOffset = if (isCollapsingWithFirstMarginSet && ownMarginsCollapseWithChildren.start) {
                0f
            } else {
                activeCollapsibleMarginSet.collapseWithMargin(resolvedMargin.top).resolve()
            }

            item.computedSize = itemLayout.size
            item.canBeCollapsedThrough = itemLayout.marginsCanCollapseThrough
            item.staticPosition = Point(
                x = resolvedContentBoxInset.left,
                y = committedYOffset + activeCollapsibleMarginSet.resolve(),
            )
            var location = Point(
                x = resolvedContentBoxInset.left + insetOffset.x + resolvedMargin.left,
                y = committedYOffset + insetOffset.y + yMarginOffset,
            )

            // Apply alignment
            val itemOuterWidth = itemLayout.size.width + resolvedMargin.horizontalAxisSum()
            if (itemOuterWidth < containerInnerWidth) {
                when (textAlign) {
                    TextAlign.AUTO -> {
                        // Do nothing
                    }

                    TextAlign.LEGACY_LEFT -> {
                        // Do nothing. Left aligned by default.
                    }

                    TextAlign.LEGACY_RIGHT -> location.x += containerInnerWidth - itemOuterWidth
                    TextAlign.LEGACY_CENTER -> location.x += (containerInnerWidth - itemOuterWidth) / 2f
                }
            }

            val scrollbarSize = Size(
                width = if (item.overflow.y == Overflow.SCROLL) item.scrollbarWidth else 0f,
                height = if (item.overflow.x == Overflow.SCROLL) item.scrollbarWidth else 0f
            )

            tree.setUnroundedLayout(
                item.nodeId,
                Layout(
                    order = item.order,
                    size = itemLayout.size,
                    contentSize = itemLayout.contentSize,
                    scrollbarSize = scrollbarSize,
                    location = location,
                    padding = item.padding,
                    border = item.border,
                    margin = resolvedMargin
                )
            )

            inflowContentSize = inflowContentSize.f32Max(
                computeContentSizeContribution(
                    location,
                    finalSize,
                    itemLayout.contentSize,
                    item.overflow
                )
            )

            // Update first_child_top_margin_set
            if (isCollapsingWithFirstMarginSet) {
                if (item.canBeCollapsedThrough) {
                    firstChildTopMarginSet = firstChildTopMarginSet
                        .collapseWithSet(topMarginSet)
                        .collapseWithSet(bottomMarginSet)
                } else {
                    firstChildTopMarginSet = firstChildTopMarginSet.collapseWithSet(topMarginSet)
                    isCollapsingWithFirstMarginSet = false
                }
            }

            // Update active_collapsible_margin_set
            if (item.canBeCollapsedThrough) {
                activeCollapsibleMarginSet = activeCollapsibleMarginSet
                    .collapseWithSet(topMarginSet)
                    .collapseWithSet(bottomMarginSet)
            } else {
                committedYOffset += itemLayout.size.height + yMarginOffset
                activeCollapsibleMarginSet = bottomMarginSet
            }
        }
    }

    val lastChildBottomMarginSet = activeCollapsibleMarginSet
    val bottomYMarginOffset =
        if (ownMarginsCollapseWithChildren.end) 0f else lastChildBottomMarginSet.resolve()

    committedYOffset += resolvedContentBoxInset.bottom + bottomYMarginOffset
    val contentHeight = f32Max(0f, committedYOffset)
    return T4(inflowContentSize, contentHeight, firstChildTopMarginSet, lastChildBottomMarginSet)
}

fun performAbsoluteLayoutOnAbsoluteChildren(
    tree: LayoutBlockContainer,
    items: List<BlockItem>,
    areaSize: Size<Float>,
    areaOffset: Point<Float>,
): Size<Float> {
    val areaWidth = areaSize.width
    val areaHeight = areaSize.height

    var absoluteContentSize = Size.ZERO

    for (item in items.filter { item -> item.position == Position.ABSOLUTE }) {
        val childStyle = tree.getBlockChildStyle(item.nodeId)

        // Skip items that are display:none or are not position:absolute
        if (childStyle.boxGenerationMode() == BoxGenerationMode.NONE || childStyle.position() != Position.ABSOLUTE) {
            continue
        }

        val aspectRatio = childStyle.aspectRatio()
        val margin = childStyle.margin().map { margin -> margin.resolveToOption(areaWidth) }
        val padding = childStyle.padding().resolveOrZero(Option.Some(areaWidth))
        val border = childStyle.border().resolveOrZero(Option.Some(areaWidth))
        val paddingBorderSum = (padding + border).sumAxes()
        val boxSizingAdjustment =
            if (childStyle.boxSizing() == BoxSizing.CONTENT_BOX) paddingBorderSum else Size.ZERO

        // Resolve inset
        val left = childStyle.inset().left.maybeResolve(areaWidth)
        val right = childStyle.inset().right.maybeResolve(areaWidth)
        val top = childStyle.inset().top.maybeResolve(areaHeight)
        val bottom = childStyle.inset().bottom.maybeResolve(areaHeight)

        // Compute known dimensions from min/max/inherent size styles
        val styleSize = childStyle
            .size()
            .maybeResolve(areaSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
        val minSize = childStyle
            .minSize()
            .maybeResolve(areaSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
            .or(paddingBorderSum.map { v -> Option.Some(v) })
            .maybeMax(paddingBorderSum)
        val maxSize = childStyle
            .maxSize()
            .maybeResolve(areaSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
        var knownDimensions = styleSize.maybeClamp(minSize, maxSize)

        // Fill in width from left/right and reapply aspect ratio if:
        //   - Width is not already known
        //   - Item has both left and right inset properties set
        if (knownDimensions.width.isNone() && left.isSome() && right.isSome()) {
            val newWidthRaw = areaWidth.maybeSub(margin.left).maybeSub(margin.right) - left.unwrap() - right.unwrap()
            knownDimensions.width = Option.Some(f32Max(newWidthRaw, 0f))
            knownDimensions = knownDimensions.maybeApplyAspectRatio(aspectRatio).maybeClamp(minSize, maxSize)
        }

        // Fill in height from top/bottom and reapply aspect ratio if:
        //   - Height is not already known
        //   - Item has both top and bottom inset properties set
        if (knownDimensions.height.isNone() && top.isSome() && bottom.isSome()) {
            val newHeightRaw = areaHeight.maybeSub(margin.top).maybeSub(margin.bottom) - top.unwrap() - bottom.unwrap()
            knownDimensions.height = Option.Some(f32Max(newHeightRaw, 0f))
            knownDimensions = knownDimensions.maybeApplyAspectRatio(aspectRatio).maybeClamp(minSize, maxSize)
        }

        val layoutOutput = tree.performChildLayout(
            item.nodeId,
            knownDimensions,
            areaSize.map { v -> Option.Some(v) },
            Size(
                width = AvailableSpace.Definite(areaWidth.maybeClamp(minSize.width, maxSize.width)),
                height = AvailableSpace.Definite(areaHeight.maybeClamp(minSize.height, maxSize.height)),
            ),
            SizingMode.CONTENT_SIZE,
            Line.FALSE,
        )
        val measuredSize = layoutOutput.size
        val finalSize = knownDimensions.unwrapOr(measuredSize).maybeClamp(minSize, maxSize)

        val nonAutoMargin = Rect(
            left = if (left.isSome()) margin.left.unwrapOr(0f) else 0f,
            right = if (right.isSome()) margin.right.unwrapOr(0f) else 0f,
            top = if (top.isSome()) margin.top.unwrapOr(0f) else 0f,
            bottom = if (bottom.isSome()) margin.left.unwrapOr(0f) else 0f
        )

        // Expand auto margins to fill available space
        // https://www.w3.org/TR/CSS21/visudet.html#abs-non-replaced-width
        val autoMargin = run {
            // Auto margins for absolutely positioned elements in block containers only resolve
            // if inset is set. Otherwise they resolve to 0.
            val absoluteAutoMarginSpace = Point(
                x = right.map { right -> areaSize.width - right - left.unwrapOr(0f) }.unwrapOr(finalSize.width),
                y = bottom.map { bottom -> areaSize.height - bottom - top.unwrapOr(0f) }.unwrapOr(finalSize.height),
            )
            val freeSpace = Size(
                width = absoluteAutoMarginSpace.x - finalSize.width - nonAutoMargin.horizontalAxisSum(),
                height = absoluteAutoMarginSpace.y - finalSize.height - nonAutoMargin.verticalAxisSum()
            )

            val autoMarginSize = Size(
                // If all three of 'left', 'width', and 'right' are 'auto': First set any 'auto' values for 'margin-left' and 'margin-right' to 0.
                // Then, if the 'direction' property of the element establishing the static-position containing block is 'ltr' set 'left' to the
                // static position and apply rule number three below; otherwise, set 'right' to the static position and apply rule number one below.
                //
                // If none of the three is 'auto': If both 'margin-left' and 'margin-right' are 'auto', solve the equation under the extra constraint
                // that the two margins get equal values, unless this would make them negative, in which case when direction of the containing block is
                // 'ltr' ('rtl'), set 'margin-left' ('margin-right') to zero and solve for 'margin-right' ('margin-left'). If one of 'margin-left' or
                // 'margin-right' is 'auto', solve the equation for that value. If the values are over-constrained, ignore the value for 'left' (in case
                // the 'direction' property of the containing block is 'rtl') or 'right' (in case 'direction' is 'ltr') and solve for that value.
                width = run {
                    val autoMarginCount = margin.left.isNone().toInt() + margin.right.isNone().toInt()
                    if (autoMarginCount == 2
                        && (styleSize.width.isNone() || styleSize.width.unwrap() >= freeSpace.width)
                    ) {
                        0f
                    } else if (autoMarginCount > 0) {
                        freeSpace.width / autoMarginCount.toFloat()
                    } else {
                        0f
                    }
                },
                height = run {
                    val autoMarginCount = margin.top.isNone().toInt() + margin.bottom.isNone().toInt()
                    if (autoMarginCount == 2
                        && (styleSize.height.isNone() || styleSize.height.unwrap() >= freeSpace.height)
                    ) {
                        0f
                    } else if (autoMarginCount > 0) {
                        freeSpace.height / autoMarginCount.toFloat()
                    } else {
                        0f
                    }
                },
            )

            Rect(
                left = margin.left.map { 0f }.unwrapOr(autoMarginSize.width),
                right = margin.right.map { 0f }.unwrapOr(autoMarginSize.width),
                top = margin.top.map { 0f }.unwrapOr(autoMarginSize.height),
                bottom = margin.bottom.map { 0f }.unwrapOr(autoMarginSize.height),
            )
        }

        val resolvedMargin = Rect(
            left = margin.left.unwrapOr(autoMargin.left),
            right = margin.right.unwrapOr(autoMargin.right),
            top = margin.top.unwrapOr(autoMargin.top),
            bottom = margin.bottom.unwrapOr(autoMargin.bottom),
        )

        val location = Point(
            x = left
                .map { left -> left + resolvedMargin.left }
                .or(right.map { right -> areaSize.width - finalSize.width - right - resolvedMargin.right })
                .maybeAdd(areaOffset.x)
                .unwrapOr(item.staticPosition.x + resolvedMargin.left),
            y = top
                .map { top -> top + resolvedMargin.top }
                .or(bottom.map { bottom -> areaSize.height - finalSize.height - bottom - resolvedMargin.bottom })
                .maybeAdd(areaOffset.y)
                .unwrapOr(item.staticPosition.y + resolvedMargin.top),
        )
        // Note: axis intentionally switched here as scrollbars take up space in the opposite axis
        // to the axis in which scrolling is enabled.
        val scrollbarSize = Size(
            width = if (item.overflow.y == Overflow.SCROLL) item.scrollbarWidth else 0f,
            height = if (item.overflow.x == Overflow.SCROLL) item.scrollbarWidth else 0f
        )

        tree.setUnroundedLayout(
            item.nodeId,
            Layout(
                order = item.order,
                size = finalSize,
                contentSize = layoutOutput.contentSize,
                scrollbarSize = scrollbarSize,
                location = location,
                padding = padding,
                border = border,
                margin = resolvedMargin,
            ),
        )

        absoluteContentSize = absoluteContentSize.f32Max(
            computeContentSizeContribution(
                location,
                finalSize,
                layoutOutput.contentSize,
                item.overflow,
            )
        )
    }

    return absoluteContentSize
}
