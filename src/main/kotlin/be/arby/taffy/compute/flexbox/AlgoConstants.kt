package be.arby.taffy.compute.flexbox

import be.arby.taffy.geometry.Rect
import be.arby.taffy.geometry.Size
import be.arby.taffy.lang.Option
import be.arby.taffy.style.alignment.AlignContent
import be.arby.taffy.style.alignment.AlignItems
import be.arby.taffy.style.flex.FlexDirection

data class AlgoConstants(
    var dir: FlexDirection,
    var isRow: Boolean,
    var isColumn: Boolean,
    var isWrap: Boolean,
    var isWrapReverse: Boolean,
    var margin: Rect<Float>,
    var border: Rect<Float>,
    var paddingBorder: Rect<Float>,
    var gap: Size<Float>,
    var alignItems: AlignItems,
    var alignContent: AlignContent,
    var nodeOuterSize: Size<Option<Float>>,
    var nodeInnerSize: Size<Option<Float>>,
    var containerSize: Size<Float>,
    var innerContainerSize: Size<Float>
)
