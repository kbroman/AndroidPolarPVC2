package com.kbroman.android.polarpvc

class RunningAverage(private val maxItems: Int) {
    private val list = ArrayList<Double>()
    var sum: Double = 0.0

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

    fun size(): Int = list.size
}
