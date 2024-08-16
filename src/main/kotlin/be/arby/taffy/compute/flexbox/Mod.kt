package be.arby.taffy.compute.flexbox

import be.arby.taffy.compute.common.applyAlignmentFallback
import be.arby.taffy.compute.common.computeAlignmentOffset
import be.arby.taffy.compute.common.computeContentSizeContribution
import be.arby.taffy.geom.*
import be.arby.taffy.lang.*
import be.arby.taffy.lang.collections.*
import be.arby.taffy.lang.tuples.T2
import be.arby.taffy.maths.into
import be.arby.taffy.maths.intoAS
import be.arby.taffy.maths.isNormal
import be.arby.taffy.style.BoxGenerationMode
import be.arby.taffy.style.BoxSizing
import be.arby.taffy.style.Overflow
import be.arby.taffy.style.Position
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.alignment.AlignSelf
import be.arby.taffy.style.alignment.JustifyContent
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.flex.FlexDirection
import be.arby.taffy.style.flex.FlexWrap
import be.arby.taffy.style.flex.FlexboxContainerStyle
import be.arby.taffy.tree.layout.*
import be.arby.taffy.tree.traits.LayoutFlexboxContainer
import be.arby.taffy.util.*

/**
 * Computes the layout of a box according to the flexbox algorithm
 */
fun computeFlexboxLayout(
    tree: LayoutFlexboxContainer,
    node: Int,
    inputs: LayoutInput
): LayoutOutput {
    val knownDimensions = inputs.knownDimensions
    val parentSize = inputs.parentSize
    val runMode = inputs.runMode

    val style = tree.getFlexboxContainerStyle(node)

    // Pull these out earlier to avoid borrowing issues
    val aspectRatio = style.aspectRatio()
    val padding = style.padding().resolveOrZero(parentSize.width)
    val border = style.border().resolveOrZero(parentSize.width)
    val paddingBorderSum = padding.sumAxes() + border.sumAxes()
    val boxSizingAdjustment = if (style.boxSizing() == BoxSizing.CONTENT_BOX) {
        paddingBorderSum
    } else {
        Size.ZERO.clone()
    }

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
        Size.NONE.clone()
    }

    // If both min and max in a given axis are set and max <= min then this determines the size in that axis
    val minMaxDefiniteSize = minSize.zipMap(maxSize) { min, max ->
        if (min.isSome() && max.isSome() && max.unwrap() <= min.unwrap()) {
            Option.Some(min.unwrap())
        } else {
            Option.None
        }
    }

    // The size of the container should be floored by the padding and border
    val styledBasedKnownDimensions =
        knownDimensions.or(minMaxDefiniteSize.or(clampedStyleSize).maybeMax(paddingBorderSum))

    // Short-circuit layout if the container's size is fully determined by the container's size and the run mode
    // is ComputeSize (and thus the container's size is all that we're interested in)
    if (runMode == RunMode.COMPUTE_SIZE) {
        val width = styledBasedKnownDimensions.width
        val height = styledBasedKnownDimensions.height

        if (width.isSome() && height.isSome()) {
            return LayoutOutput.fromOuterSize(Size(width.unwrap(), height.unwrap()))
        }
    }

    return computePreliminary(
        tree,
        node,
        inputs.copy(knownDimensions = styledBasedKnownDimensions)
    )
}

/**
 * Compute a preliminary size for an item
 */
fun computePreliminary(tree: LayoutFlexboxContainer, node: Int, inputs: LayoutInput): LayoutOutput {
    val knownDimensions = inputs.knownDimensions
    val parentSize = inputs.parentSize
    var availableSpace = inputs.availableSpace
    val runMode = inputs.runMode

    // Define some general constants we will need for the remainder of the algorithm.
    val constants = computeConstants(tree.getFlexboxContainerStyle(node), knownDimensions, parentSize)

    // 9. Flex Layout Algorithm

    // 9.1. Initial Setup

    // 1. Generate anonymous flex items as described in §4 Flex Items.
    val flexItems = generateAnonymousFlexItems(tree, node, constants)

    // 9.2. Line Length Determination

    // 2. Determine the available main and cross space for the flex items
    availableSpace = determineAvailableSpace(knownDimensions, availableSpace, constants)

    // 3. Determine the flex base size and hypothetical main size of each item.
    determineFlexBaseSize(tree, constants, availableSpace, flexItems)

    // 4. Determine the main size of the flex container
    // This has already been done as part of compute_constants. The inner size is exposed as constants.node_inner_size.

    // 9.3. Main Size Determination

    // 5. Collect flex items into flex lines.
    val flexLines = collectFlexLines(constants, availableSpace, flexItems)

    // If container size is undefined, determine the container's main size
    // and then re-resolve gaps based on newly determined size
    val innerMainSize = constants.nodeInnerSize.main(constants.dir)

    if (innerMainSize.isSome()) {
        val outerMainSize = innerMainSize.unwrap() + constants.contentBoxInset.mainAxisSum(constants.dir)
        constants.innerContainerSize.setMain(constants.dir, innerMainSize.unwrap())
        constants.containerSize.setMain(constants.dir, outerMainSize)
    } else {
        // Sets constants.container_size and constants.outer_container_size
        determineContainerMainSize(tree, availableSpace, flexLines, constants)
        constants.nodeInnerSize.setMain(constants.dir, Option.Some(constants.innerContainerSize.main(constants.dir)))
        constants.nodeOuterSize.setMain(constants.dir, Option.Some(constants.containerSize.main(constants.dir)))

        // Re-resolve percentage gaps
        val style = tree.getFlexboxContainerStyle(node)
        val innerContainerSize = constants.innerContainerSize.main(constants.dir)
        val newGap = style.gap().main(constants.dir).maybeResolve(innerContainerSize).unwrapOr(0f)
        constants.gap.setMain(constants.dir, newGap)
    }

    // 6. Resolve the flexible lengths of all the flex items to find their used main size.
    for (line in flexLines) {
        resolveFlexibleLengths(line, constants)
    }

    // 9.4. Cross Size Determination

    // 7. Determine the hypothetical cross size of each item.
    for (line in flexLines) {
        determineHypotheticalCrossSize(tree, line, constants, availableSpace)
    }

    // Calculate child baselines. This function is internally smart and only computes child baselines
    // if they are necessary.
    calculateChildrenBaseLines(tree, knownDimensions, availableSpace, flexLines, constants)

    // 8. Calculate the cross size of each flex line.
    calculateCrossSize(flexLines, knownDimensions, constants)

    // 9. Handle 'align-content: stretch'.
    handleAlignContentStretch(flexLines, knownDimensions, constants)

    // 10. Collapse visibility:collapse items. If any flex items have visibility: collapse,
    //     note the cross size of the line they’re in as the item’s strut size, and restart
    //     layout from the beginning.
    //
    //     In this second layout round, when collecting items into lines, treat the collapsed
    //     items as having zero main size. For the rest of the algorithm following that step,
    //     ignore the collapsed items entirely (as if they were display:none) except that after
    //     calculating the cross size of the lines, if any line’s cross size is less than the
    //     largest strut size among all the collapsed items in the line, set its cross size to
    //     that strut size.
    //
    //     Skip this step in the second layout round.

    // TODO implement once (if ever) we support visibility:collapse

    // 11. Determine the used cross size of each flex item.
    determineUsedCrossSize(tree, flexLines, constants)

    // 9.5. Main-Axis Alignment

    // 12. Distribute any remaining free space.
    distributeRemainingFreeSpace(flexLines, constants)

    // 9.6. Cross-Axis Alignment

    // 13. Resolve cross-axis auto margins (also includes 14).
    resolveCrossAxisAutoMargins(flexLines, constants)

    // 15. Determine the flex container’s used cross size.
    val totalLineCrossSize = determineContainerCrossSize(flexLines, knownDimensions, constants)

    // We have the container size.
    // If our caller does not care about performing layout we are done now.
    if (runMode == RunMode.COMPUTE_SIZE) {
        return LayoutOutput.fromOuterSize(constants.containerSize)
    }

    // 16. Align all flex lines per align-content.
    alignFlexLinesPerAlignContent(flexLines, constants, totalLineCrossSize)

    // Do a final layout pass and gather the resulting layouts
    val inflowContentSize = finalLayoutPass(tree, flexLines, constants)

    // Before returning we perform absolute layout on all absolutely positioned children
    val absoluteContentSize = performAbsoluteLayoutOnAbsoluteChildren(tree, node, constants)
    val len = tree.childCount(node)
    for (order in 0 until len) {
        val child = tree.getChildId(node, order)
        if (tree.getFlexboxChildStyle(child).boxGenerationMode() == BoxGenerationMode.NONE) {
            tree.setUnroundedLayout(child, Layout.withOrder(order))
            tree.performChildLayout(
                child,
                Size.NONE.clone(),
                Size.NONE.clone(),
                Size.MAX_CONTENT,
                SizingMode.INHERENT_SIZE,
                Line.FALSE,
            )
        }
    }

    // 8.5. Flex Container Baselines: calculate the flex container's first baseline
    // See https://www.w3.org/TR/css-flexbox-1/#flex-baselines
    val firstVerticalBaseline = if (flexLines.isEmpty()) {
        Option.None
    } else {
        flexLines[0]
            .items
            .findRust { item -> constants.isColumn || item.alignSelf == AlignSelf.BASELINE }
            .orElse { flexLines[0].items.next() }
            .map { child ->
                val offsetVertical = if (constants.isRow) child.offsetCross else child.offsetMain
                offsetVertical + child.baseline
            }
    }

    return LayoutOutput.fromSizesAndBaselines(
        constants.containerSize,
        inflowContentSize.f32Max(absoluteContentSize),
        Point(x = Option.None, y = firstVerticalBaseline)
    )
}

/**
 * Compute constants that can be reused during the flexbox algorithm.
 */
