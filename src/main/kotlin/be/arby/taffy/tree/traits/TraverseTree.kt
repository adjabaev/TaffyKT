package be.arby.taffy.tree.traits

/**
 * A marker trait which extends `TraversePartialTree` with the additional guarantee that the child/children methods can be used to recurse
 * infinitely down the tree. Is required by the `RoundTree` and the `PrintTree` traits.
 */
interface TraverseTree: TraversePartialTree
