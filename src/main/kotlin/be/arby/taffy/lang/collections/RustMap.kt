package be.arby.taffy.lang.collections

class RustMap<V> {
    private val data = mutableMapOf<Int, V>()
    private var nextKey = 0

    fun insert(value: V): Int {
        val key = nextKey
        data[key] = value
        nextKey++
        return key
    }

    operator fun get(id: Int): V? {
        return data[id]
    }

    fun clear() {
        data.clear()
    }

    fun remove(node: Int) {
        data.remove(node)
    }

    fun len(): Int {
        return data.size
    }
}
