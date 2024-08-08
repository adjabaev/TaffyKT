package be.arby.taffy.util

class Ordering {
    companion object {
        fun totalCmp(a: Float, b: Float): Int {
            var left = a.toBits()
            var right = b.toBits()

            left = left xor ((left shr 31).toUInt() shr 1).toInt()
            right = right xor ((right shr 31).toUInt() shr 1).toInt()

            return left.compareTo(right)
        }

        fun maxBy(a: Float, b: Float, comparator: Comparator<Float>): Float {
            val cmp = comparator.compare(a, b)
            return if (cmp > 0) {
                a
            } else {
                b
            }
        }
    }
}
