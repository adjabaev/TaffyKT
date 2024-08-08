package be.arby.taffy.node

import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.AvailableSpace

fun interface MeasureFunc {
    fun apply(knownDimensions: Size<Option<Float>>, availableSize: Size<AvailableSpace>): Size<Float>
}
