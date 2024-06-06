package org.kbroman.android.polarpvc2

// this is to keep all ECG data with ever-growing indexes, but not blow up memory use
// this contains longs, for the time stamps
class FixedSizeLList(private val maxItems: Int) {
    private val list = ArrayList<Long>()
    private var offset: Int = 0

    init {
        offset = 0
    }

    fun add(value : Long) {
        if (list.size == maxItems) {
            offset++
            list.add(value)
            list.removeFirst()
        }
    }

     fun get(index: Int): Long {
        return if (index < offset || index >= list.size + offset) 0 else list.get(index - offset)
    }

    fun size(): Int = list.size
}