fun computeConstants(
    style: FlexboxContainerStyle,
    knownDimensions: Size<Option<Float>>,
    parentSize: Size<Option<Float>>
): AlgoConstants {
    val dir = style.flexDirection()
    val isRow = dir.isRow()
    val isColumn = dir.isColumn()
    val isWrap = style.flexWrap() == FlexWrap.WRAP || style.flexWrap() == FlexWrap.WRAP_REVERSE
    val isWrapReverse = style.flexWrap() == FlexWrap.WRAP_REVERSE

    val aspectRatio = style.aspectRatio()
    val margin = style.margin().resolveOrZero(parentSize.width)
    val padding = style.padding().resolveOrZero(parentSize.width)
    val border = style.border().resolveOrZero(parentSize.width)
    val paddingBorderSum = padding.sumAxes() + border.sumAxes()
    val boxSizingAdjustment = if (style.boxSizing() == BoxSizing.CONTENT_BOX) {
        paddingBorderSum
    } else {
        Size.ZERO.clone()
    }

    val alignItems = style.alignItems().unwrapOr(AlignItems.STRETCH)
    val alignContent = style.alignContent().unwrapOr(AlignContent.STRETCH)
    val justifyContent = style.justifyContent()

    // Scrollbar gutters are reserved when the `overflow` property is set to `Overflow::Scroll`.
    // However, the axis are switched (transposed) because a node that scrolls vertically needs
    // *horizontal* space to be reserved for a scrollbar
    val scrollbarGutter = style.overflow().transpose().map { overflow ->
        when (overflow) {
            Overflow.SCROLL -> style.scrollbarWidth()
            else -> 0f
        }
    }
    // TODO: make side configurable based on the `direction` property
    val contentBoxInset = padding + border
    contentBoxInset.right += scrollbarGutter.x
    contentBoxInset.bottom += scrollbarGutter.y

    val nodeOuterSize = knownDimensions // TODO - ARBY | might have to copy this
    val nodeInnerSize = nodeOuterSize.maybeSub(contentBoxInset.sumAxes())
    val gap = style.gap().resolveOrZero(nodeInnerSize.or(Size.zeroOF()))

    val containerSize = Size.zeroF()
    val innerContainerSize = Size.zeroF()

    return AlgoConstants(
        dir = dir,
        isRow = isRow,
        isColumn = isColumn,
        isWrap = isWrap,
        isWrapReverse = isWrapReverse,
        minSize = style
            .minSize()
            .maybeResolve(parentSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment),
        maxSize = style
            .maxSize()
            .maybeResolve(parentSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment),
        margin = margin,
        border = border,
        gap = gap,
        contentBoxInset = contentBoxInset,
        scrollbarGutter = scrollbarGutter,
        alignItems = alignItems,
        alignContent = alignContent,
        justifyContent = justifyContent,
        nodeOuterSize = nodeOuterSize,
        nodeInnerSize = nodeInnerSize,
        containerSize = containerSize,
        innerContainerSize = innerContainerSize
    )
}

/**
 * Generate anonymous flex items.
 *
 * # [9.1. Initial Setup](https://www.w3.org/TR/css-flexbox-1/#box-manip)
 *
 * - [**Generate anonymous flex items**](https://www.w3.org/TR/css-flexbox-1/#algo-anon-box) as described in [§4 Flex Items](https://www.w3.org/TR/css-flexbox-1/#flex-items).
 */
fun generateAnonymousFlexItems(
    tree: LayoutFlexboxContainer,
    node: Int,
    constants: AlgoConstants
): List<FlexItem> {
    return tree.childIds(node)
        .asSequence()
        .withIndex() // Rust - enumerate()
        .map { (index, child) ->
            Triple(
                index,
                child,
                tree.getFlexboxChildStyle(child)
            )
        }
        .filter { (_, _, style) ->
            style.position() != Position.ABSOLUTE
        }
        .filter { (_, _, style) ->
            style.boxGenerationMode() != BoxGenerationMode.NONE
        }
        .map { (index, child, childStyle) ->
            val aspectRatio = childStyle.aspectRatio()
            val padding = childStyle.padding().resolveOrZero(constants.nodeInnerSize.width)
            val border = childStyle.border().resolveOrZero(constants.nodeInnerSize.width)
            val pbSum = (padding + border).sumAxes()
            val boxSizingAdjustment = if (childStyle.boxSizing() == BoxSizing.CONTENT_BOX) {
                pbSum
            } else {
                Size.ZERO.clone()
            }
            FlexItem(
                node = child,
                order = index,
                size = childStyle
                    .size()
                    .maybeResolve(constants.nodeInnerSize)
                    .maybeApplyAspectRatio(aspectRatio)
                    .maybeAdd(boxSizingAdjustment),
                minSize = childStyle
                    .minSize()
                    .maybeResolve(constants.nodeInnerSize)
                    .maybeApplyAspectRatio(aspectRatio)
                    .maybeAdd(boxSizingAdjustment),
                maxSize = childStyle
                    .maxSize()
                    .maybeResolve(constants.nodeInnerSize)
                    .maybeApplyAspectRatio(aspectRatio)
                    .maybeAdd(boxSizingAdjustment),

                inset = childStyle.inset().zipSize(constants.nodeInnerSize) { p, s ->
                    p.maybeResolve(s)
                },
                margin = childStyle.margin().resolveOrZero(constants.nodeInnerSize.width),
                marginIsAuto = childStyle.margin().map { m -> m == LengthPercentageAuto.Auto },
                padding = childStyle.padding().resolveOrZero(constants.nodeInnerSize.width),
                border = childStyle.border().resolveOrZero(constants.nodeInnerSize.width),
                alignSelf = childStyle.alignSelf().unwrapOr(constants.alignItems),
                overflow = childStyle.overflow(),
                scrollbarWidth = childStyle.scrollbarWidth(),
                flexGrow = childStyle.flexGrow(),
                flexShrink = childStyle.flexShrink(),
                flexBasis = 0f,
                innerFlexBasis = 0f,
                violation = 0f,
                frozen = false,

                resolvedMinimumMainSize = 0f,
                hypotheticalInnerSize = Size.zeroF(),
                hypotheticalOuterSize = Size.zeroF(),
                targetSize = Size.zeroF(),
                outerTargetSize = Size.zeroF(),
                contentFlexFraction = 0f,

                baseline = 0f,

                offsetMain = 0f,
                offsetCross = 0f
            )
        }
        .toList()
}

/**
 * Determine the available main and cross space for the flex items.
 *
 * # [9.2. Line Length Determination](https://www.w3.org/TR/css-flexbox-1/#line-sizing)
 *
 * - [**Determine the available main and cross space for the flex items**](https://www.w3.org/TR/css-flexbox-1/#algo-available).
 *
 * For each dimension, if that dimension of the flex container’s content box is a definite size, use that;
 * if that dimension of the flex container is being sized under a min or max-content constraint, the available space in that dimension is that constraint;
 * otherwise, subtract the flex container’s margin, border, and padding from the space available to the flex container in that dimension and use that value.
 * **This might result in an infinite value**.
 */
fun determineAvailableSpace(
    knownDimensions: Size<Option<Float>>,
    outerAvailableSpace: Size<AvailableSpace>,
    constants: AlgoConstants
): Size<AvailableSpace> {
    // Note: min/max/preferred size styles have already been applied to known_dimensions in the `compute` function above
    val width = when {
        knownDimensions.width.isSome() -> AvailableSpace.Definite(knownDimensions.width.unwrap() - constants.contentBoxInset.horizontalAxisSum())
        else -> outerAvailableSpace
            .width
            .maybeSub(constants.margin.horizontalAxisSum())
            .maybeSub(constants.contentBoxInset.horizontalAxisSum())
    }

    val height = when {
        knownDimensions.height.isSome() -> AvailableSpace.Definite(knownDimensions.height.unwrap() - constants.contentBoxInset.verticalAxisSum())
        else -> outerAvailableSpace
            .height
            .maybeSub(constants.margin.verticalAxisSum())
            .maybeSub(constants.contentBoxInset.verticalAxisSum())
    }

    return Size(width, height)
}

/**
 * Determine the flex base size and hypothetical main size of each item.
 *
 * # [9.2. Line Length Determination](https://www.w3.org/TR/css-flexbox-1/#line-sizing)
 *
 * - [**Determine the flex base size and hypothetical main size of each item:**](https://www.w3.org/TR/css-flexbox-1/#algo-main-item)
 *
 *     - A. If the item has a definite used flex basis, that’s the flex base size.
 *
 *     - B. If the flex item has ...
 *
 *         - an intrinsic aspect ratio,
 *         - a used flex basis of content, and
 *         - a definite cross size,
 *
 *     then the flex base size is calculated from its inner cross size and the flex item’s intrinsic aspect ratio.
 *
 *     - C. If the used flex basis is content or depends on its available space, and the flex container is being sized under a min-content
 *         or max-content constraint (e.g. when performing automatic table layout \[CSS21\]), size the item under that constraint.
 *         The flex base size is the item’s resulting main size.
 *
 *     - E. Otherwise, size the item into the available space using its used flex basis in place of its main size, treating a value of content as max-content.
 *         If a cross size is needed to determine the main size (e.g. when the flex item’s main size is in its block axis) and the flex item’s cross size is auto and not definite,
 *         in this calculation use fit-content as the flex item’s cross size. The flex base size is the item’s resulting main size.
 *
 *     When determining the flex base size, the item’s min and max main sizes are ignored (no clamping occurs).
 *     Furthermore, the sizing calculations that floor the content box size at zero when applying box-sizing are also ignored.
 *     (For example, an item with a specified size of zero, positive padding, and box-sizing: border-box will have an outer flex base size of zero—and hence a negative inner flex base size.)
 */
