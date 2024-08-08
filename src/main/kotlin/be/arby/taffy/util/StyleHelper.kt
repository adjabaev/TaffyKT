package be.arby.taffy.util

import be.arby.taffy.compute.grid.types.GridLine
import be.arby.taffy.geometry.Line
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.grid.*
import be.arby.taffy.style.helpers.TaffyGridLine
import be.arby.taffy.style.helpers.TaffyGridSpan

class StyleHelper {
    companion object {
        @JvmStatic
        fun repeat(
            repetitionKind: GridTrackRepetition,
            trackList: List<NonRepeatedTrackSizingFunction>
        ): TrackSizingFunction {
            return TrackSizingFunction.Repeat(repetitionKind, trackList)
        }
        @JvmStatic
        fun repeat(
            repetitionCount: Int,
            trackList: List<NonRepeatedTrackSizingFunction>
        ): TrackSizingFunction {
            return TrackSizingFunction.Repeat(GridTrackRepetition.Count(repetitionCount), trackList)
        }

        @JvmStatic
        inline fun <reified T> minmax(min: MinTrackSizingFunction, max: MaxTrackSizingFunction): T {
            return when (T::class.java) {
                NonRepeatedTrackSizingFunction::class.java -> NonRepeatedTrackSizingFunction(min, max) as T
                TrackSizingFunction::class.java -> TrackSizingFunction.Single(NonRepeatedTrackSizingFunction(min, max)) as T
                else -> throw UnsupportedOperationException("Never should happen - minmax " + T::class.java)
            }
        }

        /**
         * Specifies a grid line to place a grid item between in CSS Grid Line coordinates:
         *  - Positive indices count upwards from the start (top or left) of the explicit grid
         *  - Negative indices count downwards from the end (bottom or right) of the explicit grid
         *  - ZERO IS INVALID index, and will be treated as a GridPlacement.Auto.
         */
        @JvmStatic
        inline fun <reified T : TaffyGridLine> line(index: Short): T {
            return when (T::class.java) {
                GenericGridPlacement::class.java -> GenericGridPlacement.fromLineIndex(index) as T
                Line::class.java -> Line.fromLineIndex(index) as T
                else -> throw UnsupportedOperationException("Never should happen - TaffyGridLine " + T::class.java)
            }
        }

        @JvmStatic
        inline fun <reified T : TaffyGridSpan> span(index: Int): T {
            return when (T::class.java) {
                GenericGridPlacement::class.java -> GenericGridPlacement.fromSpan(index) as T
                Line::class.java -> Line.fromSpan(index) as T
                else -> throw UnsupportedOperationException("Never should happen - TaffyGridSpan " + T::class.java)
            }
        }

        @JvmStatic
        inline fun <reified T> points(value: Float): T {
            return when (T::class.java) {
                MinTrackSizingFunction::class.java -> MinTrackSizingFunction.fromPoints(value) as T
                MaxTrackSizingFunction::class.java -> MaxTrackSizingFunction.fromPoints(value) as T
                NonRepeatedTrackSizingFunction::class.java -> NonRepeatedTrackSizingFunction.fromPoints(value) as T
                TrackSizingFunction::class.java -> TrackSizingFunction.fromPoints(value) as T
                LengthPercentage::class.java -> LengthPercentage.fromLength(value) as T
                LengthPercentageAuto::class.java -> LengthPercentageAuto.fromPoints(value) as T
                else -> throw UnsupportedOperationException("Never should happen - points " + T::class.java)
            }
        }

        @JvmStatic
        inline fun <reified T> percent(value: Float): T {
            return when (T::class.java) {
                MinTrackSizingFunction::class.java -> MinTrackSizingFunction.fromPercent(value) as T
                MaxTrackSizingFunction::class.java -> MaxTrackSizingFunction.fromPercent(value) as T
                NonRepeatedTrackSizingFunction::class.java -> NonRepeatedTrackSizingFunction.fromPercent(value) as T
                TrackSizingFunction::class.java -> TrackSizingFunction.fromPercent(value) as T
                LengthPercentage::class.java -> LengthPercentage.fromPercent(value) as T
                else -> throw UnsupportedOperationException("Never should happen - percent " + T::class.java)
            }
        }

        @JvmStatic
        inline fun <reified T> fraction(value: Float): T {
            return when (T::class.java) {
                NonRepeatedTrackSizingFunction::class.java -> NonRepeatedTrackSizingFunction.fromFlex(value) as T
                TrackSizingFunction::class.java -> TrackSizingFunction.fromFlex(value) as T
                MaxTrackSizingFunction::class.java -> MaxTrackSizingFunction.fromFlex(value) as T
                else -> throw UnsupportedOperationException("Never should happen - fraction " + T::class.java)
            }
        }

        @JvmStatic
        inline fun <reified T> fitContent(argument: LengthPercentage): T {
            return when (T::class.java) {
                MaxTrackSizingFunction::class.java -> MaxTrackSizingFunction.fitContent(argument) as T
                NonRepeatedTrackSizingFunction::class.java -> NonRepeatedTrackSizingFunction(MinTrackSizingFunction.Auto, MaxTrackSizingFunction.fitContent(argument)) as T
                TrackSizingFunction::class.java -> TrackSizingFunction.fitContent(argument) as T
                else -> throw UnsupportedOperationException("Never should happen - fitContent " + T::class.java)
            }
        }

        @JvmStatic
        inline fun <reified T> maxContent(): T {
            return when (T::class.java) {
                MinTrackSizingFunction::class.java -> MinTrackSizingFunction.MAX_CONTENT as T
                MaxTrackSizingFunction::class.java -> MaxTrackSizingFunction.MAX_CONTENT as T
                NonRepeatedTrackSizingFunction::class.java -> NonRepeatedTrackSizingFunction.MAX_CONTENT as T
                TrackSizingFunction::class.java -> TrackSizingFunction.Single(NonRepeatedTrackSizingFunction.MAX_CONTENT) as T
                else -> throw UnsupportedOperationException("Never should happen - maxContent " + T::class.java)
            }
        }

        @JvmStatic
        inline fun <reified T> minContent(): T {
            return when (T::class.java) {
                MinTrackSizingFunction::class.java -> MinTrackSizingFunction.MIN_CONTENT as T
                MaxTrackSizingFunction::class.java -> MaxTrackSizingFunction.MIN_CONTENT as T
                NonRepeatedTrackSizingFunction::class.java -> NonRepeatedTrackSizingFunction.MIN_CONTENT as T
                TrackSizingFunction::class.java -> TrackSizingFunction.Single(NonRepeatedTrackSizingFunction.MIN_CONTENT) as T
                else -> throw UnsupportedOperationException("Never should happen - minContent " + T::class.java)
            }
        }

        @JvmStatic
        inline fun <reified T> auto(): T {
            return when (T::class.java) {
                NonRepeatedTrackSizingFunction::class.java -> NonRepeatedTrackSizingFunction.AUTO as T
                TrackSizingFunction::class.java -> TrackSizingFunction.Single(NonRepeatedTrackSizingFunction.AUTO) as T
                GenericGridPlacement::class.java -> GenericGridPlacement.Auto<GridLine>() as T
                MinTrackSizingFunction::class.java -> MinTrackSizingFunction.Auto as T
                MaxTrackSizingFunction::class.java -> MaxTrackSizingFunction.Auto as T
                else -> throw UnsupportedOperationException("Never should happen - auto " + T::class.java)
            }
        }
    }
}
