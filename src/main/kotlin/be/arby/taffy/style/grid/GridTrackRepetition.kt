package be.arby.taffy.style.grid

sealed class GridTrackRepetition {
    data object AutoFill : GridTrackRepetition()
    data object AutoFit : GridTrackRepetition()
    data class Count(var i: Int): GridTrackRepetition()
}
