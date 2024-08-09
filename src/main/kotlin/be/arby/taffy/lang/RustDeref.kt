package be.arby.taffy.lang

data class RustDeref<T>(var value: T) {
    fun get(): T {
        return value
    }

    fun set(value: T) {
        this.value = value
    }
}

operator fun RustDeref<Float>.plusAssign (other: Float) {
    set(value + other)
}
