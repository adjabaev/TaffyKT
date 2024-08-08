package be.arby.taffy.test

import net.asterium.taffy.geometry.Size
import net.asterium.taffy.lang.Option
import net.asterium.taffy.maths.axis.AbsoluteAxis
import net.asterium.taffy.maths.max
import net.asterium.taffy.maths.min
import net.asterium.taffy.style.dimension.AvailableSpace
import net.asterium.taffy.utils.Ordering
import java.util.*
import kotlin.math.floor

enum class WritingMode {
    HORIZONTAL,
    VERTICAL
}

class FixtureUtils {
    companion object {
        fun measureStandardText(
            knownDimensions: Size<Option<Float>>,
            availableSpace: Size<AvailableSpace>,
            textContent: String,
            writingMode: WritingMode,
            aspectRatio: Option<Float>
        ): Size<Float> {
            fun f32Max(a: Float, b: Float): Float {
                return Ordering.maxBy(a, b, Ordering::totalCmp)
            }

            val zws = '\u200B'
            val hWidth = 10.0f
            val hHeight = 10.0f

            if (knownDimensions.width.isSome() && knownDimensions.height.isSome()) {
                return Size(knownDimensions.width.unwrap(), knownDimensions.height.unwrap())
            }

            val inlineAxis = when (writingMode) {
                WritingMode.HORIZONTAL -> AbsoluteAxis.HORIZONTAL
                WritingMode.VERTICAL -> AbsoluteAxis.VERTICAL
            }
            val blockAxis = inlineAxis.otherAxis()

            val lines: List<String> = textContent.split(zws)
            if (lines.isEmpty()) {
                return Size.zeroF()
            }

            val minLineLength = lines.map { line -> line.length }.max()
            val maxLineLength = lines.sumOf { line -> line.length }
            val f = when (val a = availableSpace.getAbs(inlineAxis)) {
                is AvailableSpace.MinContent -> minLineLength.toFloat() * hWidth
                is AvailableSpace.MaxContent -> maxLineLength.toFloat() * hWidth
                is AvailableSpace.Definite -> a.availableSpace.min(maxLineLength.toFloat() * hWidth)
                    .max(minLineLength.toFloat() * hWidth)

                else -> 0f
            }
            val inlineSize = knownDimensions.getAbs(inlineAxis).unwrapOr(f)

            val inlineLineLength = floor(inlineSize / hWidth).toInt()
            var lineCount = 1
            var currentLineLength = 0

            for (line in lines) {
                if (currentLineLength + line.length > inlineLineLength) {
                    if (currentLineLength > 0) {
                        lineCount += 1
                    }
                    currentLineLength = line.length
                } else {
                    currentLineLength += line.length
                }
            }

            val blockSize = knownDimensions.getAbs(blockAxis).unwrapOr((lineCount.toFloat()) * hHeight);

            return when (writingMode) {
                WritingMode.HORIZONTAL -> {
                    Size(
                        width = inlineSize,
                        // Apply aspect ratio
                        height = f32Max(blockSize, aspectRatio.map { ratio -> inlineSize / ratio }.unwrapOr(0.0f))
                    )
                }
                WritingMode.VERTICAL -> {
                    Size(
                        // Apply aspect ratio
                        width = f32Max(blockSize, aspectRatio.map { ratio -> inlineSize * ratio }.unwrapOr(0.0f)),
                        height = inlineSize
                    )
                }
            }
        }
    }
}