fun determineFlexBaseSize(
    tree: LayoutFlexboxContainer,
    constants: AlgoConstants,
    availableSpace: Size<AvailableSpace>,
    flexItems: List<FlexItem>
) {
    val dir = constants.dir

    for (child in flexItems) {
        val childStyle = tree.getFlexboxChildStyle(child.node)

        // Parent size for child sizing
        val crossAxisParentSize = constants.nodeInnerSize.cross(dir)
        val childParentSize = Size.fromCross(dir, crossAxisParentSize)

        // Available space for child sizing
        val crossAxisMarginSum = constants.margin.crossAxisSum(dir)
        val childMinCross = child.minSize.cross(dir).maybeAdd(crossAxisMarginSum)
        val childMaxCross = child.maxSize.cross(dir).maybeAdd(crossAxisMarginSum)
        val crossAxisAvailableSpace: AvailableSpace = availableSpace
            .cross(dir)
            .mapDefiniteValue { value -> crossAxisParentSize.unwrapOr(value) }
            .maybeClamp(childMinCross, childMaxCross)

        // Known dimensions for child sizing
        val childKnownDimensions = run {
            val ckd = child.size.withMain(dir, Option.None)
            if (child.alignSelf == AlignSelf.STRETCH && ckd.cross(dir).isNone()) {
                ckd.setCross(
                    dir,
                    crossAxisAvailableSpace.intoOption().maybeSub(child.margin.crossAxisSum(dir))
                )
            }
            ckd
        }

        val containerWidth = constants.nodeInnerSize.main(dir)
        val boxSizingAdjustment = if (childStyle.boxSizing() == BoxSizing.CONTENT_BOX) {
            val padding = childStyle.padding().resolveOrZero(containerWidth)
            val border = childStyle.border().resolveOrZero(containerWidth)
            (padding + border).sumAxes()
        } else {
            Size.ZERO.clone()
        }.main(dir)
        val flexBasis = childStyle.flexBasis().maybeResolve(containerWidth).maybeAdd(boxSizingAdjustment)

        child.flexBasis = run {
            // A. If the item has a definite used flex basis, that’s the flex base size.

            // B. If the flex item has an intrinsic aspect ratio,
            //    a used flex basis of content, and a definite cross size,
            //    then the flex base size is calculated from its inner
            //    cross size and the flex item’s intrinsic aspect ratio.

            // Note: `child.size` has already been resolved against aspect_ratio in generate_anonymous_flex_items
            // So B will just work here by using main_size without special handling for aspect_ratio
            val mainSize = child.size.main(dir)
            if (flexBasis.isSome() || mainSize.isSome()) {
                return@run flexBasis.or(mainSize).unwrap()
            }

            // C. If the used flex basis is content or depends on its available space,
            //    and the flex container is being sized under a min-content or max-content
            //    constraint (e.g. when performing automatic table layout [CSS21]),
            //    size the item under that constraint. The flex base size is the item’s
            //    resulting main size.

            // This is covered by the implementation of E below, which passes the available_space constraint
            // through to the child size computation. It may need a separate implementation if/when D is implemented.

            // D. Otherwise, if the used flex basis is content or depends on its
            //    available space, the available main size is infinite, and the flex item’s
            //    inline axis is parallel to the main axis, lay the item out using the rules
            //    for a box in an orthogonal flow [CSS3-WRITING-MODES]. The flex base size
            //    is the item’s max-content main size.

            // TODO if/when vertical writing modes are supported

            // E. Otherwise, size the item into the available space using its used flex basis
            //    in place of its main size, treating a value of content as max-content.
            //    If a cross size is needed to determine the main size (e.g. when the
            //    flex item’s main size is in its block axis) and the flex item’s cross size
            //    is auto and not definite, in this calculation use fit-content as the
            //    flex item’s cross size. The flex base size is the item’s resulting main size.

            val childAvailableSpace = Size.MAX_CONTENT
                .withMain(
                    dir,
                    // Map AvailableSpace::Definite to AvailableSpace::MaxContent
                    if (availableSpace.main(dir) == AvailableSpace.MinContent) {
                        AvailableSpace.MinContent
                    } else {
                        AvailableSpace.MaxContent
                    },
                )
                .withCross(dir, crossAxisAvailableSpace)

            return@run tree.measureChildSize(
                child.node,
                childKnownDimensions,
                childParentSize,
                childAvailableSpace,
                SizingMode.CONTENT_SIZE,
                dir.mainAxis(),
                Line.FALSE
            )
        }

        // Floor flex-basis by the padding_border_sum (floors inner_flex_basis at zero)
        // This seems to be in violation of the spec which explicitly states that the content box should not be floored at zero
        // (like it usually is) when calculating the flex-basis. But including this matches both Chrome and Firefox's behaviour.
        //
        // TODO: resolve spec violation
        // Spec: https://www.w3.org/TR/css-flexbox-1/#intrinsic-item-contributions
        // Spec: https://www.w3.org/TR/css-flexbox-1/#change-2016-max-contribution
        val paddingBorderSum = child.padding.mainAxisSum(constants.dir) + child.border.mainAxisSum(constants.dir)
        child.flexBasis = child.flexBasis.max(paddingBorderSum)

        // The hypothetical main size is the item’s flex base size clamped according to its
        // used min and max main sizes (and flooring the content box size at zero).

        child.innerFlexBasis =
            child.flexBasis - child.padding.mainAxisSum(constants.dir) - child.border.mainAxisSum(constants.dir)

        val paddingBorderAxesSums = (child.padding + child.border).sumAxes().map { v -> Option.Some(v) }

        // Note that it is important that the `parent_size` parameter in the main axis is not set for this
        // function call as it used for resolving percentages, and percentage size in an axis should not contribute
        // to a min-content contribution in that same axis. However the `parent_size` and `available_space` *should*
        // be set to their usual values in the cross axis so that wrapping content can wrap correctly.
        //
        // See https://drafts.csswg.org/css-sizing-3/#min-percentage-contribution
        val styleMinMainSize =
            child.minSize.or(child.overflow.map(Overflow::maybeIntoAutomaticMinSize).into()).main(dir)

        child.resolvedMinimumMainSize = styleMinMainSize.unwrapOr(run {
            val minContentMainSize = run {
                val childAvailableSpace = Size.MIN_CONTENT.withCross(dir, crossAxisAvailableSpace)

                tree.measureChildSize(
                    child.node,
                    childKnownDimensions,
                    childParentSize,
                    childAvailableSpace,
                    SizingMode.CONTENT_SIZE,
                    dir.mainAxis(),
                    Line.FALSE
                )
            }

            // 4.5. Automatic Minimum Size of Flex Items
            // https://www.w3.org/TR/css-flexbox-1/#min-size-auto
            val clampedMinContentSize =
                minContentMainSize.maybeMin(child.size.main(dir)).maybeMin(child.maxSize.main(dir))
            clampedMinContentSize.maybeMax(paddingBorderAxesSums.main(dir))
        })

        val hypotheticalInnerMinMain =
            child.resolvedMinimumMainSize.maybeMax(paddingBorderAxesSums.main(constants.dir))
        val hypotheticalInnerSize =
            child.flexBasis.maybeClamp(Option.Some(hypotheticalInnerMinMain), child.maxSize.main(constants.dir))
        val hypotheticalOuterSize = hypotheticalInnerSize + child.margin.mainAxisSum(constants.dir)

        child.hypotheticalInnerSize.setMain(constants.dir, hypotheticalInnerSize)
        child.hypotheticalOuterSize.setMain(constants.dir, hypotheticalOuterSize)
    }
}

/**
 * Collect flex items into flex lines.
 *
 * # [9.3. Main Size Determination](https://www.w3.org/TR/css-flexbox-1/#main-sizing)
 *
 * - [**Collect flex items into flex lines**](https://www.w3.org/TR/css-flexbox-1/#algo-line-break):
 *
 *     - If the flex container is single-line, collect all the flex items into a single flex line.
 *
 *     - Otherwise, starting from the first uncollected item, collect consecutive items one by one until the first time that the next collected item would not fit into the flex container’s inner main size
 *         (or until a forced break is encountered, see [§10 Fragmenting Flex Layout](https://www.w3.org/TR/css-flexbox-1/#pagination)).
 *         If the very first uncollected item wouldn't fit, collect just it into the line.
 *
 *         For this step, the size of a flex item is its outer hypothetical main size. (**Note: This can be negative**.)
 *
 *         Repeat until all flex items have been collected into flex lines.
 *
 *         **Note that the "collect as many" line will collect zero-sized flex items onto the end of the previous line even if the last non-zero item exactly "filled up" the line**.
 */
fun collectFlexLines(
    constants: AlgoConstants,
    availableSpace: Size<AvailableSpace>,
    flexItems: List<FlexItem>
): List<FlexLine> {
    return if (!constants.isWrap) {
        listOf(
            FlexLine(
                items = flexItems,
                crossSize = 0f,
                offsetCross = 0f
            )
        )
    } else {
        val mainAxisAvailableSpace = when (val value = constants.maxSize.main(constants.dir)) {
            is Option.Some -> AvailableSpace.Definite(
                availableSpace
                    .main(constants.dir)
                    .intoOption()
                    .unwrapOr(value.unwrap())
                    .maybeMax(constants.minSize.main(constants.dir))
            )

            is Option.None -> availableSpace.main(constants.dir)
        }

        when (mainAxisAvailableSpace) {
            // If we're sizing under a max-content constraint then the flex items will never wrap
            // (at least for now - future extensions to the CSS spec may add provisions for forced wrap points)
            is AvailableSpace.MaxContent -> {
                listOf(
                    FlexLine(
                        items = flexItems,
                        crossSize = 0f,
                        offsetCross = 0f
                    )
                )
            }
            // If flex-wrap is Wrap and we're sizing under a min-content constraint, then we take every possible wrapping opportunity
            // and place each item in it's own line
            is AvailableSpace.MinContent -> {
                val lines = ArrayList<FlexLine>()
                var items = flexItems

                while (items.isNotEmpty()) {
                    val (lineItems, rest) = items.splitAt(1)
                    lines.add(FlexLine(items = lineItems, crossSize = 0.0f, offsetCross = 0.0f))
                    items = rest
                }
                lines
            }

            is AvailableSpace.Definite -> {
                val lines = ArrayList<FlexLine>()
                var flexItems = flexItems
                val mainAxisGap = constants.gap.main(constants.dir)

                while (!flexItems.isEmpty()) {
                    // Find index of the first item in the next line
                    // (or the last item if all remaining items are in the current line)
                    var lineLength = 0f
                    val index = flexItems
                        .enumerate()
                        .findRust { (idx, child) ->
                            // Gaps only occur between items (not before the first one or after the last one)
                            // So first item in the line does not contribute a gap to the line length
                            val gapContribution = if (idx == 0) {
                                0f
                            } else {
                                mainAxisGap
                            }
                            lineLength += child.hypotheticalOuterSize.main(constants.dir) + gapContribution
                            lineLength > mainAxisAvailableSpace.availableSpace && idx != 0
                        }
                        .map { (idx, _) -> idx }
                        .unwrapOr(flexItems.len())

                    val (items, rest) = flexItems.splitAt(index)
                    lines.add(FlexLine(items, crossSize = 0f, offsetCross = 0f))
                    flexItems = rest
                }
                lines
            }
        }
    }
}

/**
 * Determine the container's main size (if not already known)
 */
