package be.arby.taffy.lang

fun <T> Boolean.then(f: () -> T): Option<T> {
    if (this) {
        return Option.from(f())
    }
    return Option.None
}
