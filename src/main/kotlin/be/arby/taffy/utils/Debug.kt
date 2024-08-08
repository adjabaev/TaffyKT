package be.arby.taffy.utils

import be.arby.taffy.layout.LayoutTree
import be.arby.taffy.node.Node
import be.arby.taffy.style.Display

class Debug {

    companion object {
        fun printTree(tree: LayoutTree, root: Node) {
            println("TREE")
            printNode(tree, root, false, "")
        }

        fun printNode(tree: LayoutTree, node: Node, hasSibling: Boolean, linesString: String) {
            val layout = tree.layout(node)
            val style = tree.style(node)

            val numChildren = tree.childCount(node)

            val display = when {
                style.display == Display.NONE -> "NONE"
                numChildren == 0 -> "LEAF"
                style.display == Display.FLEX -> "FLEX"
                style.display == Display.GRID -> "GRID"
                else -> "???"
            }

            val forkString = if (hasSibling) { "├── " } else { "└── " };
            println(
            "$linesString$forkString $display [x: ${layout.location.x}f y: ${layout.location.y}f " +
                    "width: ${layout.size.width}f height: ${layout.size.height}f]")
            val bar = if (hasSibling) { "│   " } else { "    " }
            val newString = linesString + bar

            // Recurse into children
            for (p in tree.children(node).withIndex()) {
                val (index, child) = p
                val hasSibling = index < numChildren - 1;
                printNode(tree, child, hasSibling, newString)
            }
        }
    }
}
