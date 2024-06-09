package org.kbroman.android.polarpvc2

import java.util.ArrayList

// this is to keep all ECG data with ever-growing indexes, but not blow up memory use
// this contains doubles, for the voltages
class FixedSizedList<T>(private val maxItems: Int) {
    private val list = ArrayList<T>()
    private var offset: Int = 0

    init {
        offset = 0
    }

    fun add(value : T) {
        if (list.size == maxItems) {
            offset++
            list.removeFirst()
        }
        list.add(value)
    }

     fun get(index: Int): T {
        return list[index - offset] // gonna be bad if index out of range
    }

    fun setLast(value: T) {
        list[list.size - 1] = value
    }

    fun size() = list.size

    fun maxIndex() = list.size + offset
}
