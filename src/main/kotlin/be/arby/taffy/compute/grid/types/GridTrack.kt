package be.arby.taffy.compute.grid.types

import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.grid.MaxTrackSizingFunction
import be.arby.taffy.style.grid.MinTrackSizingFunction
import be.arby.taffy.utils.f32Min

class GridTrack(
    var kind: GridTrackKind,
    var isCollapsed: Boolean,
    var minTrackSizingFunction: MinTrackSizingFunction,
    var maxTrackSizingFunction: MaxTrackSizingFunction,
    var offset: Float,
    var baseSize: Float,
    var growthLimit: Float,
    var contentAlignmentAdjustment: Float,
    var itemIncurredIncrease: Float,
    var baseSizePlannedIncrease: Float,
    var growthLimitPlannedIncrease: Float,
    var infinitelyGrowable: Boolean
) {

    fun collapse() {
        isCollapsed = true
        minTrackSizingFunction = MinTrackSizingFunction.Fixed(LengthPercentage.Length(0.0f))
        maxTrackSizingFunction = MaxTrackSizingFunction.Fixed(LengthPercentage.Length(0.0f))
    }

    /**
     * Returns true if the track is flexible (has a Flex MaxTrackSizingFunction), else false.
     */
    fun isFlexible(): Boolean {
        return maxTrackSizingFunction is MaxTrackSizingFunction.Flex
    }

    /**
     * Returns true if the track is flexible (has a Flex MaxTrackSizingFunction), else false.
     */
    fun usesPercentage(): Boolean {
        return minTrackSizingFunction.usesPercentage() || maxTrackSizingFunction.usesPercentage()
    }

    /**
     * Returns true if the track has an intrinsic min and or max sizing function
     */
    fun hasIntrinsicSizingFunction(): Boolean {
        return minTrackSizingFunction.isIntrinsic() || maxTrackSizingFunction.isIntrinsic()
    }

    fun fitContentLimit(axisAvailableGridSpace: Option<Float>): Float {
        return when (maxTrackSizingFunction) {
            is MaxTrackSizingFunction.FitContent -> {
                when (val fn = (maxTrackSizingFunction as MaxTrackSizingFunction.FitContent).l) {
                    is LengthPercentage.Length -> fn.f
                    is LengthPercentage.Percent -> when {
                        axisAvailableGridSpace.isSome() -> axisAvailableGridSpace.unwrap() * fn.f
                        else -> Float.POSITIVE_INFINITY
                    }
                }
            }
            else -> Float.POSITIVE_INFINITY
        }
    }

    fun fitContentLimitedGrowthLimit(axisAvailableGridSpace: Option<Float>): Float {
        return f32Min(growthLimit, fitContentLimit(axisAvailableGridSpace))
    }

    fun flexFactor(): Float {
        return when (maxTrackSizingFunction) {
            is MaxTrackSizingFunction.Flex -> (maxTrackSizingFunction as MaxTrackSizingFunction.Flex).f
            else -> 0.0f
        }
    }

    companion object {
        fun newWithKind(
            kind: GridTrackKind,
            minTrackSizingFunction: MinTrackSizingFunction,
            maxTrackSizingFunction: MaxTrackSizingFunction
        ): GridTrack {
            return GridTrack(
                kind,
                isCollapsed = false,
                minTrackSizingFunction,
                maxTrackSizingFunction,
                offset = 0.0f,
                baseSize = 0.0f,
                growthLimit = 0.0f,
                contentAlignmentAdjustment = 0.0f,
                itemIncurredIncrease = 0.0f,
                baseSizePlannedIncrease = 0.0f,
                growthLimitPlannedIncrease = 0.0f,
                infinitelyGrowable = false
            )
        }

        fun make(
            minTrackSizingFunction: MinTrackSizingFunction,
            maxTrackSizingFunction: MaxTrackSizingFunction,
        ): GridTrack {
            return newWithKind(GridTrackKind.TRACK, minTrackSizingFunction, maxTrackSizingFunction)
        }

        fun gutter(size: LengthPercentage): GridTrack {
            return newWithKind(
                GridTrackKind.GUTTER,
                MinTrackSizingFunction.Fixed(size),
                MaxTrackSizingFunction.Fixed(size),
            )
        }
    }
}
