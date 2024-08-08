package be.arby.taffy.lang.collections

import be.arby.taffy.lang.Option
import java.util.*

fun <T> List<T>.position(predicate: (T) -> Boolean): Option<Int> {
    val p = indexOfFirst(predicate);
    return if (p == -1) Option.None else Option.Some(p)
}

fun <T> List<T>.rposition(predicate: (T) -> Boolean): Option<Int> {
    var index = -1
    for ((i, element) in this.withIndex()) {
        if (predicate(element)) {
            index = i
        }
    }
    return if (index >= 0) Option.Some(index) else Option.None
}

fun <T> Sequence<T>.cycle() = sequence { while (true) yieldAll(this@cycle) }

fun <T, R> List<T>.findMap(transform: (T) -> Option<R>): Option<R> {
    var result: Option<R> = Option.None
    this.map(transform).forEach { v ->
        if (v.isSome()) {
            result = v
            return@forEach
        }
    }
    return result
}

fun <T> List<T>.split(shouldSplit: (T) -> Boolean): List<List<T>> {
    val result = mutableListOf<MutableList<T>>()
    var current = mutableListOf<T>()
    for (element in this) {
        if (shouldSplit(element)) {
            result.add(current)
            current = mutableListOf()
        } else {
            current.add(element)
        }
    }
    result.add(current)
    return result
}

fun <T> List<T>.findOptional(predicate: (T) -> Boolean): Option<T> {
    return Option.from(find(predicate))
}

fun <T> List<T>.next(): Option<T> {
    if (isEmpty()) {
        return Option.None
    }
    return Option.Some(get(0))
}

fun <T> List<T>.len(): Int {
    return size
}

fun <T> List<T>.rev(): List<T> {
    return reversed()
}

fun <T> List<T>.enumerate(): Iterable<IndexedValue<T>> {
    return withIndex()
}

fun <T> Iterable<IndexedValue<T>>.findRust(predicate: (IndexedValue<T>) -> Boolean): Option<IndexedValue<T>> {
    return Option.from(find(predicate))
}

fun List<Option<Float>>.sum(): Option<Float> {
    var res = 0f
    forEach { o ->
        if (o.isSome()) {
            res += o.unwrap()
        }
    }
    return Option.Some(res)
}

fun <T> List<T>.splitAt(index: Int): Pair<List<T>, List<T>> {
    return this.take(index) to this.drop(index)
}

fun <T> Iterable<T>.minByRs(comparator: Comparator<in T>): Option<T> {
    return Option.from(minWithOrNull(comparator))
}

fun <T> Iterable<T>.maxByRs(comparator: Comparator<in T>): Option<T> {
    return Option.from(maxWithOrNull(comparator))
}