fun determineContainerMainSize(
    tree: LayoutFlexboxContainer,
    availableSpace: Size<AvailableSpace>,
    lines: List<FlexLine>,
    constants: AlgoConstants
) {
    val dir = constants.dir
    val mainContentBoxInset = constants.contentBoxInset.mainAxisSum(constants.dir)

    var outerMainSize = constants.nodeOuterSize.main(constants.dir).unwrapOrElse {
        val v = availableSpace.main(dir)
        when {
            v is AvailableSpace.Definite -> {
                val longestLineLength = lines
                    .map { line ->
                        val lineMainAxisGap = sumAxisGaps(constants.gap.main(constants.dir), line.items.len())
                        val totalTargetSize = line
                            .items
                            .map { child ->
                                val paddingBorderSum = (child.padding + child.border).mainAxisSum(constants.dir)
                                (child.flexBasis + child.margin.mainAxisSum(constants.dir)).max(paddingBorderSum)
                            }
                            .sum()
                        totalTargetSize + lineMainAxisGap
                    }
                    .maxByRust { a, b -> a.compareTo(b) }
                    .unwrapOr(0f)
                val size = longestLineLength + mainContentBoxInset
                if (lines.len() > 1) {
                    f32Max(size, v.availableSpace)
                } else {
                    size
                }
            }

            v is AvailableSpace.MinContent && constants.isWrap -> {
                val longestLineLength = lines
                    .map { line ->
                        val lineMainAxisGap = sumAxisGaps(constants.gap.main(constants.dir), line.items.len())
                        val totalTargetSize = line
                            .items
                            .map { child ->
                                val paddingBorderSum = (child.padding + child.border).mainAxisSum(constants.dir)
                                (child.flexBasis + child.margin.mainAxisSum(constants.dir)).max(paddingBorderSum)
                            }
                            .sum()
                        totalTargetSize + lineMainAxisGap
                    }
                    .maxByRust { a, b -> a.compareTo(b) }
                    .unwrapOr(0f)
                longestLineLength + mainContentBoxInset
            }

            else -> {
                // Define a base main_size variable. This is mutated once for iteration over the outer
                // loop over the flex lines as:
                //   "The flex container’s max-content size is the largest sum of the afore-calculated sizes of all items within a single line."
                var mainSize = 0f
                for (line in lines) {
                    for (item in line.items) {
                        val styleMin = item.minSize.main(constants.dir)
                        val stylePreferred = item.size.main(constants.dir)
                        val styleMax = item.maxSize.main(constants.dir)

                        // The spec seems a bit unclear on this point (my initial reading was that the `.maybe_max(style_preferred)` should
                        // not be included here), however this matches both Chrome and Firefox as of 9th March 2023.
                        //
                        // Spec: https://www.w3.org/TR/css-flexbox-1/#intrinsic-item-contributions
                        // Spec modification: https://www.w3.org/TR/css-flexbox-1/#change-2016-max-contribution
                        // Issue: https://github.com/w3c/csswg-drafts/issues/1435
                        // Gentest: padding_border_overrides_size_flex_basis_0.html
                        val clampingBasis = Option.Some(item.flexBasis).maybeMax(stylePreferred)
                        val flexBasisMin = clampingBasis.filter { item.flexShrink == 0f }
                        val flexBasisMax = clampingBasis.filter { item.flexGrow == 0f }

                        val minMainSize = styleMin
                            .maybeMax(flexBasisMin)
                            .or(flexBasisMin)
                            .unwrapOr(item.resolvedMinimumMainSize)
                            .max(item.resolvedMinimumMainSize)
                        val maxMainSize =
                            styleMax.maybeMin(flexBasisMax).or(flexBasisMax).unwrapOr(Float.POSITIVE_INFINITY)

                        val (min, pref, max) = Triple(minMainSize, stylePreferred, maxMainSize)
                        val contentContribution = run {
                            // If the clamping values are such that max <= min, then we can avoid the expensive step of computing the content size
                            // as we know that the clamping values will override it anyway

                            if (pref.isSome() && (max <= min || max <= pref.unwrap())) {
                                pref.unwrap().min(max).max(min) + item.margin.mainAxisSum(constants.dir)
                            } else if (max <= min) {
                                min + item.margin.mainAxisSum(constants.dir)
                            } else {
                                // Else compute the min- or -max content size and apply the full formula for computing the
                                // min- or max- content contributuon

                                // Parent size for child sizing
                                val crossAxisParentSize = constants.nodeInnerSize.cross(dir)

                                // Available space for child sizing
                                val crossAxisMarginSum = constants.margin.crossAxisSum(dir)
                                val childMinCross = item.minSize.cross(dir).maybeAdd(crossAxisMarginSum)
                                val childMaxCross = item.maxSize.cross(dir).maybeAdd(crossAxisMarginSum)
                                val crossAxisAvailableSpace: AvailableSpace = availableSpace
                                    .cross(dir)
                                    .mapDefiniteValue { value -> crossAxisParentSize.unwrapOr(value) }
                                    .maybeClamp(childMinCross, childMaxCross)

                                val childAvailableSpace = availableSpace.withCross(dir, crossAxisAvailableSpace)

                                // Known dimensions for child sizing
                                val childKnownDimensions = run {
                                    val ckd = item.size.withMain(dir, Option.None)
                                    if (item.alignSelf == AlignSelf.STRETCH && ckd.cross(dir).isNone()) {
                                        ckd.setCross(
                                            dir,
                                            crossAxisAvailableSpace
                                                .intoOption()
                                                .maybeSub(item.margin.crossAxisSum(dir))
                                        )
                                    }
                                    ckd
                                }

                                // Either the min- or max- content size depending on which constraint we are sizing under.
                                // TODO: Optimise by using already computed values where available
                                val contentMainSize = tree.measureChildSize(
                                    item.node,
                                    childKnownDimensions,
                                    constants.nodeInnerSize,
                                    childAvailableSpace,
                                    SizingMode.INHERENT_SIZE,
                                    dir.mainAxis(),
                                    Line.FALSE,
                                ) + item.margin.mainAxisSum(constants.dir)

                                // This is somewhat bizarre in that it's asymmetrical depending whether the flex container is a column or a row.
                                //
                                // I *think* this might relate to https://drafts.csswg.org/css-flexbox-1/#algo-main-container:
                                //
                                //    "The automatic block size of a block-level flex container is its max-content size."
                                //
                                // Which could suggest that flex-basis defining a vertical size does not shrink because it is in the block axis, and the automatic size
                                // in the block axis is a MAX content size. Whereas a flex-basis defining a horizontal size does shrink because the automatic size in
                                // inline axis is MIN content size (although I don't have a reference for that).
                                //
                                // Ultimately, this was not found by reading the spec, but by trial and error fixing tests to align with Webkit/Firefox output.
                                // (see the `flex_basis_unconstraint_row` and `flex_basis_uncontraint_column` generated tests which demonstrate this)
                                if (constants.isRow) {
                                    contentMainSize.maybeClamp(styleMin, styleMax).max(mainContentBoxInset)
                                } else {
                                    contentMainSize
                                        .max(item.flexBasis)
                                        .maybeClamp(styleMin, styleMax)
                                        .max(mainContentBoxInset)
                                }
                            }
                        }

                        item.contentFlexFraction = run {
                            val diff = contentContribution - item.flexBasis
                            if (diff > 0f) {
                                diff / f32Max(1f, item.flexGrow)
                            } else if (diff < 0f) {
                                val scaledShrinkFactor = f32Max(1f, item.flexShrink * item.innerFlexBasis)
                                diff / scaledShrinkFactor
                            } else {
                                // We are assuming that diff is 0.0 here and that we haven't accidentally introduced a NaN
                                0f
                            }
                        }
                    }

                    // TODO Spec says to scale everything by the line's max flex fraction. But neither Chrome nor firefox implement this
                    // so we don't either. But if we did want to, we'd need this computation here (and to use it below):
                    //
                    // Within each line, find the largest max-content flex fraction among all the flex items.
                    // var line_flex_fraction = line
                    //     .items
                    //     .iter()
                    //     .map(|item| item.content_flex_fraction)
                    //     .max_by(|a, b| a.total_cmp(b))
                    //     .unwrap_or(0.0); // Unwrap case never gets hit because there is always at least one item a line

                    // Add each item’s flex base size to the product of:
                    //   - its flex grow factor (or scaled flex shrink factor,if the chosen max-content flex fraction was negative)
                    //   - the chosen max-content flex fraction
                    // then clamp that result by the max main size floored by the min main size.
                    //
                    // The flex container’s max-content size is the largest sum of the afore-calculated sizes of all items within a single line.
                    val itemMainSizeSum = line
                        .items
                        .map { item ->
                            val flexFraction = item.contentFlexFraction
                            // var flex_fraction = line_flex_fraction

                            val flexContribution = if (item.contentFlexFraction > 0f) {
                                f32Max(1f, item.flexGrow) * flexFraction
                            } else if (item.contentFlexFraction < 0f) {
                                val scaledShrinkFactor = f32Max(1f, item.flexShrink) * item.innerFlexBasis
                                scaledShrinkFactor * flexFraction
                            } else {
                                0f
                            }
                            val size = item.flexBasis + flexContribution
                            item.outerTargetSize.setMain(constants.dir, size)
                            item.targetSize.setMain(constants.dir, size)
                            size
                        }
                        .sum()

                    val gapSum = sumAxisGaps(constants.gap.main(constants.dir), line.items.len())
                    mainSize = f32Max(mainSize, itemMainSizeSum + gapSum)
                }

                mainSize + mainContentBoxInset
            }
        }
    }

    outerMainSize = outerMainSize
        .maybeClamp(constants.minSize.main(constants.dir), constants.maxSize.main(constants.dir))
        .max(mainContentBoxInset - constants.scrollbarGutter.main(constants.dir))

    // var outer_main_size = inner_main_size + constants.padding_border.main_axis_sum(constants.dir)
    val innerMainSize = f32Max(outerMainSize - mainContentBoxInset, 0f)
    constants.containerSize.setMain(constants.dir, outerMainSize)
    constants.innerContainerSize.setMain(constants.dir, innerMainSize)
    constants.nodeInnerSize.setMain(constants.dir, Option.Some(innerMainSize))
}

/**
 * Resolve the flexible lengths of the items within a flex line.
 * Sets the `main` component of each item's `target_size` and `outer_target_size`
 *
 * # [9.7. Resolving Flexible Lengths](https://www.w3.org/TR/css-flexbox-1/#resolve-flexible-lengths)
 */
