package be.arby.taffy.tests.generic.fixtures

import be.arby.taffy.lang.Option

data class TextMeasure(
    val textContent: String,
    val writingMode: WritingMode,
    val _aspectRatio: Option<Float>
)
