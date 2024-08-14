package be.arby.taffy.lang.collections

import be.arby.taffy.lang.Option

fun <T> List<T>.cycle(): Sequence<T> = sequence {
    if (this@cycle.isEmpty()) throw IllegalArgumentException("List must not be empty.")
    while (true) {
        for (element in this@cycle) {
            yield(element)
        }
    }
}

fun <T> Iterator<T>.nextRust(): Option<T> {
    return if (this.hasNext()) Option.Some(this.next()) else Option.None
}

fun <T> List<T>.copied(): List<T> {
    return this.toList()
}

fun <T> repeatElement(elt: T): Sequence<T> {
    return sequence {
        while (true) {
            yield(elt)
        }
    }
}

fun <T> Sequence<T>.skip(n: Int): Sequence<T> {
    if (n < 0) throw IllegalArgumentException("Number of elements to skip cannot be negative.")
    return this.drop(n)
}

fun <T> Sequence<T>.next(): Option<T> {
    val iterator = this.iterator()
    return if (iterator.hasNext()) Option.Some(iterator.next()) else Option.None
}

fun <T, R> List<T>.findMap(transform: (T) -> Option<R>): Option<R> {
    for (element in this) {
        when (val result = transform(element)) {
            is Option.Some -> return result
            is Option.None -> continue
        }
    }
    return Option.None
}

fun <T, K : Comparable<K>> List<T>.sortByKey(selector: (T) -> K): List<T> {
    return this.sortedBy(selector)
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
