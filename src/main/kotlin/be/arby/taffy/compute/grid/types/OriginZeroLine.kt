package be.arby.taffy.compute.grid.types

data class OriginZeroLine(var value: Short) : GridCoordinate, Comparable<OriginZeroLine> {
    override fun asShort(): Short {
        return value
    }

    operator fun plus(other: OriginZeroLine): OriginZeroLine {
        return OriginZeroLine((value + other.value).toShort())
    }

    operator fun minus(other: OriginZeroLine): OriginZeroLine {
        return OriginZeroLine((value - other.value).toShort())
    }

    operator fun plus(other: Short): OriginZeroLine {
        return OriginZeroLine((value + other).toShort())
    }

    operator fun minus(other: Short): OriginZeroLine {
        return OriginZeroLine((value - other).toShort())
    }

    operator fun plus(other: Int): OriginZeroLine {
        return OriginZeroLine((value + other).toShort())
    }

    operator fun minus(other: Int): OriginZeroLine {
        return OriginZeroLine((value - other).toShort())
    }

    operator fun inc(other: Short): OriginZeroLine {
        value = (value + other).toShort()
        return this
    }

    operator fun dec(other: Short): OriginZeroLine {
        value = (value - other).toShort()
        return this
    }

    fun intoTrackVecIndex(trackCounts: TrackCounts): Int {
        return 2 * ((value + trackCounts.negativeImplicit))
    }

    fun impliedNegativeImplicitTracks(): Int {
        return if (value < 0) {
            value * -1
        } else {
            0
        }
    }

    fun impliedPositiveImplicitTracks(explicitTrackCount: Int): Int {
        return if (value > explicitTrackCount) {
            value - explicitTrackCount
        } else {
            0
        }
    }

    override operator fun compareTo(other: OriginZeroLine): Int {
        return if (value > other.value) 1 else if (value < other.value) -1 else 0
    }
}
