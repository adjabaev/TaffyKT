package be.arby.taffy.compute.flexbox

import be.arby.taffy.compute.GenericAlgorithm
import be.arby.taffy.compute.common.Alignment
import be.arby.taffy.geometry.Point
import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.geometry.extensions.*
import be.arby.taffy.lang.*
import be.arby.taffy.layout.*
import be.arby.taffy.maths.*
import be.arby.taffy.node.Node
import be.arby.taffy.resolve.*
import be.arby.taffy.style.Display
import be.arby.taffy.style.Position
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.alignment.*
import be.arby.taffy.style.flex.FlexDirection
import be.arby.taffy.style.flex.FlexWrap
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.tree.layout.RunMode
import be.arby.taffy.util.maybeClamp
import be.arby.taffy.util.maybeMax
import be.arby.taffy.util.maybeMin
import be.arby.taffy.util.maybeSub
import be.arby.taffy.utils.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class FlexBox {

    companion object {
        @JvmStatic
        fun compute(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            runMode: RunMode,
        ): SizeAndBaselines {
            val style = tree.style(node)
            val hasMinMaxSizes =
                style.minSize.width.isDefined() || style.minSize.height.isDefined() || style.maxSize.width.isDefined() || style.maxSize.height.isDefined()

            // Pull these out earlier to avoid borrowing issues
            val aspectRatio = style.aspectRatio
            val minSize = style.minSize.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)
            val maxSize = style.maxSize.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)
            val clampedStyleSize =
                style.size.maybeResolveStS(parentSize).maybeApplyAspectRatio(aspectRatio)
                    .maybeClamp(minSize, maxSize)

            // If both min and max in a given axis are set and max <= min then this determines the size in that axis
            val minMaxDefiniteSize = minSize.zipMap(maxSize) { min, max ->
                if (min.isSome() && max.isSome() && max.unwrap() <= min.unwrap()) {
                    Option.Some(min.unwrap())
                } else {
                    Option.None
                }
            }
            val styledBasedKnownDimensions = knownDimensions.or(minMaxDefiniteSize).or(clampedStyleSize)

            if (styledBasedKnownDimensions.bothAxisDefined() || !hasMinMaxSizes) {
                // Single pass
                return computePreliminary(tree, node, styledBasedKnownDimensions, parentSize, availableSpace, runMode)
            } else {
                // Two pass
                val firstPass = computePreliminary(
                    tree,
                    node,
                    styledBasedKnownDimensions,
                    parentSize,
                    availableSpace,
                    RunMode.COMPUTE_SIZE
                ).size

                val clampedFirstPassSize = firstPass.maybeClamp(minSize, maxSize)

                return computePreliminary(
                    tree,
                    node,
                    styledBasedKnownDimensions.zipMap(clampedFirstPassSize) { known, firstPass ->
                        known.orElse { Option.Some(firstPass) }
                    },
                    parentSize,
                    availableSpace,
                    runMode
                )
            }
        }

        private fun computePreliminary(
            tree: LayoutTree,
            node: Node,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            runMode: RunMode,
        ): SizeAndBaselines {
            // Define some general constants we will need for the remainder of the algorithm.
            val constants = computeConstants(tree.style(node), knownDimensions, parentSize)

            // 9. Flex Layout Algorithm

            // 9.1. Initial Setup

            // 1. Generate anonymous flex items as described in §4 Flex Items.
            val flexItems = generateAnonymousFlexItems(tree, node, constants)

            // 9.2. Line Length Determination

            // 2. Determine the available main and cross space for the flex items
            val availableSpace = determineAvailableSpace(knownDimensions, availableSpace, constants)

            // 3. Determine the flex base size and hypothetical main size of each item.
            determineFlexBaseSize(tree, constants, availableSpace, flexItems)

            // 4. Determine the main size of the flex container

            // This has already been done as part of compute_constants. The inner size is exposed as constants.nodeInnerSize.

            // 9.3. Main Size Determination

            // 5. Collect flex items into flex lines.
            val flexLines = collectFlexLines(tree, node, constants, availableSpace, flexItems)

            // If container size is undefined, determine the container's main size
            // and then re-resolve gaps based on newly determined size
            val originalGap = constants.gap.copy()

            val v = constants.nodeInnerSize.main(constants.dir)

            if (v.isSome()) {
                val innerMainSize = v.unwrap()
                val outerMainSize = innerMainSize + constants.paddingBorder.mainAxisSum(constants.dir)
                constants.innerContainerSize.setMain(constants.dir, innerMainSize)
                constants.containerSize.setMain(constants.dir, outerMainSize)
            } else {
                // Sets constants.container_size and constants.outer_container_size
                determineContainerMainSize(tree, availableSpace.main(constants.dir), flexLines, constants)
                constants.nodeInnerSize.setMain(
                    constants.dir,
                    Option.Some(constants.innerContainerSize.main(constants.dir))
                )
                constants.nodeOuterSize.setMain(constants.dir, Option.Some(constants.containerSize.main(constants.dir)))

                // Re-resolve percentage gaps
                val style = tree.style(node)
                val innerContainerSize = constants.innerContainerSize.main(constants.dir)

                val newGap = style.gap.main(constants.dir).maybeResolve(innerContainerSize).unwrapOr(0.0f)
                constants.gap.setMain(constants.dir, newGap)
            }

            // 6. Resolve the flexible lengths of all the flex items to find their used main size.
            for (line in flexLines) {
                resolveFlexibleLengths(tree, line, constants, originalGap)
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
            calculateCrossSize(tree, flexLines, knownDimensions, constants)

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
            distributeRemainingFreeSpace(tree, flexLines, node, constants)

            // 9.6. Cross-Axis Alignment

            // 13. Resolve cross-axis auto margins (also includes 14).
            resolveCrossAxisAutoMargins(tree, flexLines, constants)

            // 15. Determine the flex container’s used cross size.
            val totalLineCrossSize = determineContainerCrossSize(flexLines, knownDimensions, constants)

            // We have the container size.
            // If our caller does not care about performing layout we are done now.
            if (runMode == RunMode.COMPUTE_SIZE) {
                return constants.containerSize.intoSB()
            }

            // 16. Align all flex lines per align-content.
            alignFlexLinesPerAlignContent(tree, flexLines, node, constants, totalLineCrossSize)

            // Do a final layout pass and gather the resulting layouts
            finalLayoutPass(tree, node, flexLines, constants)

            // Before returning we perform a    bsolute layout on all absolutely positioned children
            performAbsoluteLayoutOnAbsoluteChildren(tree, node, constants)

            val len = tree.childCount(node)
            for (order in 0 until len) {
                val child = tree.child(node, order)
                if (tree.style(child).display == Display.NONE) {
                    tree.layout(node, Layout.withOrder(order))
                    GenericAlgorithm.measureSize(
                        tree,
                        child,
                        Size.none(),
                        Size.none(),
                        Size.MAX_CONTENT,
                        SizingMode.INHERENT_SIZE
                    )
                }
            }

            // 8.5. Flex Container Baselines: calculate the flex container's first baseline
            // See https://www.w3.org/TR/css-flexbox-1/#flex-baselines
            val firstVerticalBaseline: Option<Float> = if (flexLines.isEmpty()) {
                Option.None
            } else {
                flexLines[0]
                    .items
                    .findOptional { item -> constants.isColumn || item.alignSelf == AlignSelf.BASELINE }
                    .orElse { flexLines[0].items.next() }
                    .map { child ->
                        val offsetVertical = if (constants.isRow) {
                            child.offsetCross
                        } else {
                            child.offsetMain
                        }
                        offsetVertical + child.baseline
                    }
            }

            return SizeAndBaselines(
                size = constants.containerSize,
                firstBaselines = Point(x = Option.None, y = firstVerticalBaseline)
            )
        }

        fun computeConstants(
            style: Style,
            knownDimensions: Size<Option<Float>>,
            parentSize: Size<Option<Float>>
        ): AlgoConstants {
            val dir = style.flexDirection
            val isRow = dir.isRow()
            val isColumn = dir.isColumn()
            val isWrap = style.flexWrap == FlexWrap.WRAP || style.flexWrap == FlexWrap.WRAP_REVERSE
            val isWrapReverse = style.flexWrap == FlexWrap.WRAP_REVERSE

            val margin = style.margin.resolveOrZeroOtRlpa(parentSize.width)
            val padding = style.padding.resolveOrZeroOtRlp(parentSize.width)
            val border = style.border.resolveOrZeroOtRlp(parentSize.width)
            val alignItems = style.alignItems.unwrapOr(AlignItems.STRETCH)
            val alignContent = style.alignContent.unwrapOr(AlignContent.STRETCH)

            val paddingBorder = padding + border

            val nodeOuterSize = knownDimensions.copy()

            val nodeInnerSize = Size(
                width = nodeOuterSize.width.maybeSub(paddingBorder.horizontalAxisSum()),
                height = nodeOuterSize.height.maybeSub(paddingBorder.verticalAxisSum()),
            )
            val gap = style.gap.resolveOrZeroStS(nodeInnerSize.or(Size.zeroOF()))

            val containerSize = Size.zeroF()
            val innerContainerSize = Size.zeroF()

            return AlgoConstants(
                dir,
                isRow,
                isColumn,
                isWrap,
                isWrapReverse,
                margin,
                border,
                paddingBorder,
                gap,
                alignItems,
                alignContent,
                nodeOuterSize,
                nodeInnerSize,
                containerSize,
                innerContainerSize
            )
        }

        private fun generateAnonymousFlexItems(tree: LayoutTree, node: Node, constants: AlgoConstants): List<FlexItem> {
            return tree.children(node).map { child -> Pair(child, tree.style(child)) }
                .filter { (_, style) -> style.position != Position.ABSOLUTE }
                .filter { (_, style) -> style.display != Display.NONE }.map { (child, childStyle) ->
                    val aspectRatio = childStyle.aspectRatio
                    FlexItem(
                        node = child,
                        size = childStyle.size.maybeResolveStS(constants.nodeInnerSize)
                            .maybeApplyAspectRatio(aspectRatio),
                        minSize = childStyle.minSize.maybeResolveStS(constants.nodeInnerSize)
                            .maybeApplyAspectRatio(aspectRatio),
                        maxSize = childStyle.maxSize.maybeResolveStS(constants.nodeInnerSize)
                            .maybeApplyAspectRatio(aspectRatio),

                        inset = childStyle.inset.zipSize(constants.nodeInnerSize) { p, s -> p.maybeResolve(s) },
                        margin = childStyle.margin.resolveOrZeroOtRlpa(constants.nodeInnerSize.width),
                        padding = childStyle.padding.resolveOrZeroOtRlp(constants.nodeInnerSize.width),
                        border = childStyle.border.resolveOrZeroOtRlp(constants.nodeInnerSize.width),
                        alignSelf = childStyle.alignSelf.unwrapOr(constants.alignItems),
                        flexGrow = childStyle.flexGrow,
                        flexShrink = childStyle.flexShrink,
                        flexBasis = 0.0f,
                        innerFlexBasis = 0.0f,
                        violation = 0.0f,
                        frozen = false,

                        resolvedMinimumSize = Size.zeroF(),
                        hypotheticalInnerSize = Size.zeroF(),
                        hypotheticalOuterSize = Size.zeroF(),
                        targetSize = Size.zeroF(),
                        outerTargetSize = Size.zeroF(),
                        contentFlexFraction = 0.0f,

                        baseline = 0.0f,

                        offsetMain = 0.0f,
                        offsetCross = 0.0f
                    )
                }
        }

        private fun determineAvailableSpace(
            knownDimensions: Size<Option<Float>>, outerAvailableSpace: Size<AvailableSpace>, constants: AlgoConstants
        ): Size<AvailableSpace> {
            // Note: min/max/preferred size styles have already been applied to known_dimensions in the `compute` function above
            val width = when {
                knownDimensions.width.isSome() ->
                    AvailableSpace.Definite(knownDimensions.width.unwrap() - constants.paddingBorder.horizontalAxisSum())

                else -> outerAvailableSpace.width.maybeSub(constants.margin.horizontalAxisSum())
                    .maybeSub(constants.paddingBorder.horizontalAxisSum())
            }
            val height = when {
                knownDimensions.height.isSome() ->
                    AvailableSpace.Definite(knownDimensions.height.unwrap() - constants.paddingBorder.verticalAxisSum())

                else -> outerAvailableSpace.height.maybeSub(constants.margin.verticalAxisSum())
                    .maybeSub(constants.paddingBorder.verticalAxisSum())
            }

            return Size(width, height)
        }

        private fun determineFlexBaseSize(
            tree: LayoutTree,
            constants: AlgoConstants,
            availableSpace: Size<AvailableSpace>,
            flexItems: List<FlexItem>,
        ) {
            for (child in flexItems) {
                val childStyle = tree.style(child.node)

                // A. If the item has a definite used flex basis, that’s the flex base size.

                // B. If the flex item has an intrinsic aspect ratio,
                //    a used flex basis of content, and a definite cross size,
                //    then the flex base size is calculated from its inner
                //    cross size and the flex item’s intrinsic aspect ratio.

                // Note: `child.size` has already been resolved against aspect_ratio in generate_anonymous_flex_items
                // So B will just work here by using main_size without special handling for aspect_ratio

                val flexBasis = childStyle.flexBasis.maybeResolve(constants.nodeInnerSize.main(constants.dir))
                val mainSize = child.size.main(constants.dir)
                val cond = flexBasis.orElse { mainSize }
                if (cond.isSome()) {
                    child.flexBasis = cond.unwrap()
                    continue
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

                // TODO - if/when vertical writing modes are supported

                // E. Otherwise, size the item into the available space using its used flex basis
                //    in place of its main size, treating a value of content as max-content.
                //    If a cross size is needed to determine the main size (e.g. when the
                //    flex item’s main size is in its block axis) and the flex item’s cross size
                //    is auto and not definite, in this calculation use fit-content as the
                //    flex item’s cross size. The flex base size is the item’s resulting main size.

                val childKnownDimensions = child.size.copy()
                if (child.alignSelf == AlignSelf.STRETCH && childKnownDimensions.cross(constants.dir).isNone()) {
                    childKnownDimensions.setCross(
                        constants.dir,
                        availableSpace
                            .cross(constants.dir)
                            .intoOption()
                            .maybeSub(constants.margin.crossAxisSum(constants.dir))
                    )
                }

                child.flexBasis = GenericAlgorithm.measureSize(
                    tree,
                    child.node,
                    childKnownDimensions,
                    constants.nodeInnerSize,
                    availableSpace,
                    SizingMode.CONTENT_SIZE
                ).main(constants.dir)
            }

            // The hypothetical main size is the item’s flex base size clamped according to its
            // used min and max main sizes (and flooring the content box size at zero).

            for (child in flexItems) {
                child.innerFlexBasis =
                    child.flexBasis - child.padding.mainAxisSum(constants.dir) - child.border.mainAxisSum(constants.dir)

                val hypotheticalInnerMinMain = child.minSize.main(constants.dir)
                child.hypotheticalInnerSize.setMain(
                    constants.dir,
                    child.flexBasis.maybeClamp(hypotheticalInnerMinMain, child.maxSize.main(constants.dir)),
                )
                child.hypotheticalOuterSize.setMain(
                    constants.dir,
                    child.hypotheticalInnerSize.main(constants.dir) + child.margin.mainAxisSum(constants.dir),
                )

                val minContentSize = GenericAlgorithm.measureSize(
                    tree,
                    child.node,
                    Size.none(),
                    constants.nodeInnerSize,
                    Size.MIN_CONTENT,
                    SizingMode.CONTENT_SIZE
                )

                // 4.5. Automatic Minimum Size of Flex Items
                // https://www.w3.org/TR/css-flexbox-1/#min-size-auto
                val clampedMinContentSize = minContentSize.maybeMin(child.size).maybeMin(child.maxSize)
                child.resolvedMinimumSize = child.minSize.unwrapOr(clampedMinContentSize)
            }
        }

        private fun collectFlexLines(
            tree: LayoutTree,
            node: Node,
            constants: AlgoConstants,
            availableSpace: Size<AvailableSpace>,
            flexItems: List<FlexItem>
        ): List<FlexLine> {
            if (tree.style(node).flexWrap == FlexWrap.NO_WRAP) {
                return listOf(
                    FlexLine(
                        items = flexItems,
                        crossSize = 0.0f,
                        offsetCross = 0.0f,
                    )
                )
            } else {
                return when (val v = availableSpace.main(constants.dir)) {
                    // If we're sizing under a max-content constraint then the flex items will never wrap
                    // (at least for now - future extensions to the CSS spec may add provisions for forced wrap points)

                    is AvailableSpace.MaxContent -> {
                        listOf(
                            FlexLine(
                                items = flexItems,
                                crossSize = 0.0f,
                                offsetCross = 0.0f,
                            )
                        )
                    }

                    // If flex-wrap is Wrap, and we're sizing under a min-content constraint, then we take every possible wrapping opportunity
                    // and place each item in its own line
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
                        val mainAxisAvailableSpace = v.availableSpace

                        val lines = ArrayList<FlexLine>()
                        var flexItems = flexItems
                        val mainAxisGap = constants.gap.main(constants.dir)

                        while (flexItems.isNotEmpty()) {
                            // Find index of the first item in the next line
                            // (or the last item if all remaining items are in the current line)
                            var lineLength = 0.0
                            val index = flexItems
                                .withIndex()
                                .findNullable { (idx, child) ->
                                    // Gaps only occur between items (not before the first one or after the last one)
                                    // So first item in the line does not contribute a gap to the line length
                                    val gapContribution = if (idx == 0) 0.0f else mainAxisGap
                                    lineLength += child.hypotheticalOuterSize.main(constants.dir) + gapContribution

                                    lineLength > mainAxisAvailableSpace && idx != 0
                                }
                                .map { (idx, _) -> idx }
                                .unwrapOr(flexItems.size)


                            val (items, rest) = flexItems.splitAt(index)
                            lines.add(
                                FlexLine(
                                    items = items,
                                    crossSize = 0.0f,
                                    offsetCross = 0.0f
                                )
                            )
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
        private fun determineContainerMainSize(
            tree: LayoutTree,
            mainAxisAvailableSpace: AvailableSpace,
            lines: List<FlexLine>,
            constants: AlgoConstants
        ) {
            val outerMainSize: Float = constants.nodeOuterSize.main(constants.dir).unwrapOrElse {
                when {
                    mainAxisAvailableSpace is AvailableSpace.Definite -> {
                        val mainAxisAvailableSpaceDef = mainAxisAvailableSpace.availableSpace
                        val longestLineLength: Float = lines.map { line ->
                            val lineMainAxisGap = sumAxisGaps(constants.gap.main(constants.dir), line.items.size)
                            val totalTargetSize = line
                                .items
                                .map { child ->
                                    child.flexBasis + child.margin.mainAxisSum(constants.dir)
                                }
                                .sum()
                            totalTargetSize + lineMainAxisGap
                        }
                            .maxByRs { a, b -> a.compareTo(b) }
                            .unwrapOr(0f)

                        val size = longestLineLength + constants.paddingBorder.mainAxisSum(constants.dir)
                        if (lines.size > 1) {
                            f32Max(size, mainAxisAvailableSpaceDef)
                        } else {
                            size
                        }
                    }

                    mainAxisAvailableSpace is AvailableSpace.MinContent && constants.isWrap -> {
                        val longestLineLength: Float = lines
                            .map { line ->
                                val lineMainAxisGap = sumAxisGaps(constants.gap.main(constants.dir), line.items.size)
                                val totalTargetSize = line.items
                                    .map { child ->
                                        child.flexBasis + child.margin.mainAxisSum(constants.dir)
                                    }
                                    .sum()

                                totalTargetSize + lineMainAxisGap
                            }
                            .maxByRs { a, b -> a.compareTo(b) }
                            .unwrapOr(0f)
                        longestLineLength + constants.paddingBorder.mainAxisSum(constants.dir)
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
                                val flexBasisMin = Option.Some(item.flexBasis).filter { _ -> item.flexShrink == 0f }
                                val flexBasisMax = Option.Some(item.flexBasis).filter { _ -> item.flexGrow == 0f }
                                val minMainSize = styleMin.maybeMax(flexBasisMin).orElse { flexBasisMin }
                                val maxMainSize = styleMax.maybeMin(flexBasisMax).orElse { flexBasisMax }

                                val isMin = minMainSize.isSome()
                                val isPref = stylePreferred.isSome()
                                val isMax = maxMainSize.isSome()

                                // If the clamping values are such that max <= min, then we can avoid the expensive step of computing the content size
                                // as we know that the clamping values will override it anyway
                                val contentContribution =
                                    if (isMin && isPref && isMax &&
                                        (maxMainSize.unwrap() <= minMainSize.unwrap() || maxMainSize.unwrap() <= stylePreferred.unwrap())
                                    ) {
                                        val min = minMainSize.unwrap()
                                        val pref = stylePreferred.unwrap()
                                        val max = maxMainSize.unwrap()
                                        pref.min(max).max(min) + item.margin.mainAxisSum(constants.dir)
                                    } else if (isMin && isMax && maxMainSize.unwrap() <= minMainSize.unwrap()) {
                                        minMainSize.unwrap() + item.margin.mainAxisSum(constants.dir)
                                    } else if (isPref && isMax && maxMainSize.unwrap() <= stylePreferred.unwrap()) {
                                        maxMainSize.unwrap() + item.margin.mainAxisSum(constants.dir)
                                        // Else compute the min- or -max content size and apply the full formula for computing the
                                        // min- or max- content contributuon
                                    } else {
                                        // Either the min- or max- content size depending on which constraint we are sizing under.
                                        val contentMainSize = GenericAlgorithm.measureSize(
                                            tree,
                                            item.node,
                                            Size.none(),
                                            constants.nodeInnerSize,
                                            Size(width = mainAxisAvailableSpace, height = mainAxisAvailableSpace),
                                            SizingMode.INHERENT_SIZE
                                        ).main(constants.dir) + item.margin.mainAxisSum(constants.dir)

                                        // This is somewhat bizarre in that it's asymetrical depending whether the flex container is a column or a row.
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
                                            contentMainSize.maybeClamp(styleMin, styleMax)
                                        } else {
                                            contentMainSize.max(item.flexBasis).maybeClamp(styleMin, styleMax)
                                        }
                                    }

                                val diff = contentContribution - item.flexBasis
                                item.contentFlexFraction = if (diff > 0f) {
                                    diff / f32Max(1f, item.flexGrow)
                                } else if (diff < 0f) {
                                    val scaledShrinkFactor = f32Max(1f, item.flexShrink) * item.innerFlexBasis
                                    // let scaled_shrink_factor - f32_max(1.0, item.flex_shrink * item.inner_flex_basis);
                                    diff / scaledShrinkFactor
                                } else {
                                    // We are assuming that diff is 0.0 here and that we haven't accidentally introduced a NaN
                                    0.0f
                                }
                            }

                            // TODO Spec says to scale everything by the line's max flex fraction. But neither Chrome nor firefox implement this
                            // so we don't either. But if we did want to, we'd need this computation here (and to use it below):
                            //
                            // Within each line, find the largest max-content flex fraction among all the flex items.
                            // let line_flex_fraction = line
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
                                    // let flex_fraction = line_flex_fraction;

                                    val flexContribution = if (item.contentFlexFraction > 0.0f) {
                                        f32Max(1.0f, item.flexGrow) * flexFraction
                                    } else if (item.contentFlexFraction < 0.0f) {
                                        val scaledShrinkFactor = f32Max(1.0f, item.flexShrink) * item.innerFlexBasis
                                        scaledShrinkFactor * flexFraction
                                    } else {
                                        0.0f
                                    }
                                    val size = item.flexBasis + flexContribution
                                    item.outerTargetSize.setMain(constants.dir, size)
                                    item.targetSize.setMain(constants.dir, size)
                                    size
                                }.sum()

                            val gapSum = sumAxisGaps(constants.gap.main(constants.dir), line.items.size)
                            mainSize = f32Max(mainSize, itemMainSizeSum + gapSum)
                        }

                        mainSize + constants.paddingBorder.mainAxisSum(constants.dir)
                    }
                }
            }

            // let outer_main_size = inner_main_size + constants.padding_border.main_axis_sum(constants.dir)
            val innerMainSize = outerMainSize - constants.paddingBorder.mainAxisSum(constants.dir)
            constants.containerSize.setMain(constants.dir, outerMainSize)
            constants.innerContainerSize.setMain(constants.dir, innerMainSize)
            constants.nodeInnerSize.setMain(constants.dir, Option.Some(innerMainSize))
        }

        private fun resolveFlexibleLengths(
            tree: LayoutTree,
            line: FlexLine,
            constants: AlgoConstants,
            originalGap: Size<Float>,
        ) {
            val totalOriginalMainAxisGap = sumAxisGaps(originalGap.main(constants.dir), line.items.size)
            val totalMainAxisGap = sumAxisGaps(constants.gap.main(constants.dir), line.items.size)

            // 1. Determine the used flex factor. Sum the outer hypothetical main sizes of all
            //    items on the line. If the sum is less than the flex container’s inner main size,
            //    use the flex grow factor for the rest of this algorithm; otherwise, use the
            //    flex shrink factor.

            val totalHypotheticalOuterMainSize =
                line.items.map { child -> child.hypotheticalOuterSize.main(constants.dir) }.sum()
            val usedFlexFactor = totalOriginalMainAxisGap + totalHypotheticalOuterMainSize
            val growing = usedFlexFactor < constants.nodeInnerSize.main(constants.dir).unwrapOr(0.0f)
            val shrinking = !growing

            // 2. Size inflexible items. Freeze, setting its target main size to its hypothetical main size
            //    - Any item that has a flex factor of zero
            //    - If using the flex grow factor: any item that has a flex base size
            //      greater than its hypothetical main size
            //    - If using the flex shrink factor: any item that has a flex base size
            //      smaller than its hypothetical main size

            for (child in line.items) {
                val innerTargetSize = child.hypotheticalInnerSize.main(constants.dir)
                child.targetSize.setMain(constants.dir, innerTargetSize)

                val childStyle = tree.style(child.node)
                if ((childStyle.flexGrow == 0.0f && childStyle.flexShrink == 0.0f) || (growing && child.flexBasis > child.hypotheticalInnerSize.main(
                        constants.dir
                    )) || (shrinking && child.flexBasis < child.hypotheticalInnerSize.main(constants.dir))
                ) {
                    child.frozen = true
                    val outerTargetSize = innerTargetSize + child.margin.mainAxisSum(constants.dir)
                    child.outerTargetSize.setMain(constants.dir, outerTargetSize)
                }
            }

            // 3. Calculate initial free space. Sum the outer sizes of all items on the line,
            //    and subtract this from the flex container’s inner main size. For frozen items,
            //    use their outer target main size; for other items, use their outer flex base size.

            val usedSpace = totalMainAxisGap + line.items.map { child ->
                child.margin.mainAxisSum(constants.dir) + if (child.frozen) {
                    child.outerTargetSize.main(constants.dir)
                } else {
                    child.flexBasis
                }
            }.sum()

            val initialFreeSpace = constants.nodeInnerSize.main(constants.dir).maybeSub(usedSpace).unwrapOr(0.0f)

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

                val usedSpace = totalMainAxisGap + line.items.map { child ->
                    child.margin.mainAxisSum(constants.dir) + if (child.frozen) {
                        child.outerTargetSize.main(constants.dir)
                    } else {
                        child.flexBasis
                    }
                }.sum()

                val unfrozen: List<FlexItem> = line.items.filter { child -> !child.frozen }

                val (sumFlexGrow, sumFlexShrink) = unfrozen.fold(Pair(0.0f, 0.0f)) { (flexGrow, flexShrink), item ->
                    val style = tree.style(item.node)
                    Pair(flexGrow + style.flexGrow, flexShrink + style.flexShrink)
                }

                val freeSpace = if (growing && sumFlexGrow < 1.0f) {
                    (initialFreeSpace * sumFlexGrow - totalMainAxisGap).maybeMin(
                        constants.nodeInnerSize.main(constants.dir).maybeSub(usedSpace)
                    )
                } else if (shrinking && sumFlexShrink < 1.0f) {
                    (initialFreeSpace * sumFlexShrink - totalMainAxisGap).maybeMax(
                        constants.nodeInnerSize.main(constants.dir).maybeSub(usedSpace)
                    )
                } else {
                    (constants.nodeInnerSize.main(constants.dir).maybeSub(usedSpace)).unwrapOr(usedFlexFactor - usedSpace)
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

                if (freeSpace.isNormal()) {
                    if (growing && sumFlexGrow > 0.0f) {
                        for (child in unfrozen) {
                            child.targetSize.setMain(
                                constants.dir,
                                child.flexBasis + freeSpace * (tree.style(child.node).flexGrow / sumFlexGrow),
                            )
                        }
                    } else if (shrinking && sumFlexShrink > 0.0f) {
                        val sumScaledShrinkFactor =
                            unfrozen.map { child -> child.innerFlexBasis * tree.style(child.node).flexShrink }.sum()

                        if (sumScaledShrinkFactor > 0.0f) {
                            for (child in unfrozen) {
                                val scaledShrinkFactor = child.innerFlexBasis * tree.style(child.node).flexShrink
                                child.targetSize.setMain(
                                    constants.dir,
                                    child.flexBasis + freeSpace * (scaledShrinkFactor / sumScaledShrinkFactor)
                                )
                            }
                        }
                    }
                }

                // d. Fix min/max violations. Clamp each non-frozen item’s target main size by its
                //    used min and max main sizes and floor its content-box size at zero. If the
                //    item’s target main size was made smaller by this, it’s a max violation.
                //    If the item’s target main size was made larger by this, it’s a min violation.

                val totalViolation = unfrozen.fold(0.0f) { acc, child ->
                    val resolvedMinMain: Option<Float> = child.resolvedMinimumSize.main(constants.dir).into()
                    val maxMain = child.maxSize.main(constants.dir)
                    val clamped = child.targetSize.main(constants.dir).maybeClamp(resolvedMinMain, maxMain).max(0.0f)
                    child.violation = clamped - child.targetSize.main(constants.dir)
                    child.targetSize.setMain(constants.dir, clamped)
                    child.outerTargetSize.setMain(
                        constants.dir, child.targetSize.main(constants.dir) + child.margin.mainAxisSum(constants.dir)
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
                    child.frozen = when {
                        totalViolation > 0f -> child.violation > 0f
                        totalViolation < 0f -> child.violation < 0f
                        else -> true
                    }
                }

                // f. Return to the start of this loop.
            }
        }

        private fun determineHypotheticalCrossSize(
            tree: LayoutTree,
            line: FlexLine,
            constants: AlgoConstants,
            availableSpace: Size<AvailableSpace>,
        ) {
            for (child in line.items) {
                val childCross = child.size.cross(constants.dir)
                    .maybeClamp(child.minSize.cross(constants.dir), child.maxSize.cross(constants.dir))

                child.hypotheticalInnerSize.setCross(
                    constants.dir, GenericAlgorithm.measureSize(
                        tree,
                        child.node,
                        Size(
                            width = if (constants.isRow) {
                                child.targetSize.width.into()
                            } else {
                                childCross
                            }, height = if (constants.isRow) {
                                childCross
                            } else {
                                child.targetSize.height.into()
                            }
                        ),
                        constants.nodeInnerSize,
                        Size(
                            width = if (constants.isRow) {
                                constants.containerSize.main(constants.dir).intoAS()
                            } else {
                                availableSpace.width
                            },
                            height = if (constants.isRow) {
                                availableSpace.height
                            } else {
                                constants.containerSize.main(constants.dir).intoAS()
                            },
                        ),
                        SizingMode.CONTENT_SIZE,
                    ).cross(constants.dir)
                        .maybeClamp(child.minSize.cross(constants.dir), child.maxSize.cross(constants.dir))
                )

                child.hypotheticalOuterSize.setCross(
                    constants.dir,
                    child.hypotheticalInnerSize.cross(constants.dir) + child.margin.crossAxisSum(constants.dir),
                )
            }
        }

        private fun calculateChildrenBaseLines(
            tree: LayoutTree,
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
                val lineBaselineChildCount = line.items.count { child -> child.alignSelf == AlignSelf.BASELINE }
                if (lineBaselineChildCount <= 1) {
                    continue
                }

                for (child in line.items) {
                    // Only calculate baselines for children participating in baseline alignment
                    if (child.alignSelf != AlignSelf.BASELINE) {
                        continue
                    }
                    val measuredSizeAndBaselines = GenericAlgorithm.performLayout(
                        tree,
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
                            },
                        ),
                        constants.nodeInnerSize,
                        Size(
                            width = if (constants.isRow) {
                                constants.containerSize.width.intoAS()
                            } else {
                                availableSpace.width.maybeSet(nodeSize.width)
                            },
                            height = if (constants.isRow) {
                                availableSpace.height.maybeSet(nodeSize.height)
                            } else {
                                constants.containerSize.height.intoAS()
                            },
                        ),
                        SizingMode.CONTENT_SIZE,
                    )

                    val baseline = measuredSizeAndBaselines.firstBaselines.y
                    val height = measuredSizeAndBaselines.size.height

                    child.baseline = baseline.unwrapOr(height) + child.margin.top
                }
            }
        }

        private fun calculateCrossSize(
            tree: LayoutTree, flexLines: List<FlexLine>, nodeSize: Size<Option<Float>>, constants: AlgoConstants
        ) {
            // Note: AlignContent::SpaceEvenly and AlignContent::SpaceAround behave like AlignContent::Stretch when there is only
            // a single flex line in the container. See: https://www.w3.org/TR/css-flexbox-1/#align-content-property
            if (flexLines.size == 1 && nodeSize.cross(constants.dir).isSome() &&
                constants.alignContent.matches(
                    AlignContent.STRETCH,
                    AlignContent.SPACE_EVENLY,
                    AlignContent.SPACE_AROUND
                )
            ) {
                flexLines[0].crossSize = (nodeSize.cross(constants.dir)
                    .maybeSub(constants.paddingBorder.crossAxisSum(constants.dir))).unwrapOr(0.0f)
            } else {
                for (line in flexLines) {
                    //    1. Collect all the flex items whose inline-axis is parallel to the main-axis, whose
                    //       align-self is baseline, and whose cross-axis margins are both non-auto. Find the
                    //       largest of the distances between each item’s baseline and its hypothetical outer
                    //       cross-start edge, and the largest of the distances between each item’s baseline
                    //       and its hypothetical outer cross-end edge, and sum these two values.

                    //    2. Among all the items not collected by the previous step, find the largest
                    //       outer hypothetical cross size.

                    //    3. The used cross-size of the flex line is the largest of the numbers found in the
                    //       previous two steps and zero.

                    val maxBaseline = line.items.map { child -> child.baseline }.fold(0.0f) { acc, x -> acc.max(x) }
                    line.crossSize = line.items.map { child ->
                        val childStyle = tree.style(child.node)
                        if (
                            child.alignSelf == AlignSelf.BASELINE &&
                            childStyle.margin.crossStart(constants.dir) != LengthPercentageAuto.Auto &&
                            childStyle.margin.crossEnd(constants.dir) != LengthPercentageAuto.Auto
                        ) {
                            maxBaseline - child.baseline + child.hypotheticalOuterSize.cross(constants.dir)
                        } else {
                            child.hypotheticalOuterSize.cross(constants.dir)
                        }
                    }.fold(0.0f) { acc, x -> acc.max(x) }
                }
            }
        }

        private fun handleAlignContentStretch(
            flexLines: List<FlexLine>,
            nodeSize: Size<Option<Float>>,
            constants: AlgoConstants
        ) {
            if (constants.alignContent == AlignContent.STRETCH && nodeSize.cross(constants.dir).isSome()) {
                val totalCrossAxisGap = sumAxisGaps(constants.gap.cross(constants.dir), flexLines.size)
                val totalCross = flexLines.map { line -> line.crossSize }.sum() + totalCrossAxisGap
                val innerCross = (nodeSize.cross(constants.dir)
                    .maybeSub(constants.paddingBorder.crossAxisSum(constants.dir))).unwrapOr(0.0f)

                if (totalCross < innerCross) {
                    val remaining = innerCross - totalCross
                    val addition = remaining / flexLines.size.toFloat()
                    flexLines.forEach { line -> line.crossSize += addition }
                }
            }
        }

        private fun determineUsedCrossSize(tree: LayoutTree, flexLines: List<FlexLine>, constants: AlgoConstants) {
            for (line in flexLines) {
                val lineCrossSize = line.crossSize

                for (child in line.items) {
                    val childStyle = tree.style(child.node)
                    child.targetSize.setCross(
                        constants.dir,
                        if (child.alignSelf == AlignSelf.STRETCH && childStyle.margin.crossStart(constants.dir) != LengthPercentageAuto.Auto && childStyle.margin.crossEnd(
                                constants.dir
                            ) != LengthPercentageAuto.Auto && childStyle.size.cross(constants.dir) == Dimension.Auto
                        ) {
                            // For some reason this particular usage of max_width is an exception to the rule that max_width's transfer
                            // using the aspect_ratio (if set). Both Chrome and Firefox agree on this. And reading the spec, it seems like
                            // a reasonable interpretation. Although it seems to me that the spec *should* apply aspectRatio here.
                            val maxSizeIgnoringAspectRatio = childStyle.maxSize.maybeResolveStS(constants.nodeInnerSize)
                            (lineCrossSize - child.margin.crossAxisSum(constants.dir))
                                .maybeClamp(
                                    child.minSize.cross(constants.dir),
                                    maxSizeIgnoringAspectRatio.cross(constants.dir)
                                )
                        } else {
                            child.hypotheticalInnerSize.cross(constants.dir)
                        },
                    )

                    child.outerTargetSize.setCross(
                        constants.dir,
                        child.targetSize.cross(constants.dir) + child.margin.crossAxisSum(constants.dir),
                    )
                }
            }
        }

        private fun distributeRemainingFreeSpace(
            tree: LayoutTree,
            flexLines: List<FlexLine>,
            node: Node,
            constants: AlgoConstants,
        ) {
            for (line in flexLines) {
                val totalMainAxisGap = sumAxisGaps(constants.gap.main(constants.dir), line.items.size)
                val usedSpace =
                    totalMainAxisGap + line.items.map { child -> child.outerTargetSize.main(constants.dir) }.sum()
                val freeSpace = constants.innerContainerSize.main(constants.dir) - usedSpace
                var numAutoMargins = 0

                for (child in line.items) {
                    val childStyle = tree.style(child.node)
                    if (childStyle.margin.mainStart(constants.dir) == LengthPercentageAuto.Auto) {
                        numAutoMargins += 1
                    }
                    if (childStyle.margin.mainEnd(constants.dir) == LengthPercentageAuto.Auto) {
                        numAutoMargins += 1
                    }
                }

                if (freeSpace > 0.0f && numAutoMargins > 0) {
                    val margin = freeSpace / numAutoMargins.toFloat()

                    for (child in line.items) {
                        val childStyle = tree.style(child.node)
                        if (childStyle.margin.mainStart(constants.dir) == LengthPercentageAuto.Auto) {
                            if (constants.isRow) {
                                child.margin.left = margin
                            } else {
                                child.margin.top = margin
                            }
                        }
                        if (childStyle.margin.mainEnd(constants.dir) == LengthPercentageAuto.Auto) {
                            if (constants.isRow) {
                                child.margin.right = margin
                            } else {
                                child.margin.bottom = margin
                            }
                        }
                    }
                } else {
                    val numItems = line.items.size
                    val layoutReverse = constants.dir.isReverse()
                    val gap = constants.gap.main(constants.dir)
                    val justifyContentMode: JustifyContent =
                        tree.style(node).justifyContent.unwrapOr(JustifyContent.FLEX_START)

                    val justifyItem = { iv: IndexedValue<FlexItem> ->
                        val i = iv.index
                        val child = iv.value
                        child.offsetMain = Alignment.computeAlignmentOffset(
                            freeSpace, numItems, gap, justifyContentMode, layoutReverse, i == 0
                        )
                    }

                    if (layoutReverse) {
                        line.items.reversed().withIndex().forEach(justifyItem)
                    } else {
                        line.items.withIndex().forEach(justifyItem)
                    }
                }
            }
        }

        fun resolveCrossAxisAutoMargins(tree: LayoutTree, flexLines: List<FlexLine>, constants: AlgoConstants) {
            for (line in flexLines) {
                val lineCrossSize = line.crossSize
                val maxBaseline = line.items.map { child -> child.baseline }.fold(0.0f) { acc, x -> acc.max(x) }

                for (child in line.items) {
                    val freeSpace = lineCrossSize - child.outerTargetSize.cross(constants.dir)
                    val childStyle = tree.style(child.node)

                    if (childStyle.margin.crossStart(constants.dir) == LengthPercentageAuto.Auto && childStyle.margin.crossEnd(
                            constants.dir
                        ) == LengthPercentageAuto.Auto
                    ) {
                        if (constants.isRow) {
                            child.margin.top = freeSpace / 2.0f
                            child.margin.bottom = freeSpace / 2.0f
                        } else {
                            child.margin.left = freeSpace / 2.0f
                            child.margin.right = freeSpace / 2.0f
                        }
                    } else if (childStyle.margin.crossStart(constants.dir) == LengthPercentageAuto.Auto) {
                        if (constants.isRow) {
                            child.margin.top = freeSpace
                        } else {
                            child.margin.left = freeSpace
                        }
                    } else if (childStyle.margin.crossEnd(constants.dir) == LengthPercentageAuto.Auto) {
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

        private fun alignFlexItemsAlongCrossAxis(
            child: FlexItem,
            freeSpace: Float,
            maxBaseline: Float,
            constants: AlgoConstants,
        ): Float {
            return when (child.alignSelf) {
                AlignSelf.START -> {
                    0f
                }

                AlignSelf.FLEX_START -> {
                    if (constants.isWrapReverse) {
                        freeSpace
                    } else {
                        0.0f
                    }
                }

                AlignSelf.END -> freeSpace
                AlignSelf.FLEX_END -> {
                    if (constants.isWrapReverse) {
                        0.0f
                    } else {
                        freeSpace
                    }
                }

                AlignSelf.CENTER -> freeSpace / 2.0f
                AlignSelf.BASELINE -> {
                    if (constants.isRow) {
                        maxBaseline - child.baseline
                    } else {
                        // Until we support vertical writing modes, baseline alignment only makes sense if
                        // the constants.direction is row, so we treat it as flex-start alignment in columns.
                        if (constants.isWrapReverse) {
                            freeSpace
                        } else {
                            0.0f
                        }
                    }
                }

                AlignSelf.STRETCH -> {
                    if (constants.isWrapReverse) {
                        freeSpace
                    } else {
                        0.0f
                    }
                }
            }
        }

        private fun determineContainerCrossSize(
            flexLines: List<FlexLine>, nodeSize: Size<Option<Float>>, constants: AlgoConstants
        ): Float {
            val totalCrossAxisGap = sumAxisGaps(constants.gap.cross(constants.dir), flexLines.size)
            val totalLineCrossSize = flexLines.map { line -> line.crossSize }.sum()

            constants.containerSize.setCross(
                constants.dir, nodeSize.cross(constants.dir).unwrapOr(
                    totalLineCrossSize + totalCrossAxisGap + constants.paddingBorder.crossAxisSum(constants.dir),
                )
            )

            constants.innerContainerSize.setCross(
                constants.dir,
                constants.containerSize.cross(constants.dir) - constants.paddingBorder.crossAxisSum(constants.dir)
            )

            return totalLineCrossSize
        }

        private fun alignFlexLinesPerAlignContent(
            tree: LayoutTree,
            flexLines: List<FlexLine>,
            node: Node,
            constants: AlgoConstants,
            totalCrossSize: Float,
        ) {
            val numLines = flexLines.size
            val gap = constants.gap.cross(constants.dir)
            val alignContentMode = tree.style(node).alignContent.unwrapOr(AlignContent.STRETCH)
            val totalCrossAxisGap = sumAxisGaps(gap, numLines)
            val freeSpace = constants.innerContainerSize.cross(constants.dir) - totalCrossSize - totalCrossAxisGap

            val alignLine = { iv: IndexedValue<FlexLine> ->
                val i = iv.index
                val line = iv.value
                line.offsetCross = Alignment.computeAlignmentOffset(
                    freeSpace, numLines, gap, alignContentMode, constants.isWrapReverse, i == 0
                )
            }

            if (constants.isWrapReverse) {
                flexLines.reversed().withIndex().forEach(alignLine)
            } else {
                flexLines.withIndex().forEach(alignLine)
            }
        }

        private fun calculateFlexItem(
            tree: LayoutTree,
            node: Node,
            item: FlexItem,
            totalOffsetMain: AtomicReference<Float>,
            totalOffsetCross: Float,
            lineOffsetCross: Float,
            containerSize: Size<Float>,
            nodeInnerSize: Size<Option<Float>>,
            direction: FlexDirection
        ) {
            val preliminarySizeAndBaselines = GenericAlgorithm.performLayout(
                tree, item.node,
                item.targetSize.map { s -> s.into() },
                nodeInnerSize,
                containerSize.map { s -> s.intoAS() },
                SizingMode.CONTENT_SIZE
            )

            val preliminarySize = preliminarySizeAndBaselines.size

            val offsetMain =
                totalOffsetMain.get() + item.offsetMain + item.margin.mainStart(direction) + (item.inset.mainStart(
                    direction
                ).orElse { item.inset.mainEnd(direction).map { pos -> -pos } }.unwrapOr(0.0f))

            val offsetCross =
                totalOffsetCross + item.offsetCross + lineOffsetCross + item.margin.crossStart(direction) + (item.inset.crossStart(
                    direction
                ).orElse { item.inset.crossEnd(direction).map { pos -> -pos } }.unwrapOr(0.0f))

            if (direction.isRow()) {
                val baselineOffsetCross = totalOffsetCross + item.offsetCross + item.margin.crossStart(direction)
                val innerBaseline = preliminarySizeAndBaselines.firstBaselines.y.unwrapOr(preliminarySize.height)
                item.baseline = baselineOffsetCross + innerBaseline
            } else {
                val baselineOffsetMain = totalOffsetMain.get() + item.offsetMain + item.margin.mainStart(direction)
                val innerBaseline = preliminarySizeAndBaselines.firstBaselines.y.unwrapOr(preliminarySize.height)
                item.baseline = baselineOffsetMain + innerBaseline
            }

            val order = tree.children(node).position { n -> n == item.node }.unwrap()

            tree.layout(
                item.node, Layout(
                    order = order,
                    size = preliminarySizeAndBaselines.size,
                    location = Point(
                        x = if (direction.isRow()) {
                            offsetMain
                        } else {
                            offsetCross
                        },
                        y = if (direction.isColumn()) {
                            offsetMain
                        } else {
                            offsetCross
                        }
                    )
                )
            )

            totalOffsetMain.getAndUpdate { prev ->
                prev + item.offsetMain + item.margin.mainAxisSum(direction) +
                        preliminarySize.main(direction)
            }
        }

        private fun calculateLayoutLine(
            tree: LayoutTree,
            node: Node,
            line: FlexLine,
            totalOffsetCross: AtomicReference<Float>,
            containerSize: Size<Float>,
            nodeInnerSize: Size<Option<Float>>,
            paddingBorder: Rect<Float>,
            direction: FlexDirection,
        ) {
            val totalOffsetMain = AtomicReference(paddingBorder.mainStart(direction))
            val lineOffsetCross = line.offsetCross

            if (direction.isReverse()) {
                for (item in line.items.reversed()) {
                    calculateFlexItem(
                        tree,
                        node,
                        item,
                        totalOffsetMain,
                        totalOffsetCross.get(),
                        lineOffsetCross,
                        containerSize,
                        nodeInnerSize,
                        direction
                    )
                }
            } else {
                for (item in line.items) {
                    calculateFlexItem(
                        tree,
                        node,
                        item,
                        totalOffsetMain,
                        totalOffsetCross.get(),
                        lineOffsetCross,
                        containerSize,
                        nodeInnerSize,
                        direction
                    )
                }
            }

            totalOffsetCross.getAndUpdate { prev ->
                prev + lineOffsetCross + line.crossSize
            }
        }

        private fun finalLayoutPass(
            tree: LayoutTree,
            node: Node,
            flexLines: List<FlexLine>,
            constants: AlgoConstants
        ) {
            val totalOffsetCross = AtomicReference(constants.paddingBorder.crossStart(constants.dir))

            if (constants.isWrapReverse) {
                for (line in flexLines.reversed()) {
                    calculateLayoutLine(
                        tree,
                        node,
                        line,
                        totalOffsetCross,
                        constants.containerSize,
                        constants.nodeInnerSize,
                        constants.paddingBorder,
                        constants.dir
                    )
                }
            } else {
                for (line in flexLines) {
                    calculateLayoutLine(
                        tree,
                        node,
                        line,
                        totalOffsetCross,
                        constants.containerSize,
                        constants.nodeInnerSize,
                        constants.paddingBorder,
                        constants.dir
                    )
                }
            }
        }

        private fun performAbsoluteLayoutOnAbsoluteChildren(
            tree: LayoutTree,
            node: Node,
            constants: AlgoConstants
        ) {
            val containerWidth = constants.containerSize.width
            val containerHeight = constants.containerSize.height

            for (order in 0 until tree.childCount(node)) {
                val child = tree.child(node, order)
                val childStyle = tree.style(child)

                // Skip items that are display:none or are not position:absolute
                if (childStyle.display == Display.NONE || childStyle.position != Position.ABSOLUTE) {
                    continue
                }

                val aspectRatio = childStyle.aspectRatio;
                val margin = childStyle.margin.map { margin -> margin.resolveToOption(containerWidth) }
                val alignSelf = childStyle.alignSelf.unwrapOr(constants.alignItems)

                // Resolve inset
                val left = childStyle.inset.left.maybeResolve(containerWidth)
                val right = childStyle.inset.right.maybeResolve(containerWidth)
                val top = childStyle.inset.top.maybeResolve(containerHeight)
                val bottom = childStyle.inset.bottom.maybeResolve(containerHeight)

                // Compute known dimensions from min/max/inherent size styles
                val styleSize =
                    childStyle.size.maybeResolveStSd(constants.containerSize).maybeApplyAspectRatio(aspectRatio)
                val minSize =
                    childStyle.minSize.maybeResolveStSd(constants.containerSize).maybeApplyAspectRatio(aspectRatio)
                val maxSize =
                    childStyle.maxSize.maybeResolveStSd(constants.containerSize).maybeApplyAspectRatio(aspectRatio)
                var knownDimensions = styleSize.maybeClamp(minSize, maxSize)

                // Fill in width from left/right and reapply aspect ratio if:
                //   - Width is not already known
                //   - Item has both left and right inset properties set
                var tr = Triple(knownDimensions.width, left, right)
                if (tr.first.isNone() && tr.second.isSome() && tr.third.isSome()) {
                    val newWidthRaw =
                        containerWidth.maybeSub(margin.left).maybeSub(margin.right) - tr.second.unwrap() - tr.third.unwrap()
                    knownDimensions.width = Option.Some(f32Max(newWidthRaw, 0.0f));
                    knownDimensions =
                        knownDimensions.maybeApplyAspectRatio(aspectRatio).maybeClamp(minSize, maxSize)
                }

                // Fill in height from top/bottom and reapply aspect ratio if:
                //   - Height is not already known
                //   - Item has both top and bottom inset properties set
                tr = Triple(knownDimensions.height, top, bottom)
                if (tr.first.isNone() && tr.second.isSome() && tr.third.isSome()) {
                    val newHeightRaw =
                        containerHeight.maybeSub(margin.top).maybeSub(margin.bottom) - tr.second.unwrap() - tr.third.unwrap()
                    knownDimensions.height = Option.Some(f32Max(newHeightRaw, 0.0f));
                    knownDimensions =
                        knownDimensions.maybeApplyAspectRatio(aspectRatio).maybeClamp(minSize, maxSize)
                }

                val measuredSizeAndBaselines = GenericAlgorithm.performLayout(
                    tree,
                    child,
                    knownDimensions,
                    constants.nodeInnerSize,
                    Size(
                        width = AvailableSpace.Definite(containerWidth.maybeClamp(minSize.width, maxSize.width)),
                        height = AvailableSpace.Definite(containerHeight.maybeClamp(minSize.width, maxSize.width)),
                    ),
                    SizingMode.CONTENT_SIZE,
                )
                val measuredSize = measuredSizeAndBaselines.size
                val finalSize = knownDimensions.unwrapOr(measuredSize).maybeClamp(minSize, maxSize)

                val nonAutoMargin = margin.map { m -> m.unwrapOr(0.0f) }

                val freeSpace = Size(
                    width = constants.containerSize.width - finalSize.width - nonAutoMargin.horizontalAxisSum(),
                    height = constants.containerSize.height - finalSize.height - nonAutoMargin.verticalAxisSum()
                ).f32Max(Size.zeroF())

                // Expand auto margins to fill available space
                val autoMarginCountW = (if (margin.left.isNone()) 1 else 0) + (if (margin.right.isNone()) 1 else 0)
                val autoMarginCountH = (if (margin.top.isNone()) 1 else 0) + (if (margin.bottom.isNone()) 1 else 0)
                val autoMarginSize = Size(
                    width = if (autoMarginCountW > 0) {
                        freeSpace.width / autoMarginCountW.toFloat()
                    } else {
                        0.0f
                    },
                    height = if (autoMarginCountH > 0) {
                        freeSpace.height / autoMarginCountH.toFloat()
                    } else {
                        0.0f
                    }
                )
                val resolvedMargin = Rect(
                    left = margin.left.unwrapOr(autoMarginSize.width),
                    right = margin.right.unwrapOr(autoMarginSize.width),
                    top = margin.top.unwrapOr(autoMarginSize.height),
                    bottom = margin.bottom.unwrapOr(autoMarginSize.height)
                )

                // Determine flex-relative insets
                val (startMain, endMain) = if (constants.isRow) Pair(left, right) else Pair(top, bottom)
                val (startCross, endCross) = if (constants.isRow) Pair(top, bottom) else Pair(left, right)

                // Apply main-axis alignment
                // let free_main_space = free_space.main(constants.dir) - resolved_margin.main_axis_sum(constants.dir);
                val offsetMain: Float = if (startMain.isSome()) {
                    startMain.unwrap() + constants.border.mainStart(constants.dir) + resolvedMargin.mainStart(constants.dir)
                } else if (endMain.isSome()) {
                    constants.containerSize.main(constants.dir) - constants.border.mainEnd(constants.dir) -
                            finalSize.main(constants.dir) - endMain.unwrap() - resolvedMargin.mainEnd(constants.dir)
                } else {
                    // Stretch is an invalid value for justify_content in the flexbox algorithm, so we
                    // treat it as if it wasn't set (and thus we default to FlexStart behaviour)
                    val a = tree.style(node).justifyContent.unwrapOr(JustifyContent.START)
                    val b = constants.isWrapReverse
                    when {
                        // Stretch is an invalid value for justify_content in the flexbox algorithm, so we
                        // treat it as if it wasn't set (and thus we default to FlexStart behaviour)
                        (a == JustifyContent.SPACE_BETWEEN || a == JustifyContent.START ||
                                (a == JustifyContent.STRETCH && !b) || (a == JustifyContent.FLEX_START && !b) ||
                                (a == JustifyContent.FLEX_END && b)) -> {
                            constants.paddingBorder.mainStart(constants.dir) + resolvedMargin.mainStart(constants.dir)
                        }

                        (a == JustifyContent.END || (a == JustifyContent.FLEX_END) ||
                                (a == JustifyContent.FLEX_START) || (a == JustifyContent.STRETCH)) -> {
                            constants.containerSize.main(constants.dir) -
                                    constants.paddingBorder.mainEnd(constants.dir) -
                                    finalSize.main(constants.dir) - resolvedMargin.mainEnd(constants.dir)
                        }

                        (a == JustifyContent.SPACE_EVENLY || a == JustifyContent.SPACE_AROUND || a == JustifyContent.CENTER) -> {
                            (constants.containerSize.main(constants.dir) +
                                    constants.paddingBorder.mainStart(constants.dir) -
                                    constants.paddingBorder.mainEnd(constants.dir) -
                                    finalSize.main(constants.dir) + resolvedMargin.mainStart(constants.dir) -
                                    resolvedMargin.mainEnd(constants.dir)) / 2.0f
                        }

                        else -> throw UnsupportedOperationException("Shouldn't happen")
                    }
                }

                // Apply cross-axis alignment
                // let free_cross_space = free_space.cross(constants.dir) - resolved_margin.cross_axis_sum(constants.dir);
                val offsetCross = if (startCross.isSome()) {
                    startCross.unwrap() + constants.border.crossStart(constants.dir) + resolvedMargin.crossStart(constants.dir)
                } else if (endCross.isSome()) {
                    constants.containerSize.cross(constants.dir) - constants.border.crossEnd(constants.dir) -
                            finalSize.cross(constants.dir) - endCross.unwrap() - resolvedMargin.crossEnd(constants.dir)
                } else {
                    val a = alignSelf
                    val b = constants.isWrapReverse
                    when {
                        // Stretch alignment does not apply to absolutely positioned items
                        // See "Example 3" at https://www.w3.org/TR/css-flexbox-1/#abspos-items
                        // Note: Stretch should be FlexStart not Start when we support both
                        (a == AlignSelf.START ||
                                ((a == AlignSelf.BASELINE || a == AlignSelf.STRETCH || a == AlignSelf.FLEX_START) && !b) ||
                                (a == AlignItems.FLEX_END && b)
                                ) -> {
                            constants.paddingBorder.crossStart(constants.dir) + resolvedMargin
                                .crossStart(constants.dir)
                        }

                        (a == AlignSelf.END || a == AlignSelf.BASELINE || a == AlignSelf.STRETCH ||
                                a == AlignSelf.FLEX_START || a == AlignItems.FLEX_END) -> {
                            constants.containerSize.cross(constants.dir) -
                                    constants.paddingBorder.crossEnd(constants.dir) - finalSize.cross(constants.dir) -
                                    resolvedMargin.crossEnd(constants.dir)
                        }

                        (a == AlignSelf.CENTER) -> {
                            (constants.containerSize.cross(constants.dir) +
                                    constants.paddingBorder.crossStart(constants.dir) -
                                    constants.paddingBorder.crossEnd(constants.dir) - finalSize.cross(constants.dir) +
                                    resolvedMargin.crossStart(constants.dir) -
                                    resolvedMargin.crossEnd(constants.dir)) / 2.0f
                        }

                        else -> throw UnsupportedOperationException("Unsupported!")
                    }
                }

                tree.layout(
                    child, Layout(
                        order = order,
                        size = finalSize,
                        location = Point(
                            x = if (constants.isRow) {
                                offsetMain
                            } else {
                                offsetCross
                            },
                            y = if (constants.isColumn) {
                                offsetMain
                            } else {
                                offsetCross
                            },
                        ),
                    )
                )
            }
        }

        private fun sumAxisGaps(gap: Float, numItems: Int): Float {
            // Gaps only exist between items, so...
            return if (numItems <= 1) {
                // ...if there are less than 2 items then there are no gaps
                0.0f
            } else {
                // ...otherwise there are (num_items - 1) gaps
                gap * (numItems - 1).toFloat()
            }
        }
    }
}
