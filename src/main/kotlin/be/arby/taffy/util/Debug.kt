package be.arby.taffy.util

import be.arby.taffy.lang.collections.enumerate
import be.arby.taffy.tree.traits.PrintTree

class Debug {
    companion object {
        fun printTree(tree: PrintTree, root: Int) {
            println("TREE")
            printNode(tree, root, false, "")
        }

        private fun printNode(tree: PrintTree, nodeId: Int, hasSibling: Boolean, linesString: String) {
            val layout = tree.getFinalLayout(nodeId)
            val display = tree.getDebugLabel(nodeId)
            val numChildren = tree.childCount(nodeId)

            val forkString = if (hasSibling) { "├── " } else { "└── " }

            val lines = linesString
            val fork = forkString
            val x = layout.location.x
            val y = layout.location.y
            val width = layout.size.width
            val height = layout.size.height
            val contentWidth = layout.contentSize.width
            val contentHeight = layout.contentSize.height
            val bl = layout.border.left
            val br = layout.border.right
            val bt = layout.border.top
            val bb = layout.border.bottom
            val pl = layout.padding.left
            val pr = layout.padding.right
            val pt = layout.padding.top
            val pb = layout.padding.bottom
            val key = nodeId

            println("$lines $fork $display [x: ${x.toString().padEnd(4)} y: ${y.toString().padEnd(4)} w: ${width.toString().padEnd(4)} h: ${height.toString().padEnd(4)} content_w: ${contentWidth.toString().padEnd(4)} content_h: ${contentHeight.toString().padEnd(4)} border: l:$bl r:$br t:$bt b:$bb, padding: l:$pl r:$pr t:$pt b:$pb] ($key)")
            val bar = if (hasSibling) { "│   " } else { "    " }
            val newString = linesString + bar

            // Recurse into children
            for ((index, child) in tree.childIds(nodeId).enumerate()) {
                val hasSibling = index < numChildren - 1
                printNode(tree, child, hasSibling, newString)
            }
        }
    }
}
