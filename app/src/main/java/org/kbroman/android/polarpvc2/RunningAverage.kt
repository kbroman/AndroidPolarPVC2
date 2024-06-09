package org.kbroman.android.polarpvc2

import java.util.ArrayList

class RunningAverage(private val maxItems: Int) {
    private val list = ArrayList<Double>()
    private var sum: Double = 0.0
    var lastTime: Double = -1.0;

    init {
        sum = 0.0
    }

    fun add(value: Double) {
        if (list.size == maxItems) {
            sum -= list.removeFirst()
        }
        list.add(value)
        sum += value
    }

    fun average(): Double {
        return if (list.isEmpty()) 0.0 else sum / list.size
    }

    fun size() = list.size
}
