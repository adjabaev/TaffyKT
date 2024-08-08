package be.arby.taffy.style.grid

sealed class GridTrackRepetition {
    object AutoFill : GridTrackRepetition()
    object AutoFit : GridTrackRepetition()
    data class Count(var i: Int): GridTrackRepetition()
}