fun resolveFlexibleLengths(
    line: FlexLine,
    constants: AlgoConstants
) {
    val totalMainAxisGap = sumAxisGaps(constants.gap.main(constants.dir), line.items.len())

    // 1. Determine the used flex factor. Sum the outer hypothetical main sizes of all
    //    items on the line. If the sum is less than the flex container’s inner main size,
    //    use the flex grow factor for the rest of this algorithm; otherwise, use the
    //    flex shrink factor.

    val totalHypotheticalOuterMainSize =
        line.items.map { child -> child.hypotheticalOuterSize.main(constants.dir) }.sum()
    val usedFlexFactor: Float = totalMainAxisGap + totalHypotheticalOuterMainSize
    val growing = usedFlexFactor < constants.nodeInnerSize.main(constants.dir).unwrapOr(0f)
    val shrinking = usedFlexFactor > constants.nodeInnerSize.main(constants.dir).unwrapOr(0f)
    val exactlySized = !growing && !shrinking

    // 2. Size inflexible items. Freeze, setting its target main size to its hypothetical main size
    //    - Any item that has a flex factor of zero
    //    - If using the flex grow factor: any item that has a flex base size
    //      greater than its hypothetical main size
    //    - If using the flex shrink factor: any item that has a flex base size
    //      smaller than its hypothetical main size

    for (child in line.items) {
        val innerTargetSize = child.hypotheticalInnerSize.main(constants.dir)
        child.targetSize.setMain(constants.dir, innerTargetSize)

        if (exactlySized
                || (child.flexGrow == 0f && child.flexShrink == 0f)
                || (growing && child.flexBasis > child.hypotheticalInnerSize.main(constants.dir))
                || (shrinking && child.flexBasis < child.hypotheticalInnerSize.main(constants.dir)))
        {
            child.frozen = true
            val outerTargetSize = innerTargetSize + child.margin.mainAxisSum(constants.dir)
            child.outerTargetSize.setMain(constants.dir, outerTargetSize)
        }
    }

    if (exactlySized) {
        return
    }

    // 3. Calculate initial free space. Sum the outer sizes of all items on the line,
    //    and subtract this from the flex container’s inner main size. For frozen items,
    //    use their outer target main size; for other items, use their outer flex base size.

    val usedSpace: Float = totalMainAxisGap + line
                .items
                .map { child ->
                    if (child.frozen) {
                        child.outerTargetSize.main(constants.dir)
                    } else {
                        child.flexBasis + child.margin.mainAxisSum(constants.dir)
                    }
                }
                .sum()

    val initialFreeSpace = constants.nodeInnerSize.main(constants.dir).maybeSub(usedSpace).unwrapOr(0f)

    // 4. Loop
    while (true) {
        // a. Check for flexible items. If all the flex items on the line are frozen,
        //    free space has been distributed; exit this loop.

        if (line.items.all { child -> child.frozen }) {
            break
        }

        // b. Calculate the remaining free space as for initial free space, above.
        //    If the sum of the unfrozen flex items’ flex factors is less than one,
        //    multiply the initial free space by this sum. If the magnitude of this
        //    value is less than the magnitude of the remaining free space, use this
        //    as the remaining free space.

        val usedSpace: Float = totalMainAxisGap + line
                    .items
                    .map { child ->
                        if (child.frozen) {
                            child.outerTargetSize.main(constants.dir)
                        } else {
                            child.flexBasis + child.margin.mainAxisSum(constants.dir)
                        }
                    }
                    .sum()

        val unfrozen: List<FlexItem> = line.items.filter { child -> !child.frozen}

        val (sumFlexGrow, sumFlexShrink) = unfrozen.fold(T2(0f, 0f)) { (flexGrow, flexShrink), item ->
            T2(flexGrow + item.flexGrow, flexShrink + item.flexShrink)
        }

        val free_space = if (growing && sumFlexGrow < 1f) {
            (initialFreeSpace * sumFlexGrow - totalMainAxisGap)
            .maybeMin(constants.nodeInnerSize.main(constants.dir)
            .maybeSub(usedSpace))
        } else if (shrinking && sumFlexShrink < 1f) {
            (initialFreeSpace * sumFlexShrink - totalMainAxisGap)
            .maybeMax(constants.nodeInnerSize.main(constants.dir)
            .maybeSub(usedSpace))
    } else {
        (constants.nodeInnerSize.main(constants.dir).maybeSub(usedSpace))
            .unwrapOr(usedFlexFactor - usedSpace)
    }

        // c. Distribute free space proportional to the flex factors.
        //    - If the remaining free space is zero
        //        Do Nothing
        //    - If using the flex grow factor
        //        Find the ratio of the item’s flex grow factor to the sum of the
        //        flex grow factors of all unfrozen items on the line. Set the item’s
        //        target main size to its flex base size plus a fraction of the remaining
        //        free space proportional to the ratio.
        //    - If using the flex shrink factor
        //        For every unfrozen item on the line, multiply its flex shrink factor by
        //        its inner flex base size, and note this as its scaled flex shrink factor.
        //        Find the ratio of the item’s scaled flex shrink factor to the sum of the
        //        scaled flex shrink factors of all unfrozen items on the line. Set the item’s
        //        target main size to its flex base size minus a fraction of the absolute value
        //        of the remaining free space proportional to the ratio. Note this may result
        //        in a negative inner main size; it will be corrected in the next step.
        //    - Otherwise
        //        Do Nothing

        if (free_space.isNormal()) {
            if (growing && sumFlexGrow > 0f) {
                for (child in unfrozen) {
                    child
                        .targetSize
                        .setMain(constants.dir, child.flexBasis + free_space * (child.flexGrow / sumFlexGrow))
                }
            } else if (shrinking && sumFlexShrink > 0f) {
                val sumScaledShrinkFactor: Float =
                unfrozen.map { child -> child.innerFlexBasis * child.flexShrink }.sum()

                if (sumScaledShrinkFactor > 0.0f) {
                    for (child in unfrozen) {
                        val scaledShrinkFactor = child.innerFlexBasis * child.flexShrink
                        child.targetSize.setMain(
                            constants.dir,
                            child.flexBasis + free_space * (scaledShrinkFactor / sumScaledShrinkFactor)
                        )
                    }
                }
            }
        }

        // d. Fix min/max violations. Clamp each non-frozen item’s target main size by its
        //    used min and max main sizes and floor its content-box size at zero. If the
        //    item’s target main size was made smaller by this, it’s a max violation.
        //    If the item’s target main size was made larger by this, it’s a min violation.

        val totalViolation = unfrozen.fold(0f) { acc, child ->
            val resolvedMinMain: Option<Float> = child.resolvedMinimumMainSize.into()
            val maxMain = child.maxSize.main(constants.dir)
            val clamped = child.targetSize.main(constants.dir).maybeClamp(resolvedMinMain, maxMain).max(0f)
            child.violation = clamped - child.targetSize.main(constants.dir)
            child.targetSize.setMain(constants.dir, clamped)
            child.outerTargetSize.setMain(
                constants.dir,
                child.targetSize.main(constants.dir) + child.margin.mainAxisSum(constants.dir)
            )

            acc + child.violation
        }

        // e. Freeze over-flexed items. The total violation is the sum of the adjustments
        //    from the previous step ∑(clamped size - unclamped size). If the total violation is:
        //    - Zero
        //        Freeze all items.
        //    - Positive
        //        Freeze all the items with min violations.
        //    - Negative
        //        Freeze all the items with max violations.

        for (child in unfrozen) {
            if (totalViolation > 0f) {
                child.frozen = child.violation > 0f
            } else if (totalViolation < 0f) {
                child.frozen = child.violation < 0f
            } else {
                child.frozen = true
            }
        }

        // f. Return to the start of this loop.
    }
}

/**
 * Determine the hypothetical cross size of each item.
 *
 * # [9.4. Cross Size Determination](https://www.w3.org/TR/css-flexbox-1/#cross-sizing)
 *
 * - [**Determine the hypothetical cross size of each item**](https://www.w3.org/TR/css-flexbox-1/#algo-cross-item)
 *     by performing layout with the used main size and the available space, treating auto as fit-content.
 */
fun determineHypotheticalCrossSize(
    tree: LayoutFlexboxContainer,
    line: FlexLine,
    constants: AlgoConstants,
    availableSpace: Size<AvailableSpace>
) {
    for (child in line.items) {
        val paddingBorderSum = (child.padding + child.border).crossAxisSum(constants.dir)

        val childKnownMain = constants.containerSize.main(constants.dir).intoAS()

        val childCross = child
            .size
            .cross(constants.dir)
            .maybeClamp(child.minSize.cross(constants.dir), child.maxSize.cross(constants.dir))
            .maybeMax(paddingBorderSum)

        val childAvailableCross = availableSpace
            .cross(constants.dir)
            .maybeClamp(child.minSize.cross(constants.dir), child.maxSize.cross(constants.dir))
            .maybeMax(paddingBorderSum)

        val childInnerCross = childCross.unwrapOrElse {
            tree.measureChildSize(
                child.node,
                Size(
                    width = if (constants.isRow) child.targetSize.width.into() else childCross,
                    height = if (constants.isRow) childCross else child.targetSize.height.into()
                ),
                constants.nodeInnerSize,
                Size(
                    width = if (constants.isRow) childKnownMain else childAvailableCross,
                    height = if (constants.isRow) childAvailableCross else childKnownMain
                ),
                SizingMode.CONTENT_SIZE,
                constants.dir.crossAxis(),
                Line.FALSE,
            )
                .maybeClamp(child.minSize.cross(constants.dir), child.maxSize.cross(constants.dir))
                .max(paddingBorderSum)
        }
        val childOuterCross = childInnerCross + child.margin.crossAxisSum(constants.dir)

        child.hypotheticalInnerSize.setCross(constants.dir, childInnerCross)
        child.hypotheticalOuterSize.setCross(constants.dir, childOuterCross)
    }
}

/**
 * Calculate the base lines of the children.
 */
fun calculateChildrenBaseLines(
    tree: LayoutFlexboxContainer,
    nodeSize: Size<Option<Float>>,
    availableSpace: Size<AvailableSpace>,
    flexLines: List<FlexLine>,
    constants: AlgoConstants
) {
    // Only compute baselines for flex rows because we only support baseline alignment in the cross axis
    // where that axis is also the inline axis
    // TODO: this may need revisiting if/when we support vertical writing modes
    if (!constants.isRow) {
        return
    }

    for (line in flexLines) {
        // If a flex line has one or zero items participating in baseline alignment then baseline alignment is a no-op so we skip
        val lineBaselineChildCount =
            line.items.count { child -> child.alignSelf == AlignSelf.BASELINE }
        if (lineBaselineChildCount <= 1) {
            continue
        }

        for (child in line.items) {
            // Only calculate baselines for children participating in baseline alignment
            if (child.alignSelf != AlignSelf.BASELINE) {
                continue
            }

            val measuredSizeAndBaselines = tree.performChildLayout(
                child.node,
                Size(
                    width = if (constants.isRow) {
                        child.targetSize.width.into()
                    } else {
                        child.hypotheticalInnerSize.width.into()
                    },
                    height = if (constants.isRow) {
                        child.hypotheticalInnerSize.height.into()
                    } else {
                        child.targetSize.height.into()
                    }
                ),
                constants.nodeInnerSize,
                Size(
                    width = if (constants.isRow) constants.containerSize.width.intoAS() else availableSpace.width.maybeSet(
                        nodeSize.width
                    ),
                    height = if (constants.isRow) availableSpace.height.maybeSet(nodeSize.height) else constants.containerSize.height.intoAS()
                ),
                SizingMode.CONTENT_SIZE,
                Line.FALSE
            )

            val baseline = measuredSizeAndBaselines.firstBaselines.y
            val height = measuredSizeAndBaselines.size.height

            child.baseline = baseline.unwrapOr(height) + child.margin.top
        }
    }
}

/**
 * Calculate the cross size of each flex line.
 *
 * # [9.4. Cross Size Determination](https://www.w3.org/TR/css-flexbox-1/#cross-sizing)
 *
 * - [**Calculate the cross size of each flex line**](https://www.w3.org/TR/css-flexbox-1/#algo-cross-line).
 */
fun calculateCrossSize(
    flexLines: List<FlexLine>,
    nodeSize: Size<Option<Float>>,
    constants: AlgoConstants
) {
    // If the flex container is single-line and has a definite cross size,
    // the cross size of the flex line is the flex container’s inner cross size.
    if (!constants.isWrap && nodeSize.cross(constants.dir).isSome()) {
        val crossAxisPaddingBorder = constants.contentBoxInset.crossAxisSum(constants.dir)
        val crossMinSize = constants.minSize.cross(constants.dir)
        val crossMaxSize = constants.maxSize.cross(constants.dir)
        flexLines[0].crossSize = nodeSize
            .cross(constants.dir)
            .maybeClamp(crossMinSize, crossMaxSize)
            .maybeSub(crossAxisPaddingBorder)
            .maybeMax(0f)
            .unwrapOr(0f)
    } else {
        // Otherwise, for each flex line:
        //
        //    1. Collect all the flex items whose inline-axis is parallel to the main-axis, whose
        //       align-self is baseline, and whose cross-axis margins are both non-auto. Find the
        //       largest of the distances between each item’s baseline and its hypothetical outer
        //       cross-start edge, and the largest of the distances between each item’s baseline
        //       and its hypothetical outer cross-end edge, and sum these two values.

        //    2. Among all the items not collected by the previous step, find the largest
        //       outer hypothetical cross size.

        //    3. The used cross-size of the flex line is the largest of the numbers found in the
        //       previous two steps and zero.
        for (line in flexLines) {
            val maxBaseline: Float = line.items.map { child -> child.baseline }.fold(0f) { acc, x -> acc.max(x) }
            line.crossSize = line
                .items
                .map { child ->
                    if (
                        child.alignSelf == AlignSelf.BASELINE
                        && !child.marginIsAuto.crossStart(constants.dir)
                        && !child.marginIsAuto.crossEnd(constants.dir)
                    ) {
                        maxBaseline - child.baseline + child.hypotheticalOuterSize.cross(constants.dir)
                    } else {
                        child.hypotheticalOuterSize.cross(constants.dir)
                    }
                }
                .fold(0f) { acc, x -> acc.max(x) }
        }

        // If the flex container is single-line, then clamp the line’s cross-size to be within the container’s computed min and max cross sizes.
        // Note that if CSS 2.1’s definition of min/max-width/height applied more generally, this behavior would fall out automatically.
        if (!constants.isWrap) {
            val crossAxisPaddingBorder = constants.contentBoxInset.crossAxisSum(constants.dir)
            val crossMinSize = constants.minSize.cross(constants.dir)
            val crossMaxSize = constants.maxSize.cross(constants.dir)
            flexLines[0].crossSize = flexLines[0].crossSize.maybeClamp(
                crossMinSize.maybeSub(crossAxisPaddingBorder),
                crossMaxSize.maybeSub(crossAxisPaddingBorder)
            )
        }
    }
}

