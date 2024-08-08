package be.arby.taffy.layout

import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.node.Node
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.layout.Layout

interface LayoutTree {
    fun isChildless(node: Node): Boolean
    fun style(node: Node): Style
    fun layout(root: Node, layout: Layout)
    fun cache(node: Node, idx: Int): Option<Cache>
    fun cache(node: Node, idx: Int, cache: Cache?)
    fun childCount(node: Node): Int
    fun child(node: Node, order: Int): Node
    fun layout(root: Node): Layout
    fun measureNode(node: Node, knownDimensions: Size<Option<Float>>, availableSpace: Size<AvailableSpace>): Size<Float>
    fun needsMeasure(node: Node): Boolean
    fun children(node: Node): List<Node>
}
