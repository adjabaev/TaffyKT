package be.arby.taffy.lang

fun <T> matches(item: T, vararg items: T): Boolean {
    for (ac in items) {
        if (ac == item) {
            return true
        }
    }
    return false
}
