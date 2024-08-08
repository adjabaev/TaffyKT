package be.arby.taffy.compute.grid.types

class GridLine(var value: Short) : GridCoordinate {
    override fun asShort(): Short {
        return value
    }

    fun intoOriginZeroLine(explicitTrackCount: Int): OriginZeroLine {
        val explicitLineCount = explicitTrackCount + 1
        val cmp = value.compareTo(0.toShort())
        val ozLine = when {
            cmp > 0 -> (value - 1).toShort()
            cmp < 0 -> (value + explicitLineCount).toShort()
            else -> throw UnsupportedOperationException("Grid line of zero is invalid")
        }
        return OriginZeroLine(ozLine)
    }

    companion object {
        fun from(value: Short): GridLine {
            return GridLine(value)
        }
    }
}
