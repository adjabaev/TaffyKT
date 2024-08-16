package be.arby.taffy.compute.common

import be.arby.taffy.geom.Point
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.f32Max
import be.arby.taffy.style.Overflow

/**
 * Determine how much width/height a given node contributes to it's parent's content size
 */
fun computeContentSizeContribution(
location: Point<Float>,
size: Size<Float>,
contentSize: Size<Float>,
overflow: Point<Overflow>
): Size<Float> {
    val sizeContentSizeContribution = Size(
        width = if (overflow.x == Overflow.VISIBLE) f32Max(size.width, contentSize.width) else size.width,
        height = if (overflow.y == Overflow.VISIBLE) f32Max(size.height, contentSize.height) else size.height
    )
    return if (sizeContentSizeContribution.width > 0f && sizeContentSizeContribution.height > 0f) {
        Size(
            width = location.x + sizeContentSizeContribution.width,
            height = location.y + sizeContentSizeContribution.height,
        )
    } else {
        Size.ZERO.clone()
    }
}
