package be.arby.taffy.style.grid

import be.arby.taffy.lang.DoubleTryFrom
import be.arby.taffy.lang.Result

/**
 * The first argument to a repeated track definition. This type represents the type of automatic repetition to perform.
 * See <https://www.w3.org/TR/css-grid-1/#auto-repeat> for an explanation of how auto-repeated track definitions work
 * and the difference between AutoFit and AutoFill.
 */
sealed class GridTrackRepetition {
    /**
     * Auto-repeating tracks should be generated to fit the container
     * See: <https://developer.mozilla.org/en-US/docs/Web/CSS/repeat#auto-fill>
     */
    data object AutoFill : GridTrackRepetition()

    /**
     * Auto-repeating tracks should be generated to fit the container
     * See: <https://developer.mozilla.org/en-US/docs/Web/CSS/repeat#auto-fit>
     */
    data object AutoFit : GridTrackRepetition()

    /**
     * The specified tracks should be repeated exacts N times
     */
    data class Count(var i: Int): GridTrackRepetition()

    companion object: DoubleTryFrom<Int, String, GridTrackRepetition> {
        override fun tryFrom1(value: Int): Result<GridTrackRepetition> {
            return Result.Ok(Count(value))
        }

        override fun tryFrom2(value: String): Result<GridTrackRepetition> {
            return when (value) {
                "auto-fit" -> Result.Ok(AutoFit)
                "auto-fill" -> Result.Ok(AutoFill)
                else -> Result.Err(Exception("Invalid value for GridTrackRepetition: $value"))
            }
        }
    }
}