/**
 * Handle 'align-content: stretch'.
 *
 * # [9.4. Cross Size Determination](https://www.w3.org/TR/css-flexbox-1/#cross-sizing)
 *
 * - [**Handle 'align-content: stretch'**](https://www.w3.org/TR/css-flexbox-1/#algo-line-stretch). If the flex container has a definite cross size, align-content is stretch,
 *     and the sum of the flex lines' cross sizes is less than the flex container’s inner cross size,
 *     increase the cross size of each flex line by equal amounts such that the sum of their cross sizes exactly equals the flex container’s inner cross size.
 */
fun handleAlignContentStretch(
    flexLines: List<FlexLine>,
    nodeSize: Size<Option<Float>>,
    constants: AlgoConstants
) {
    if (constants.alignContent == AlignContent.STRETCH) {
        val crossAxisPaddingBorder = constants.contentBoxInset.crossAxisSum(constants.dir)
        val crossMinSize = constants.minSize.cross(constants.dir)
        val crossMaxSize = constants.maxSize.cross(constants.dir)
        val containerMinInnerCross = nodeSize
            .cross(constants.dir)
            .or(crossMinSize)
            .maybeClamp(crossMinSize, crossMaxSize)
            .maybeSub(crossAxisPaddingBorder)
            .maybeMax(0f)
            .unwrapOr(0f)

        val totalCrossAxisGap = sumAxisGaps(constants.gap.cross(constants.dir), flexLines.len())
        val linesTotalCross: Float = flexLines.map { line -> line.crossSize }.sum() + totalCrossAxisGap

        if (linesTotalCross < containerMinInnerCross) {
            val remaining = containerMinInnerCross - linesTotalCross
            val addition = remaining / flexLines.len().toFloat()
            flexLines.forEach { line -> line.crossSize += addition }
        }
    }
}

/**
 * Determine the used cross size of each flex item.
 *
 * # [9.4. Cross Size Determination](https://www.w3.org/TR/css-flexbox-1/#cross-sizing)
 *
 * - [**Determine the used cross size of each flex item**](https://www.w3.org/TR/css-flexbox-1/#algo-stretch). If a flex item has align-self: stretch, its computed cross size property is auto,
 *     and neither of its cross-axis margins are auto, the used outer cross size is the used cross size of its flex line, clamped according to the item’s used min and max cross sizes.
 *     Otherwise, the used cross size is the item’s hypothetical cross size.
 *
 *     If the flex item has align-self: stretch, redo layout for its contents, treating this used size as its definite cross size so that percentage-sized children can be resolved.
 *
 *     **Note that this step does not affect the main size of the flex item, even if it has an intrinsic aspect ratio**.
 */
fun determineUsedCrossSize(
    tree: LayoutFlexboxContainer,
    flexLines: List<FlexLine>,
    constants: AlgoConstants
) {
    for (line in flexLines) {
        val lineCrossSize = line.crossSize

        for (child in line.items) {
            val childStyle = tree.getFlexboxChildStyle(child.node)
            child.targetSize.setCross(
                constants.dir,
                if (child.alignSelf == AlignSelf.STRETCH
                    && !child.marginIsAuto.crossStart(constants.dir)
                    && !child.marginIsAuto.crossEnd(constants.dir)
                    && childStyle.size().cross(constants.dir) is Dimension.Auto
                ) {
                    // For some reason this particular usage of max_width is an exception to the rule that max_width's transfer
                    // using the aspect_ratio (if set). Both Chrome and Firefox agree on this. And reading the spec, it seems like
                    // a reasonable interpretation. Although it seems to me that the spec *should* apply aspect_ratio here.
                    val padding = childStyle.padding().resolveOrZero(constants.nodeInnerSize)
                    val border = childStyle.border().resolveOrZero(constants.nodeInnerSize)
                    val pbSum = (padding + border).sumAxes()
                    val boxSizingAdjustment =
                        if (childStyle.boxSizing() == BoxSizing.CONTENT_BOX) {
                            pbSum
                        } else {
                            Size.ZERO.clone()
                        }

                    val maxSizeIgnoringAspectRatio = childStyle
                        .maxSize()
                        .maybeResolve(constants.nodeInnerSize)
                        .maybeAdd(boxSizingAdjustment)

                    (lineCrossSize - child.margin.crossAxisSum(constants.dir)).maybeClamp(
                        child.minSize.cross(constants.dir),
                        maxSizeIgnoringAspectRatio.cross(constants.dir)
                    )
                } else {
                    child.hypotheticalInnerSize.cross(constants.dir)
                }
            )

            child.outerTargetSize.setCross(
                constants.dir,
                child.targetSize.cross(constants.dir) + child.margin.crossAxisSum(constants.dir)
            )
        }
    }
}

/**
 * Distribute any remaining free space.
 *
 * # [9.5. Main-Axis Alignment](https://www.w3.org/TR/css-flexbox-1/#main-alignment)
 *
 * - [**Distribute any remaining free space**](https://www.w3.org/TR/css-flexbox-1/#algo-main-align). For each flex line:
 *
 *     1. If the remaining free space is positive and at least one main-axis margin on this line is `auto`, distribute the free space equally among these margins.
 *         Otherwise, set all `auto` margins to zero.
 *
 *     2. Align the items along the main-axis per `justify-content`.
 */
fun distributeRemainingFreeSpace(
    flexLines: List<FlexLine>,
    constants: AlgoConstants
) {
    for (line in flexLines) {
        val totalMainAxisGap = sumAxisGaps(constants.gap.main(constants.dir), line.items.len())
        val usedSpace: Float = totalMainAxisGap +
                line.items.map { child -> child.outerTargetSize.main(constants.dir) }.sum()
        val freeSpace = constants.innerContainerSize.main(constants.dir) - usedSpace
        var numAutoMargins = 0

        for (child in line.items) {
            if (child.marginIsAuto.mainStart(constants.dir)) {
                numAutoMargins += 1
            }
            if (child.marginIsAuto.mainEnd(constants.dir)) {
                numAutoMargins += 1
            }
        }

        if (freeSpace > 0.0 && numAutoMargins > 0) {
            val margin = freeSpace / numAutoMargins.toFloat()

            for (child in line.items) {
                if (child.marginIsAuto.mainStart(constants.dir)) {
                    if (constants.isRow) {
                        child.margin.left = margin
                    } else {
                        child.margin.top = margin
                    }
                }
                if (child.marginIsAuto.mainEnd(constants.dir)) {
                    if (constants.isRow) {
                        child.margin.right = margin
                    } else {
                        child.margin.bottom = margin
                    }
                }
            }
        } else {
            val numItems = line.items.len()
            val layoutReverse = constants.dir.isReverse()
            val gap = constants.gap.main(constants.dir)
            val isSafe = false; // TODO: Implement safe alignment
            val rawJustifyContentMode = constants.justifyContent.unwrapOr(JustifyContent.FLEX_START)
            val justifyContentMode =
                applyAlignmentFallback(freeSpace, numItems, rawJustifyContentMode, isSafe)

            val justifyItem = { (i, child): IndexedValue<FlexItem> ->
                child.offsetMain =
                    computeAlignmentOffset(freeSpace, numItems, gap, justifyContentMode, layoutReverse, i == 0)
            }

            if (layoutReverse) {
                line.items.rev().enumerate().forEach(justifyItem)
            } else {
                line.items.enumerate().forEach(justifyItem)
            }
        }
    }
}

/**
 * Resolve cross-axis `auto` margins.
 *
 * # [9.6. Cross-Axis Alignment](https://www.w3.org/TR/css-flexbox-1/#cross-alignment)
 *
 * - [**Resolve cross-axis `auto` margins**](https://www.w3.org/TR/css-flexbox-1/#algo-cross-margins).
 *     If a flex item has auto cross-axis margins:
 *
 *     - If its outer cross size (treating those auto margins as zero) is less than the cross size of its flex line,
 *         distribute the difference in those sizes equally to the auto margins.
 *
 *     - Otherwise, if the block-start or inline-start margin (whichever is in the cross axis) is auto, set it to zero.
 *         Set the opposite margin so that the outer cross size of the item equals the cross size of its flex line.
 */
fun resolveCrossAxisAutoMargins(
    flexLines: List<FlexLine>,
    constants: AlgoConstants
) {
    for (line in flexLines) {
        val lineCrossSize = line.crossSize
        val maxBaseline: Float = line.items.map { child -> child.baseline }.fold(0f) { acc, x -> acc.max(x) }

        for (child in line.items) {
            val freeSpace = lineCrossSize - child.outerTargetSize.cross(constants.dir)

            if (child.marginIsAuto.crossStart(constants.dir) && child.marginIsAuto.crossEnd(constants.dir)) {
                if (constants.isRow) {
                    child.margin.top = freeSpace / 2f
                    child.margin.bottom = freeSpace / 2f
                } else {
                    child.margin.left = freeSpace / 2f
                    child.margin.right = freeSpace / 2f
                }
            } else if (child.marginIsAuto.crossStart(constants.dir)) {
                if (constants.isRow) {
                    child.margin.top = freeSpace
                } else {
                    child.margin.left = freeSpace
                }
            } else if (child.marginIsAuto.crossEnd(constants.dir)) {
                if (constants.isRow) {
                    child.margin.bottom = freeSpace
                } else {
                    child.margin.right = freeSpace
                }
            } else {
                // 14. Align all flex items along the cross-axis.
                child.offsetCross = alignFlexItemsAlongCrossAxis(child, freeSpace, maxBaseline, constants)
            }
        }
    }
}

