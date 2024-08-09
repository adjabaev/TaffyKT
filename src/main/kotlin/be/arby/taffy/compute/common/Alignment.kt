package be.arby.taffy.compute.common

import be.arby.taffy.lang.max
import be.arby.taffy.style.alignment.AlignContent

/**
 * Implement fallback alignment.
 *
 * In addition to the spec at https://www.w3.org/TR/css-align-3/ this implementation follows
 * the resolution of https://github.com/w3c/csswg-drafts/issues/10154
 */
fun applyAlignmentFallback(
    freeSpace: Float,
    numItems: Int,
    alignmentMode: AlignContent,
    isSafe: Boolean,
): AlignContent {
    var pair = Pair(alignmentMode, isSafe)
    // Fallback occurs in two cases:

    // 1. If there is only a single item being aligned and alignment is a distributed alignment keyword
    //    https://www.w3.org/TR/css-align-3/#distribution-values
    if (numItems <= 1f || freeSpace <= 0f) {
        pair = when (alignmentMode) {
            AlignContent.STRETCH -> Pair(AlignContent.FLEX_START, true)
            AlignContent.SPACE_BETWEEN -> Pair(AlignContent.FLEX_START, true)
            AlignContent.SPACE_AROUND -> Pair(AlignContent.CENTER, true)
            AlignContent.SPACE_EVENLY -> Pair(AlignContent.CENTER, true)
            else -> Pair(alignmentMode, isSafe)
        }
    }

    // 2. If free space is negative the "safe" alignment variants all fallback to Start alignment
    if (freeSpace <= 0f && pair.second) {
        return AlignContent.START
    }

    return pair.first
}

/**
 * Generic alignment function that is used:
 *   - For both align-content and justify-content alignment
 *   - For both the Flexbox and CSS Grid algorithms
 *
 * CSS Grid does not apply gaps as part of alignment, so the gap parameter should
 * always be set to zero for CSS Grid.
 */
fun computeAlignmentOffset(
    freeSpace: Float,
    numItems: Int,
    gap: Float,
    alignmentMode: AlignContent,
    layoutIsFlexReversed: Boolean,
    isFirst: Boolean
): Float {
    return if (isFirst) {
        when (alignmentMode) {
            AlignContent.START -> 0f
            AlignContent.FLEX_START -> run {
                if (layoutIsFlexReversed) {
                    freeSpace
                } else {
                    0f
                }
            }

            AlignContent.END -> freeSpace
            AlignContent.FLEX_END -> {
                if (layoutIsFlexReversed) 0f else freeSpace
            }

            AlignContent.CENTER -> freeSpace / 2f
            AlignContent.STRETCH -> 0f
            AlignContent.SPACE_BETWEEN -> 0f
            AlignContent.SPACE_AROUND -> {
                if (freeSpace >= 0f) (freeSpace / numItems.toFloat()) / 2f else freeSpace / 2f
            }

            AlignContent.SPACE_EVENLY -> {
                if (freeSpace >= 0f) freeSpace / (numItems + 1f) else freeSpace / 2f
            }
        }
    } else {
        val freeSpace = freeSpace.max(0f)
        gap + when (alignmentMode) {
            AlignContent.START -> 0f
            AlignContent.FLEX_START -> 0f
            AlignContent.END -> 0f
            AlignContent.FLEX_END -> 0f
            AlignContent.CENTER -> 0f
            AlignContent.STRETCH -> 0f
            AlignContent.SPACE_BETWEEN -> freeSpace / (numItems - 1f)
            AlignContent.SPACE_AROUND -> freeSpace / numItems.toFloat()
            AlignContent.SPACE_EVENLY -> freeSpace / (numItems + 1f)
        }
    }
}
