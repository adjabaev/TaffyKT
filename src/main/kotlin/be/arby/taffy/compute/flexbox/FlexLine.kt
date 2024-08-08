package be.arby.taffy.compute.flexbox

data class FlexLine(
    var items: List<FlexItem>,
    var crossSize: Float,
    var offsetCross: Float
)
