package be.arby.taffy;

import be.arby.taffy.compute.Compute;
import be.arby.taffy.geometry.Size;
import be.arby.taffy.lang.Option;
import be.arby.taffy.layout.Cache;
import be.arby.taffy.tree.layout.Layout;
import be.arby.taffy.layout.LayoutTree;
import be.arby.taffy.node.MeasureFunc;
import be.arby.taffy.node.Node;
import be.arby.taffy.node.NodeData;
import be.arby.taffy.style.Style;
import be.arby.taffy.style.dimension.AvailableSpace;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Taffy implements LayoutTree {
    public List<Node> leafs = new ArrayList<>();

    public Taffy() {
    }

    @Override
    public @NotNull List<Node> children(Node node) {
        return node.children;
    }

    @Override
    public int childCount(Node node) {
        return node.children.size();
    }

    @Override
    public boolean isChildless(Node node) {
        return node.children.isEmpty();
    }

    public Option<Node> parent(Node node) {
        return Option.from(node.parent);
    }

    @Override
    public @NotNull Style style(Node node) {
        return node.data.getStyle();
    }

    @Override
    public @NotNull Layout layout(Node node) {
        return node.data.getLayout();
    }

    @Override
    public void layout(Node node, @NotNull Layout layout) {
        node.data.setLayout(layout);
    }

    public void markDirty(Node node) {
        markDirtyRecursive(node);
    }

    @Override
    public @NotNull Size<Float> measureNode(Node node, @NotNull Size<Option<Float>> knownDimensions, @NotNull Size<AvailableSpace> availableSpace) {
        return node.measureFunc.apply(knownDimensions, availableSpace);
    }

    @Override
    public boolean needsMeasure(Node node) {
        return node.data.getNeedsMeasure() && node.measureFunc != null;
    }

    @Override
    public @NotNull Option<Cache> cache(Node node, int index) {
        return node.data.getSizeCache()[index];
    }

    @Override
    public void cache(Node node, int index, Cache cache) {
        node.data.getSizeCache()[index] = Option.from(cache);
    }

    @Override
    public @NotNull Node child(Node node, int index) {
        return node.children.get(index);
    }

    public void cache(Node node, int index, Option<Cache> newCache) {
        node.data.getSizeCache()[index] = newCache;
    }

    public Node newLeaf(Style layout) {
        Node node = Node.Companion.make(NodeData.Companion.make(layout));
        leafs.add(node);
        return node;
    }

    public Node newLeafWithMeasure(Style layout, MeasureFunc func) {
        NodeData data = NodeData.Companion.make(layout);
        data.setNeedsMeasure(true);

        Node node = Node.Companion.make(data);
        node.measureFunc = func;

        leafs.add(node);
        return node;
    }

    public Node newLeafWithChildren(Style layout, List<Node> children) {
        Node node = newLeaf(layout);

        for (Node child : children) {
            child.parent = node;
        }

        node.children.addAll(children);
        return node;
    }

    public void clear() {
        this.leafs.clear();
    }

    public void remove(Node node) {
        for (Node child : node.children) {
            child.parent = null;
        }

        if (node.parent != null) {
            node.parent.children.remove(node);
        }

        this.leafs.remove(node);
    }

    public void setMeasure(Node node, MeasureFunc func) {
        node.measureFunc = func;
        node.data.setNeedsMeasure(true);
        markDirtyRecursive(node);
    }

    public void addChild(Node parent, Node child) {
        child.parent = parent;
        parent.children.add(child);
        markDirtyRecursive(parent);
    }

    public void addChild(Node parent, Node child, int index) {
        child.parent = parent;
        parent.children.add(index, child);
        markDirtyRecursive(parent);
    }

    public void setChildren(Node parent, List<Node> children) {
        // Remove node as parent from all its current children.
        for (Node child : parent.children) {
            child.parent = null;
        }

        // Build up relation node <-> child
        for (Node child : children) {
            child.parent = parent;
        }

        parent.children = children;
        markDirtyRecursive(parent);
    }

    public void removeChild(Node parent, Node child) {
        int index = parent.children.indexOf(child);
        if (index != -1) {
            removeChildAtIndex(parent, index);
        }
    }

    public void removeChildAtIndex(Node parent, int childIndex) {
        int childCount = parent.children.size();
        if (childIndex >= childCount) {
            throw new IndexOutOfBoundsException(String.format("Requested: %s Size: %s", childIndex, childCount));
        }

        Node child = parent.children.remove(childIndex);
        child.parent = null;

        markDirtyRecursive(parent);
    }

    public void replaceChildAtIndex(Node parent, int childIndex, Node newChild) {
        int childCount = parent.children.size();
        if (childIndex >= childCount) {
            throw new IndexOutOfBoundsException(String.format("Requested: %s Size: %s", childIndex, childCount));
        }

        newChild.parent = parent;

        Node oldChild = parent.children.set(childIndex, newChild);
        oldChild.parent = null;

        markDirtyRecursive(parent);
    }

    public Node childAtIndex(Node parent, int childIndex) {
        int childCount = parent.children.size();
        if (childIndex >= childCount) {
            throw new IndexOutOfBoundsException(String.format("Requested: %s Size: %s", childIndex, childCount));
        }

        return parent.children.get(childIndex);
    }

    public void setStyle(Node node, Style style) {
        node.data.setStyle(style);
        markDirtyRecursive(node);
    }

    public boolean dirty(Node node) {
        return Arrays.stream(node.data.getSizeCache()).allMatch(Option::isNone);
    }

    public void computeLayout(Node node, Size<AvailableSpace> availableSpace) {
        Compute.computeLayout(this, node, availableSpace);
    }

    public static Consumer<Node> markDirtyRecursive = (node) -> {
        node.data.markDirty();

        if (node.parent != null) {
            markDirtyRecursive(node.parent);
        }
    };

    private static void markDirtyRecursive(Node node) {
        markDirtyRecursive.accept(node);
    }
}
