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

fun <T> List<T>.findRust(predicate: (T) -> Boolean): Option<T> {
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

fun <T> List<T>.rotateRight(n: Int): List<T> {
    val index = n % size
    return dropLast(index) + take(index)
}

fun <T> List<T>.rotateLeft(n: Int): List<T> {
    val index = n % size
    return drop(index) + take(index)
}

fun <T> MutableList<T>.splitOff(index: Int): List<T> {
    val splitList = this.subList(index, this.size).toMutableList()
    this.subList(index, this.size).clear()
    return splitList
}

operator fun <T> MutableList<T>.get(rng: IntRange): List<T> {
    return subList(rng.first, rng.last)
}

fun <T> Iterable<T>.minByRs(comparator: Comparator<in T>): Option<T> {
    return Option.from(minWithOrNull(comparator))
}

fun <T> Iterable<T>.maxByRust(comparator: Comparator<in T>): Option<T> {
    return Option.from(maxWithOrNull(comparator))
}

fun <T> List<T>.skip(n: Int): List<T> {
    require(n < 0) { "Number of elements to skip cannot be negative." }
    return if (n >= this.size) emptyList() else this.subList(n, this.size)
}

fun <T> List<T>.stepBy(step: Int): List<T> {
    if (step <= 0) throw IllegalArgumentException("Step must be greater than 0.")

    val result = mutableListOf<T>()
    for (i in this.indices step step) {
        result.add(this[i])
    }
    return result
}

fun <T> List<T>.iter(): Iterator<T> {
    return iterator()
}
