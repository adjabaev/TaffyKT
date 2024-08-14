package be.arby.taffy.tests

import org.junit.jupiter.api.Assertions

fun assertEq(actual: Any, expected: Any) {
    Assertions.assertEquals(expected, actual)
}

fun assertEq(actual: Any, expected: Any, template: String) {
    Assertions.assertEquals(expected, actual, template)
}

fun assert(actual: Boolean) {
    Assertions.assertTrue(actual)
}

fun assert(actual: Boolean, template: String) {
    Assertions.assertTrue(actual, template)
}
