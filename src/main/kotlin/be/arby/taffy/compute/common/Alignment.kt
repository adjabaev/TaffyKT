package be.arby.taffy.compute.common

import be.arby.taffy.style.alignment.AlignContent

class Alignment {
    companion object {
        fun computeAlignmentOffset(
            freeSpace: Float,
            numItems: Int,
            gap: Float,
            alignmentMode: AlignContent,
            layoutIsFlexReversed: Boolean,
            isFirst: Boolean,
        ): Float {
            if (isFirst) {
                return when (alignmentMode) {
                    AlignContent.START -> 0f
                    AlignContent.FLEX_START -> if (layoutIsFlexReversed) {
                        freeSpace
                    } else {
                        0f
                    }
                    AlignContent.END -> freeSpace
                    AlignContent.FLEX_END -> if (layoutIsFlexReversed) {
                        0f
                    } else {
                        freeSpace
                    }
                    AlignContent.CENTER -> freeSpace / 2f
                    AlignContent.STRETCH -> 0f
                    AlignContent.SPACE_BETWEEN -> 0f
                    AlignContent.SPACE_AROUND -> (freeSpace / numItems.toFloat()) / 2f
                    AlignContent.SPACE_EVENLY -> freeSpace / (numItems + 1).toFloat()
                }
            } else {
                return gap + when (alignmentMode) {
                    AlignContent.START -> 0.0f
                    AlignContent.FLEX_START -> 0.0f
                    AlignContent.END -> 0.0f
                    AlignContent.FLEX_END -> 0.0f
                    AlignContent.CENTER -> 0.0f
                    AlignContent.STRETCH -> 0.0f
                    AlignContent.SPACE_BETWEEN -> freeSpace / (numItems - 1).toFloat()
                    AlignContent.SPACE_AROUND -> freeSpace / numItems.toFloat()
                    AlignContent.SPACE_EVENLY -> freeSpace / (numItems + 1).toFloat()
                }
            }
        }
    }
}
