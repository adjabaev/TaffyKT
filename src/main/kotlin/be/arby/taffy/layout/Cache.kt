package be.arby.taffy.layout

import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.AvailableSpace
import be.arby.taffy.tree.layout.RunMode

class Cache(
    var knownDimensions: Size<Option<Float>>,
    var availableSpace: Size<AvailableSpace>,
    var runMode: RunMode,
    var cachedSizeAndBaselines: SizeAndBaselines
)
