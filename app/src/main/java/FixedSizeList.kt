package org.kbroman.android.polarpvc2

// this is to keep all ECG data with ever-growing indexes, but not blow up memory use
class FixedSizeList(private val maxItems: Int) {
    private val list = ArrayList<Double>()
    var offset: Int = 0

    init {
        offset = 0
    }

    fun add(value : Double) {
        if (list.size == maxItems) {
            offset++
            list.add(value)
            list.removeFirst()
        }
    }

     fun get(index: Int): Double {
        return if (index < offset || index >= list.size + offset) 0.0 else list.get(index - offset)
    }

    fun size(): Int = list.size
}