/**
 * Align all flex items along the cross-axis.
 *
 * # [9.6. Cross-Axis Alignment](https://www.w3.org/TR/css-flexbox-1/#cross-alignment)
 *
 * - [**Align all flex items along the cross-axis**](https://www.w3.org/TR/css-flexbox-1/#algo-cross-align) per `align-self`,
 *     if neither of the item's cross-axis margins are `auto`.
 */
fun alignFlexItemsAlongCrossAxis(
    child: FlexItem,
    freeSpace: Float,
    maxBaseline: Float,
    constants: AlgoConstants
): Float {
    return when (child.alignSelf) {
        AlignSelf.START -> 0f
        AlignSelf.FLEX_START -> if (constants.isWrapReverse) freeSpace else 0f
        AlignSelf.END -> freeSpace
        AlignSelf.FLEX_END -> if (constants.isWrapReverse) 0f else freeSpace
        AlignSelf.CENTER -> freeSpace / 2f
        // Until we support vertical writing modes, baseline alignment only makes sense if
        // the constants.direction is row, so we treat it as flex-start alignment in columns.
        AlignSelf.BASELINE -> if (constants.isRow) maxBaseline - child.baseline else if (constants.isWrapReverse) freeSpace else 0f
        AlignSelf.STRETCH -> if (constants.isWrapReverse) freeSpace else 0f
    }
}

/**
 * Determine the flex container’s used cross size.
 *
 * # [9.6. Cross-Axis Alignment](https://www.w3.org/TR/css-flexbox-1/#cross-alignment)
 *
 * - [**Determine the flex container’s used cross size**](https://www.w3.org/TR/css-flexbox-1/#algo-cross-container):
 *
 *     - If the cross size property is a definite size, use that, clamped by the used min and max cross sizes of the flex container.
 *
 *     - Otherwise, use the sum of the flex lines' cross sizes, clamped by the used min and max cross sizes of the flex container.
 */
fun determineContainerCrossSize(
    flexLines: List<FlexLine>,
    nodeSize: Size<Option<Float>>,
    constants: AlgoConstants
): Float {
    val totalCrossAxisGap = sumAxisGaps(constants.gap.cross(constants.dir), flexLines.len())
    val totalLineCrossSize: Float = flexLines.map { line -> line.crossSize }.sum()

    val paddingBorderSum = constants.contentBoxInset.crossAxisSum(constants.dir)
    val crossScrollbarGutter = constants.scrollbarGutter.cross(constants.dir)
    val minCrossSize = constants.minSize.cross(constants.dir)
    val maxCrossSize = constants.maxSize.cross(constants.dir)
    val outerContainerSize = nodeSize
        .cross(constants.dir)
        .unwrapOr(totalLineCrossSize + totalCrossAxisGap + paddingBorderSum)
        .maybeClamp(minCrossSize, maxCrossSize)
        .max(paddingBorderSum - crossScrollbarGutter)
    val innerContainerSize = f32Max(outerContainerSize - paddingBorderSum, 0f)

    constants.containerSize.setCross(constants.dir, outerContainerSize)
    constants.innerContainerSize.setCross(constants.dir, innerContainerSize)

    return totalLineCrossSize
}

/**
 * Align all flex lines per `align-content`.
 *
 * # [9.6. Cross-Axis Alignment](https://www.w3.org/TR/css-flexbox-1/#cross-alignment)
 *
 * - [**Align all flex lines**](https://www.w3.org/TR/css-flexbox-1/#algo-line-align) per `align-content`.
 */
fun alignFlexLinesPerAlignContent(
    flexLines: List<FlexLine>,
    constants: AlgoConstants,
    totalCrossSize: Float
) {
    val numLines = flexLines.len()
    val gap = constants.gap.cross(constants.dir)
    val totalCrossAxisGap = sumAxisGaps(gap, numLines)
    val freeSpace = constants.innerContainerSize.cross(constants.dir) - totalCrossSize - totalCrossAxisGap
    val isSafe = false; // TODO: Implement safe alignment

    val alignContentMode = applyAlignmentFallback(freeSpace, numLines, constants.alignContent, isSafe)

    val alignLine = { (i, line): IndexedValue<FlexLine> ->
        line.offsetCross =
            computeAlignmentOffset(freeSpace, numLines, gap, alignContentMode, constants.isWrapReverse, i == 0)
    }

    if (constants.isWrapReverse) {
        flexLines.rev().enumerate().forEach(alignLine)
    } else {
        flexLines.enumerate().forEach(alignLine)
    }
}

/**
 * Calculates the layout for a flex-item
 */
fun calculateFlexItem(
    tree: LayoutFlexboxContainer,
    item: FlexItem,
    totalOffsetMain: RustDeref<Float>,
    totalOffsetCross: Float,
    lineOffsetCross: Float,
    totalContentSize: RustDeref<Size<Float>>,
    containerSize: Size<Float>,
    nodeInnerSize: Size<Option<Float>>,
    direction: FlexDirection
) {
    val layoutOutput = tree.performChildLayout(
        item.node,
        item.targetSize.map { s -> s.into() },
        nodeInnerSize,
        containerSize.map { s -> s.intoAS() },
        SizingMode.CONTENT_SIZE,
        Line.FALSE,
    )
    val (size, contentSize) = layoutOutput

    val offsetMain = totalOffsetMain.get() +
            item.offsetMain +
            item.margin.mainStart(direction) +
            (item.inset.mainStart(direction).or(item.inset.mainEnd(direction).map { pos -> -pos }).unwrapOr(0f))

    val offsetCross = totalOffsetCross +
            item.offsetCross +
            lineOffsetCross +
            item.margin.crossStart(direction) +
            (item.inset.crossStart(direction).or(item.inset.crossEnd(direction).map { pos -> -pos }).unwrapOr(0f))

    if (direction.isRow()) {
        val baselineOffsetCross = totalOffsetCross + item.offsetCross + item.margin.crossStart(direction)
        val innerBaseline = layoutOutput.firstBaselines.y.unwrapOr(size.height)
        item.baseline = baselineOffsetCross + innerBaseline
    } else {
        val baselineOffsetMain = totalOffsetMain.get() + item.offsetMain + item.margin.mainStart(direction)
        val innerBaseline = layoutOutput.firstBaselines.y.unwrapOr(size.height)
        item.baseline = baselineOffsetMain + innerBaseline
    }

    val location = when (direction.isRow()) {
        true -> Point(x = offsetMain, y = offsetCross)
        false -> Point(x = offsetCross, y = offsetMain)
    }
    val scrollbarSize = Size(
        width = if (item.overflow.y == Overflow.SCROLL) item.scrollbarWidth else 0f,
        height = if (item.overflow.x == Overflow.SCROLL) item.scrollbarWidth else 0f
    )

    tree.setUnroundedLayout(
        item.node,
        Layout(
            order = item.order,
            size = size,
            contentSize = contentSize,
            scrollbarSize = scrollbarSize,
            location = location,
            padding = item.padding,
            border = item.border,
            margin = item.margin
        )
    )

    totalOffsetMain += item.offsetMain + item.margin.mainAxisSum(direction) + size.main(direction)

    totalContentSize.set(
        totalContentSize.get().f32Max(computeContentSizeContribution(location, size, contentSize, item.overflow))
    )
}

/**
 * Calculates the layout line
 */
fun calculateLayoutLine(
    tree: LayoutFlexboxContainer,
    line: FlexLine,
    totalOffsetCross: RustDeref<Float>,
    contentSize: RustDeref<Size<Float>>,
    containerSize: Size<Float>,
    nodeInnerSize: Size<Option<Float>>,
    paddingBorder: Rect<Float>,
    direction: FlexDirection
) {
    val totalOffsetMain = RustDeref(paddingBorder.mainStart(direction))
    val lineOffsetCross = line.offsetCross

    if (direction.isReverse()) {
        for (item in line.items.rev()) {
            calculateFlexItem(
                tree,
                item,
                totalOffsetMain,
                totalOffsetCross.get(),
                lineOffsetCross,
                contentSize,
                containerSize,
                nodeInnerSize,
                direction
            )
        }
    } else {
        for (item in line.items) {
            calculateFlexItem(
                tree,
                item,
                totalOffsetMain,
                totalOffsetCross.get(),
                lineOffsetCross,
                contentSize,
                containerSize,
                nodeInnerSize,
                direction,
            )
        }
    }

    totalOffsetCross += lineOffsetCross + line.crossSize
}

/**
 * Do a final layout pass and collect the resulting layouts.
 */
fun finalLayoutPass(
    tree: LayoutFlexboxContainer,
    flexLines: List<FlexLine>,
    constants: AlgoConstants
): Size<Float> {
    val totalOffsetCross = constants.contentBoxInset.crossStart(constants.dir)

    val contentSize = RustDeref(Size.ZERO.clone())

    if (constants.isWrapReverse) {
        for (line in flexLines.rev()) {
            calculateLayoutLine(
                tree,
                line,
                RustDeref(totalOffsetCross),
                contentSize,
                constants.containerSize,
                constants.nodeInnerSize,
                constants.contentBoxInset,
                constants.dir,
            )
        }
    } else {
        for (line in flexLines) {
            calculateLayoutLine(
                tree,
                line,
                RustDeref(totalOffsetCross),
                contentSize,
                constants.containerSize,
                constants.nodeInnerSize,
                constants.contentBoxInset,
                constants.dir
            )
        }
    }

    return contentSize.get()
}

/**
 * Perform absolute layout on all absolutely positioned children.
 */
