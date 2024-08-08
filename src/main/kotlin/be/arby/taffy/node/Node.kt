package be.arby.taffy.node

class Node(
    @JvmField var data: NodeData,
    @JvmField var measureFunc: MeasureFunc?,
    @JvmField var children: List<Node>,
    @JvmField var parent: Node?,
    @JvmField var debug: String?
) {
    companion object {
        @JvmStatic
        fun make(data: NodeData): Node {
            return Node(data = data, measureFunc = null, children = ArrayList(), parent = null, debug = "Default")
        }
    }
}
