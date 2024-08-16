package be.arby.taffy.tree.node

import be.arby.taffy.compute.computeRootLayout
import be.arby.taffy.compute.roundLayout
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.Result
import be.arby.taffy.lang.collections.RustMap
import be.arby.taffy.lang.collections.len
import be.arby.taffy.style.Display
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.layout.Layout
import be.arby.taffy.tree.traits.LayoutPartialTree
import be.arby.taffy.tree.traits.PrintTree
import be.arby.taffy.tree.traits.TraversePartialTree
import be.arby.taffy.tree.traits.TraverseTree
import be.arby.taffy.util.Debug
import be.arby.taffy.vec

typealias TaffyResult = Result<Int>
typealias AltMap<T> = MutableMap<Int, T>

/**
 * An entire tree of UI nodes. The entry point to Taffy's high-level API.
 *
 * Allows you to build a tree of UI nodes, run Taffy's layout algorithms over that tree, and then access the resultant layout.]
 */
data class TaffyTree<NodeContext>(
    /**
     * The [`NodeData`] for each node stored in this tree
     */
    val nodes: RustMap<NodeData>,
    /**
     * Functions/closures that compute the intrinsic size of leaf nodes
     */
    val nodeContextData: AltMap<NodeContext>,
    /**
     * The children of each node
     *
     * The indexes in the outer vector correspond to the position of the parent [`NodeData`]
     */
    val children: AltMap<MutableList<Int>>,
    /**
     * The parents of each node
     *
     * The indexes in the outer vector correspond to the position of the child [`NodeData`]
     */
    val parents: AltMap<Option<Int>>,
    /**
     * Layout mode configuration
     */
    val config: TaffyConfig
) : TraversePartialTree, TraverseTree, PrintTree {
    override fun getDebugLabel(nodeId: Int): String {
        val node = nodes[nodeId]!!
        val display = node.style.display
        val numChildren = childCount(nodeId)

        return if (display == Display.NONE) {
            "NONE"
        } else if (numChildren == 0) {
            "LEAF"
        } else if (display == Display.BLOCK) {
            "BLOCK"
        } else if (display == Display.FLEX) {
            if (node.style.flexDirection.isColumn()) {
                "FLEX COL"
            } else {
                "FLEX ROW"
            }
        } else {
            "GRID"
        }
    }

    override fun getFinalLayout(nodeId: Int): Layout {
        return if (config.useRounding) {
            nodes[nodeId]!!.finalLayout
        } else {
            nodes[nodeId]!!.unroundedLayout
        }
    }

    override fun childIds(parentNodeId: Int): MutableList<Int> {
        return children[parentNodeId] ?: throw TaffyError.InvalidParentNode(parentNodeId)
    }

    override fun childCount(parentNodeId: Int): Int {
        return children[parentNodeId]?.len() ?: throw TaffyError.InvalidParentNode(parentNodeId)
    }

    override fun getChildId(parentNodeId: Int, childIndex: Int): Int {
        return children[parentNodeId]?.get(childIndex) ?: throw TaffyError.InvalidParentNode(parentNodeId)
    }

    /**
     * Enable rounding of layout values. Rounding is enabled by default.
     */
    fun enableRounding() {
        config.useRounding = true
    }

    /**
     * Disable rounding of layout values. Rounding is enabled by default.
     */
    fun disableRounding() {
        config.useRounding = false
    }

    /**
     * Creates and adds a new unattached leaf node to the tree, and returns the node of the new node
     */
    fun newLeaf(layout: Style): TaffyResult {
        val id = nodes.insert(NodeData.new(layout))
        children[id] = vec()
        parents[id] = Option.None

        return Result.Ok(id)
    }

    /**
     * Creates and adds a new unattached leaf node to the tree, and returns the [`NodeId`] of the new node
     *
     * Creates and adds a new leaf node with a supplied context
     */
    fun newLeafWithContext(layout: Style, context: NodeContext): TaffyResult {
        val data = NodeData.new(layout)
        data.hasContext = true

        val id = nodes.insert(data)
        nodeContextData[id] = context
        children[id] = vec()
        parents[id] = Option.None

        return Result.Ok(id)
    }

    /**
     * Creates and adds a new node, which may have any number of `children`
     */
    fun newWithChildren(layout: Style, children: List<Int>): TaffyResult {
        val id = nodes.insert(NodeData.new(layout))

        for (child in children) {
            parents[child] = Option.Some(id)
        }

        this.children[id] = children.toMutableList()
        parents[id] = Option.None

        return Result.Ok(id)
    }

    /**
     * Drops all nodes in the tree
     */
    fun clear() {
        nodes.clear()
        children.clear()
        parents.clear()
    }

    /**
     * Remove a specific node from the tree and drop it
     *
     * Returns the id of the node removed.
     */
    fun remove(node: Int): TaffyResult {
        val cp = parents[node]
        if (cp != null && cp.isSome()) {
            val parent = cp.unwrap()
            val cc = children[parent]
            if (cc != null) {
                cc.retainAll { f -> f != node }
            }
        }

        // Remove "parent" references to a node when removing that node
        val cc = children[node]
        if (cc != null) {
            for (child in cc) {
                parents[child] = Option.None
            }
        }

        children.remove(node)
        parents.remove(node)
        nodes.remove(node)

        return Result.Ok(node)
    }

    /**
     * Sets the context data associated with the node
     */
    fun setNodeContext(node: Int, measure: Option<NodeContext>): TaffyResult {
        if (measure.isSome()) {
            nodes[node]?.hasContext = true
            nodeContextData[node] = measure.unwrap()
        } else {
            nodes[node]?.hasContext = false
            nodeContextData.remove(node)
        }

        markDirty(node)

        return Result.Ok(-1)
    }

    /**
     * Gets a reference to the the context data associated with the node
     */
    fun getNodeContext(node: Int): Option<NodeContext> {
        return Option.from(nodeContextData[node])
    }

    /**
     * Adds a `child` node under the supplied `parent`
     */
    fun addChild(parent: Int, child: Int): Result<Boolean> {
        parents[child] = Option.Some(parent)
        children[parent]?.add(child)
        markDirty(parent)

        return Result.Ok(false)
    }

    /**
     * Inserts a `child` node at the given `child_index` under the supplied `parent`, shifting all children after it to the right.
     */
    fun insertChildAtIndex(parent: Int, childIndex: Int, child: Int): TaffyResult {
        val childCount = children[parent]?.len() ?: return Result.Err(TaffyError.InvalidParentNode(parent))
        if (childIndex > childCount) {
            return Result.Err(TaffyError.ChildIndexOutOfBounds(parent, childIndex, childCount))
        }

        parents[child] = Option.Some(parent)
        children[parent]?.add(childIndex, child)
        markDirty(parent)

        return Result.Ok(-1)
    }

    /**
     * Directly sets the `children` of the supplied `parent`
     */
    fun setChildren(parent: Int, children: List<Int>): TaffyResult {
        // Remove node as parent from all its current children.
        for (child in this.children[parent]!!) {
            parents[child] = Option.None
        }

        // Build up relation node <-> child
        for (child in children) {
            parents[child] = Option.Some(parent)
        }

        val parentChildren = this.children[parent] ?: return Result.Err(TaffyError.InvalidParentNode(parent))
        parentChildren.clear()
        children.forEach { child -> parentChildren.add(child) }

        markDirty(parent)

        return Result.Ok(-1)
    }

    /**
     * Removes the `child` of the parent `node`
     *
     * The child is not removed from the tree entirely, it is simply no longer attached to its previous parent.
     */
    fun removeChild(parent: Int, child: Int): Result<Int> {
        val index = children[parent]!!.indexOf(child)
        return removeChildAtIndex(parent, index)
    }

    /**
     * Removes the child at the given `index` from the `parent`
     *
     * The child is not removed from the tree entirely, it is simply no longer attached to its previous parent.
     */
    fun removeChildAtIndex(parent: Int, childIndex: Int): Result<Int> {
        val childCount = children[parent]!!.len()
        if (childIndex >= childCount) {
            return Result.Err(TaffyError.ChildIndexOutOfBounds(parent, childIndex, childCount))
        }

        children[parent]!!.removeAt(childIndex)
        parents[childIndex] = Option.None

        markDirty(parent)

        return Result.Ok(childIndex)
    }

    /**
     * Replaces the child at the given `child_index` from the `parent` node with the new `child` node
     *
     * The child is not removed from the tree entirely, it is simply no longer attached to its previous parent.
     */
    fun replaceChildAtIndex(
        parent: Int,
        childIndex: Int,
        newChild: Int,
    ): Result<Int> {
        val childCount = children[parent]!!.len()
        if (childIndex >= childCount) {
            return Result.Err(TaffyError.ChildIndexOutOfBounds(parent, childIndex, childCount))
        }

        parents[newChild] = Option.Some(parent)
        val oldChild = children[parent]?.set(childIndex, newChild)!!
        parents[oldChild] = Option.None

        markDirty(parent)

        return Result.Ok(oldChild)
    }

    /**
     * Returns the child node of the parent `node` at the provided `child_index`
     */
    fun childAtIndex(parent: Int, childIndex: Int): Result<Int> {
        val childCount = children[parent]!!.len()
        if (childIndex >= childCount) {
            return Result.Err(TaffyError.ChildIndexOutOfBounds(parent, childIndex, childCount))
        }

        return Result.Ok(children[parent]!![childIndex])
    }

    /**
     * Returns the total number of nodes in the tree
     */
    fun totalNodeCount(): Int {
        return nodes.len()
    }

    /**
     * Returns the `NodeId` of the parent node of the specified node (if it exists)
     *
     * - Return None if the specified node has no parent
     * - Panics if the specified node does not exist
     */
    fun parent(childId: Int): Option<Int> {
        return parents[childId]!!
    }

    /**
     * Returns a list of children that belong to the parent node
     */
    fun children(parent: Int): Result<List<Int>> {
        return Result.Ok(children[parent]!!.toList())
    }

    /**
     * Sets the [`Style`] of the provided `node`
     */
    fun setStyle(node: Int, style: Style): Result<Boolean> {
        nodes[node]?.style = style
        markDirty(node)
        return Result.Ok(false)
    }

    /**
     * Gets the [`Style`] of the provided `node`
     */
    fun style(node: Int): Result<Style> {
        return Result.Ok(nodes[node]!!.style)
    }

    /**
     * Return this node layout relative to its parent
     */
    fun layout(node: Int): Result<Layout> {
        return if (config.useRounding) {
            Result.Ok(nodes[node]!!.finalLayout)
        } else {
            Result.Ok(nodes[node]!!.unroundedLayout)
        }
    }

    private fun markDirtyRecursive(
        nodes: RustMap<NodeData>,
        parents: AltMap<Option<Int>>,
        nodeKey: Int,
    ) {
        nodes[nodeKey]!!.markDirty()

        when (val v = parents[nodeKey]!!) {
            is Option.Some -> markDirtyRecursive(nodes, parents, v.unwrap())
            else -> {}
        }
    }

    /**
     * Marks the layout computation of this node and its children as outdated
     *
     * Performs a recursive depth-first search up the tree until the root node is reached
     *
     * WARNING: this will stack-overflow if the tree contains a cycle
     */
    fun markDirty(node: Int): Result<Boolean> {
        /// WARNING: this will stack-overflow if the tree contains a cycle

        markDirtyRecursive(nodes, parents, node)

        return Result.Ok(false)
    }

    /**
     * Indicates whether the layout of this node (and its children) need to be recomputed
     */
    fun dirty(node: Int): Result<Boolean> {
        return Result.Ok(nodes[node]!!.cache.isEmpty())
    }

    /**
     * Updates the stored layout of the provided `node` and its children
     */
    fun <MeasureFunction : (Size<Option<Float>>, Size<AvailableSpace>, Int, Option<NodeContext>, Style) -> Size<Float>> computeLayoutWithMeasure(
        nodeId: Int,
        availableSpace: Size<AvailableSpace>,
        measureFunction: MeasureFunction
    ): Result<Boolean> {
        val useRounding = config.useRounding
        val taffyView = TaffyView(taffy = this, measureFunction = measureFunction)
        computeRootLayout(taffyView, nodeId, availableSpace)
        if (useRounding) {
            roundLayout(taffyView, nodeId)
        }
        return Result.Ok(false)
    }

    /**
     * Updates the stored layout of the provided `node` and its children
     */
    fun computeLayout(node: Int, availableSpace: Size<AvailableSpace>): Result<Boolean> {
        return computeLayoutWithMeasure(node, availableSpace, { _, _, _, _, _ -> Size.ZERO.clone() })
    }

    /**
     * Prints a debug representation of the tree's layout
     */
    fun printTree(root: Int) {
        Debug.printTree(this, root)
    }

    /**
     * Returns an instance of LayoutTree representing the TaffyTree
     */
    fun asLayoutTree(): LayoutPartialTree {
        return TaffyView(taffy = this, measureFunction = { _, _, _, _, _ -> Size.ZERO.clone() })
    }

    companion object {
        /**
         * Creates a new [`TaffyTree`]
         *
         * The default capacity of a [`TaffyTree`] is 16 nodes.
         */
        fun <NodeContext> new(): TaffyTree<NodeContext> {
            return TaffyTree(
                // TODO: make this method const upstream,
                // so constructors here can be const
                nodes = RustMap(),
                nodeContextData = mutableMapOf(),
                children = mutableMapOf(),
                parents = mutableMapOf(),
                config = TaffyConfig()
            )
        }
    }
}