fun performAbsoluteLayoutOnAbsoluteChildren(
    tree: LayoutFlexboxContainer,
    node: Int,
    constants: AlgoConstants
): Size<Float> {
    val containerWidth = constants.containerSize.width
    val containerHeight = constants.containerSize.height
    val insetRelativeSize =
        constants.containerSize - constants.border.sumAxes() - constants.scrollbarGutter.into()

    var contentSize = Size.ZERO.clone()

    for (order in 0 until tree.childCount(node)) {
        val child = tree.getChildId(node, order)
        val childStyle = tree.getFlexboxChildStyle(child)

        // Skip items that are display:none or are not position:absolute
        if (childStyle.boxGenerationMode() == BoxGenerationMode.NONE ||
            childStyle.position() != Position.ABSOLUTE
        ) {
            continue
        }

        val overflow = childStyle.overflow()
        val scrollbarWidth = childStyle.scrollbarWidth()
        val aspectRatio = childStyle.aspectRatio()
        val alignSelf = childStyle.alignSelf().unwrapOr(constants.alignItems)
        val margin = childStyle.margin().map { margin -> margin.resolveToOption(insetRelativeSize.width) }
        val padding = childStyle.padding().resolveOrZero(Option.Some(insetRelativeSize.width))
        val border = childStyle.border().resolveOrZero(Option.Some(insetRelativeSize.width))
        val paddingBorderSum = (padding + border).sumAxes()
        var boxSizingAdjustment =
            if (childStyle.boxSizing() == BoxSizing.CONTENT_BOX) paddingBorderSum else Size.ZERO.clone()

        // Resolve inset
        // Insets are resolved against the container size minus border
        val left = childStyle.inset().left.maybeResolve(insetRelativeSize.width)
        val right = childStyle.inset().right.maybeResolve(insetRelativeSize.width)
        val top = childStyle.inset().top.maybeResolve(insetRelativeSize.height)
        val bottom = childStyle.inset().bottom.maybeResolve(insetRelativeSize.height)

        // Compute known dimensions from min/max/inherent size styles
        val styleSize = childStyle
            .size()
            .maybeResolve(insetRelativeSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
        val minSize = childStyle
            .minSize()
            .maybeResolve(insetRelativeSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
            .or(paddingBorderSum.map { v -> Option.Some(v) })
            .maybeMax(paddingBorderSum)
        val maxSize = childStyle
            .maxSize()
            .maybeResolve(insetRelativeSize)
            .maybeApplyAspectRatio(aspectRatio)
            .maybeAdd(boxSizingAdjustment)
        var knownDimensions = styleSize.maybeClamp(minSize, maxSize)

        // Fill in width from left/right and reapply aspect ratio if:
        //   - Width is not already known
        //   - Item has both left and right inset properties set
        val (width, lf, rg) = Triple(knownDimensions.width, left, right)
        if (width.isNone() && lf.isSome() && rg.isSome()) {
            val newWidthRaw =
                insetRelativeSize.width.maybeSub(margin.left).maybeSub(margin.right) - lf.unwrap() - rg.unwrap()
            knownDimensions.width = Option.Some(f32Max(newWidthRaw, 0f))
            knownDimensions =
                knownDimensions.maybeApplyAspectRatio(aspectRatio).maybeClamp(minSize, maxSize)
        }

        // Fill in height from top/bottom and reapply aspect ratio if:
        //   - Height is not already known
        //   - Item has both top and bottom inset properties set
        val (height, tp, bt) = Triple(knownDimensions.height, top, bottom)
        if (height.isNone() && tp.isSome() && bt.isSome()) {
            val newHeightRaw =
                insetRelativeSize.height.maybeSub(margin.top).maybeSub(margin.bottom) - tp.unwrap() - bt.unwrap()
            knownDimensions.height = Option.Some(f32Max(newHeightRaw, 0f))
            knownDimensions =
                knownDimensions.maybeApplyAspectRatio(aspectRatio).maybeClamp(minSize, maxSize)
        }
        val layoutOutput = tree.performChildLayout(
            child,
            knownDimensions,
            constants.nodeInnerSize,
            Size(
                width = AvailableSpace.Definite(containerWidth.maybeClamp(minSize.width, maxSize.width)),
                height = AvailableSpace.Definite(containerHeight.maybeClamp(minSize.height, maxSize.height))
            ),
            SizingMode.INHERENT_SIZE,
            Line.FALSE
        )
        val measuredSize = layoutOutput.size
        val finalSize = knownDimensions.unwrapOr(measuredSize).maybeClamp(minSize, maxSize)

        val nonAutoMargin = margin.map { m -> m.unwrapOr(0f) }

        val freeSpace = Size(
            width = constants.containerSize.width - finalSize.width - nonAutoMargin.horizontalAxisSum(),
            height = constants.containerSize.height - finalSize.height - nonAutoMargin.verticalAxisSum(),
        )
            .f32Max(Size.ZERO.clone())

        // Expand auto margins to fill available space
        val resolvedMargin = run {
            val autoMarginSize = Size(
                width = run {
                    val autoMarginCount = margin.left.isNone().toInt() + margin.right.isNone().toInt()
                    if (autoMarginCount > 0f) {
                        freeSpace.width / autoMarginCount.toFloat()
                    } else {
                        0f
                    }
                },
                height = run {
                    val autoMarginCount = margin.top.isNone().toInt() + margin.bottom.isNone().toInt()
                    if (autoMarginCount > 0) {
                        freeSpace.height / autoMarginCount.toFloat()
                    } else {
                        0f
                    }
                }
            )

            Rect(
                left = margin.left.unwrapOr(autoMarginSize.width),
                right = margin.right.unwrapOr(autoMarginSize.width),
                top = margin.top.unwrapOr(autoMarginSize.height),
                bottom = margin.bottom.unwrapOr(autoMarginSize.height),
            )
        }

        // Determine flex-relative insets
        val (startMain, endMain) = if (constants.isRow) Pair(left, right) else Pair(top, bottom)
        val (startCross, endCross) = if (constants.isRow) Pair(top, bottom) else Pair(left, right)

        // Apply main-axis alignment
        // var free_main_space = free_space.main(constants.dir) - resolved_margin.main_axis_sum(constants.dir)
        val offsetMain: Float = if (startMain.isSome()) {
            startMain.unwrap() + constants.border.mainStart(constants.dir) + resolvedMargin.mainStart(constants.dir)
        } else if (endMain.isSome()) {
            constants.containerSize.main(constants.dir) -
                    constants.border.mainEnd(constants.dir) -
                    constants.scrollbarGutter.main(constants.dir) -
                    finalSize.main(constants.dir) -
                    endMain.unwrap() -
                    resolvedMargin.mainEnd(constants.dir)
        } else {
            val jc = constants.justifyContent.unwrapOr(JustifyContent.START)
            val isWrapReverse = constants.isWrapReverse

            // Stretch is an invalid value for justify_content in the flexbox algorithm, so we
            // treat it as if it wasn't set (and thus we default to FlexStart behaviour)
            if (
                (jc == JustifyContent.SPACE_BETWEEN) ||
                (jc == JustifyContent.START) ||
                (jc == JustifyContent.STRETCH && !isWrapReverse) ||
                (jc == JustifyContent.FLEX_START && !isWrapReverse) ||
                (jc == JustifyContent.FLEX_END && isWrapReverse)
            ) {
                constants.contentBoxInset.mainStart(constants.dir) + resolvedMargin.mainStart(constants.dir)
            } else if (
                (jc == JustifyContent.END) ||
                (jc == JustifyContent.FLEX_END && !isWrapReverse) ||
                (jc == JustifyContent.FLEX_START && isWrapReverse) ||
                (jc == JustifyContent.STRETCH && isWrapReverse)
            ) {
                constants.containerSize.main(constants.dir) -
                        constants.contentBoxInset.mainEnd(constants.dir) -
                        finalSize.main(constants.dir) -
                        resolvedMargin.mainEnd(constants.dir)
            } else if (
                (jc == JustifyContent.SPACE_EVENLY) ||
                (jc == JustifyContent.SPACE_AROUND) ||
                (jc == JustifyContent.CENTER)
            ) {
                (constants.containerSize.main(constants.dir) +
                        constants.contentBoxInset.mainStart(constants.dir) -
                        constants.contentBoxInset.mainEnd(constants.dir) -
                        finalSize.main(constants.dir) +
                        resolvedMargin.mainStart(constants.dir) -
                        resolvedMargin.mainEnd(constants.dir)) / 2f
            } else {
                0f // Should never produce
            }
        }

        // Apply cross-axis alignment
        // var free_cross_space = free_space.cross(constants.dir) - resolved_margin.cross_axis_sum(constants.dir)
        val offsetCross: Float = if (startCross.isSome()) {
            startCross.unwrap() + constants.border.crossStart(constants.dir) + resolvedMargin.crossStart(constants.dir)
        } else if (endCross.isSome()) {
            constants.containerSize.cross(constants.dir) -
                    constants.border.crossEnd(constants.dir) -
                    constants.scrollbarGutter.cross(constants.dir) -
                    finalSize.cross(constants.dir) -
                    endCross.unwrap() -
                    resolvedMargin.crossEnd(constants.dir)
        } else {
            val isWrapReverse = constants.isWrapReverse

            // Stretch alignment does not apply to absolutely positioned items
            // See "Example 3" at https://www.w3.org/TR/css-flexbox-1/#abspos-items
            // Note: Stretch should be FlexStart not Start when we support both
            if (
                (alignSelf == AlignItems.START) ||
                ((alignSelf == AlignItems.BASELINE || alignSelf == AlignItems.STRETCH || alignSelf == AlignItems.FLEX_START) && !isWrapReverse) ||
                (alignSelf == AlignItems.FLEX_END && isWrapReverse)
            ) {
                constants.contentBoxInset.crossStart(constants.dir) + resolvedMargin.crossStart(constants.dir)
            } else if (
                (alignSelf == AlignItems.END) ||
                ((alignSelf == AlignItems.BASELINE || alignSelf == AlignItems.STRETCH || alignSelf == AlignItems.FLEX_START) && isWrapReverse) ||
                (alignSelf == AlignItems.FLEX_END && !isWrapReverse)
            ) {
                constants.containerSize.cross(constants.dir) -
                        constants.contentBoxInset.crossEnd(constants.dir) -
                        finalSize.cross(constants.dir) -
                        resolvedMargin.crossEnd(constants.dir)
            } else if (alignSelf == AlignItems.CENTER) {
                (constants.containerSize.cross(constants.dir) +
                        constants.contentBoxInset.crossStart(constants.dir) -
                        constants.contentBoxInset.crossEnd(constants.dir) -
                        finalSize.cross(constants.dir) +
                        resolvedMargin.crossStart(constants.dir) -
                        resolvedMargin.crossEnd(constants.dir)
                        ) / 2f
            } else {
                0f // Should never produce
            }
        }

        val location = when (constants.isRow) {
            true -> Point(x = offsetMain, y = offsetCross)
            false -> Point(x = offsetCross, y = offsetMain)
        }
        val scrollbarSize = Size(
            width = if (overflow.y == Overflow.SCROLL) scrollbarWidth else 0f,
            height = if (overflow.x == Overflow.SCROLL) scrollbarWidth else 0f
        )
        tree.setUnroundedLayout(
            child,
            Layout(
                order = order,
                size = finalSize,
                contentSize = layoutOutput.contentSize,
                scrollbarSize = scrollbarSize,
                location = location,
                padding = padding,
                border = border,
                margin = resolvedMargin
            )
        )

        val sizeContentSizeContribution = Size(
            width = if (overflow.x == Overflow.VISIBLE) {
                f32Max(finalSize.width, layoutOutput.contentSize.width)
            } else {
                finalSize.width
            },
            height = if (overflow.y == Overflow.VISIBLE) {
                f32Max(finalSize.height, layoutOutput.contentSize.height)
            } else {
                finalSize.height
            }
        )
        if (sizeContentSizeContribution.hasNonZeroArea()) {
            val contentSizeContribution = Size(
                width = location.x + sizeContentSizeContribution.width,
                height = location.y + sizeContentSizeContribution.height,
            )
            contentSize = contentSize.f32Max(contentSizeContribution)
        }
    }

    return contentSize
}

/**
 * Computes the total space taken up by gaps in an axis given:
 *   - The size of each gap
 *   - The number of items (children or flex-lines) between which there are gaps
 */
fun sumAxisGaps(
    gap: Float,
    numItems: Int
): Float {
    // Gaps only exist between items, so...
    return if (numItems <= 1) {
        // ...if there are less than 2 items then there are no gaps
        0f
    } else {
        // ...otherwise there are (num_items - 1) gaps
        gap * (numItems - 1).toFloat()
    }
}
