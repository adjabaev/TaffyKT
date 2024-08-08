package be.arby.taffy.compute.flexbox

import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.node.Node
import be.arby.taffy.style.alignment.AlignSelf

data class FlexItem(
    var node: Node,
    var size: Size<Option<Float>>,
    var minSize: Size<Option<Float>>,
    var maxSize: Size<Option<Float>>,
    var alignSelf: AlignSelf,

    /**
     * The flex shrink style of the item
     */
    var flexShrink: Float,
    /**
     * The flex grow style of the item
     */
    var flexGrow: Float,

    var resolvedMinimumSize: Size<Float>,
    var inset: Rect<Option<Float>>,
    var margin: Rect<Float>,
    var padding: Rect<Float>,
    var border: Rect<Float>,
    var flexBasis: Float,
    var innerFlexBasis: Float,
    var violation: Float,
    var frozen: Boolean,

    var contentFlexFraction: Float,

    var hypotheticalInnerSize: Size<Float>,
    var hypotheticalOuterSize: Size<Float>,
    var targetSize: Size<Float>,
    var outerTargetSize: Size<Float>,
    var baseline: Float,
    var offsetMain: Float,
    var offsetCross: Float,
)
