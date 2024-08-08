package be.arby.taffy.node

import be.arby.taffy.lang.Option
import be.arby.taffy.layout.Cache
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.style.Style

class NodeData(var style: Style, var layout: Layout, var needsMeasure: Boolean, var sizeCache: Array<Option<Cache>>) {
    fun markDirty() {
        sizeCache = Array(7) { Option.None }
    }

    companion object {
        @JvmStatic
        fun make(style: Style): NodeData {
            return NodeData(
                style = style,
                sizeCache = Array(7) { Option.None },
                layout = Layout.new(),
                needsMeasure = false
            )
        }
    }
}
