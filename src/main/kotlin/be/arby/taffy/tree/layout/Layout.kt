package be.arby.taffy.tree.layout

import be.arby.taffy.geom.Point
import be.arby.taffy.geom.Rect
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Default
import be.arby.taffy.lang.f32Max
import be.arby.taffy.lang.f32Min

/**
 * The final result of a layout algorithm for a single node.
 */
data class Layout(
    /**
     * The relative ordering of the node
     *
     * Nodes with a higher order should be rendered on top of those with a lower order.
     * This is effectively a topological sort of each tree.
     */
    var order: Int,
    /**
     * The top-left corner of the node
     */
    var location: Point<Float>,
    /**
     * The width and height of the node
     */
    var size: Size<Float>,
    /**
     * The width and height of the content inside the node. This may be larger than the size of the node in the case of
     * overflowing content and is useful for computing a "scroll width/height" for scrollable nodes
     */
    var contentSize: Size<Float>,
    /**
     * The size of the scrollbars in each dimension. If there is no scrollbar then the size will be zero.
     */
    var scrollbarSize: Size<Float>,
    /**
     * The size of the borders of the node
     */
    var border: Rect<Float>,
    /**
     * The size of the padding of the node
     */
    var padding: Rect<Float>,
    /**
     * The size of the margin of the node
     */
    var margin: Rect<Float>
): Cloneable {
    /**
     * Return the scroll width of the node.
     * The scroll width is the difference between the width and the content width, floored at zero
     */
    fun scrollWidth(): Float {
        return f32Max(
            0.0f,
            contentSize.width + f32Min(scrollbarSize.width, size.width) - size.width + border.right
        )
    }

    /**
     * Return the scroll width of the node.
     * The scroll width is the difference between the width and the content width, floored at zero
     */
    fun scrollHeight(): Float {
        return f32Max(
            0.0f,
            contentSize.height + f32Min(scrollbarSize.height, size.height) - size.height + border.bottom
        )
    }

    public override fun clone(): Layout {
        return Layout(
            order = order,
            location = location.clone(),
            size = size.clone(),
            contentSize = contentSize.clone(),
            scrollbarSize = scrollbarSize.clone(),
            border = border.clone(),
            padding = padding.clone(),
            margin = margin.clone()
        )
    }

    companion object: Default<Layout> {
        override fun default(): Layout {
            return new()
        }

        /**
         * Creates a new zero-[`Layout`].
         *
         * The Zero-layout has size and location set to ZERO.
         * The `order` value of this layout is set to the minimum value of 0.
         * This means it should be rendered below all other [`Layout`]s.
         */
        fun new(): Layout {
            return Layout(
                order = 0,
                location = Point.ZERO.clone(),
                size = Size.ZERO.clone(),
                contentSize = Size.ZERO.clone(),
                scrollbarSize = Size.ZERO.clone(),
                border = Rect.ZERO.clone(),
                padding = Rect.ZERO.clone(),
                margin = Rect.ZERO.clone()
            )
        }

        /**
         * Creates a new zero-[`Layout`] with the supplied `order` value.
         *
         * Nodes with a higher order should be rendered on top of those with a lower order.
         * The Zero-layout has size and location set to ZERO.
         */
        fun withOrder(order: Int): Layout {
            return Layout(
                order = order,
                location = Point.ZERO.clone(),
                size = Size.ZERO.clone(),
                contentSize = Size.ZERO.clone(),
                scrollbarSize = Size.ZERO.clone(),
                border = Rect.ZERO.clone(),
                padding = Rect.ZERO.clone(),
                margin = Rect.ZERO.clone()
            )
        }
    }
}
