package be.arby.taffy.tests.generic.fixtures

import be.arby.taffy.geom.AbsoluteAxis
import be.arby.taffy.geom.Size
import be.arby.taffy.geom.bothAxisDefined
import be.arby.taffy.lang.Option
import be.arby.taffy.lang.floor
import be.arby.taffy.lang.max
import be.arby.taffy.lang.min
import be.arby.taffy.style.Style
import be.arby.taffy.style.dimension.AvailableSpace

fun testMeasureFunction(
    knownDimensions: Size<Option<Float>>,
    availableSpace: Size<AvailableSpace>,
    _nodeId: Int,
    nodeContext: Option<TextMeasure>,
    _style: Style,
): Size<Float> {
    val ZWS: Char = '\u200B'
    val H_WIDTH: Float = 10f
    val H_HEIGHT: Float = 10f

    if (knownDimensions.bothAxisDefined()) {
        return Size(width = knownDimensions.width.unwrap(), height = knownDimensions.height.unwrap())
    }

    val v = if (nodeContext.isSome()) nodeContext.unwrap() else return Size.ZERO.clone()

    val inlineAxis = when (v.writingMode) {
        WritingMode.HORIZONTAL -> AbsoluteAxis.HORIZONTAL
        WritingMode.VERTICAL -> AbsoluteAxis.VERTICAL
    };
    val blockAxis = inlineAxis.otherAxis()
    val lines: List<String> = v.textContent.split(ZWS).toList()

    if (lines.isEmpty()) {
        return Size.ZERO.clone()
    }

    val minLineLength: Int = lines.map { line -> line.length }.maxOrNull() ?:0
    val maxLineLength: Int = lines.map { line -> line.length }.sum()
    val avs = availableSpace.getAbs(inlineAxis)
    val inlineSize = knownDimensions
        .getAbs(inlineAxis)
        .unwrapOrElse {
            when {
                avs is AvailableSpace.MinContent -> minLineLength.toFloat() * H_WIDTH
                avs is AvailableSpace.MaxContent -> maxLineLength.toFloat() * H_WIDTH
                avs is AvailableSpace.Definite -> avs.availableSpace.min(maxLineLength.toFloat() * H_WIDTH)
                else -> throw UnsupportedOperationException("Unknown available space type")
            }
        }
    .max(minLineLength.toFloat() * H_WIDTH)
    val blockSize = knownDimensions.getAbs(blockAxis).unwrapOrElse {
        val inlineLineLength = (inlineSize / H_WIDTH).floor().toInt()
        var lineCount = 1
        var currentLineLength = 0
        for (line in lines) {
            if (currentLineLength + line.length > inlineLineLength) {
                if (currentLineLength > 0) {
                    lineCount += 1
                };
                currentLineLength = line.length
            } else {
                currentLineLength += line.length
            };
        }
        lineCount.toFloat() * H_HEIGHT
    }

    return when (v.writingMode) {
        WritingMode.HORIZONTAL -> Size ( width = inlineSize, height = blockSize )
        WritingMode.VERTICAL -> Size ( width = blockSize, height = inlineSize )
    }
}
