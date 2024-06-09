package org.kbroman.android.polarpvc2

import java.util.ArrayList
import kotlin.math.sqrt

class RunningAveSD(private val maxItems: Int)  {

    private val list = ArrayList<Double>()
    private var sum: Double = 0.0
    private var sumsq: Double = 0.0

    init {
        sum = 0.0
        sumsq = 0.0
    }

    fun add(value: Double) {
        if (list.size == maxItems) {
            val first = list.removeFirst()
            sum -= first
            sumsq -= (first * first)
        }
        list.add(value)
        sum += value
        sumsq += (value * value)
    }

    fun average(): Double {
        return if (list.isEmpty()) 0.0 else sum / list.size
    }

    fun size() = list.size

    fun sd(): Double {
        val n = list.size
        return if (n < 2) {
            0.0
        } else {
            sqrt((sumsq - (sum * sum) / n) / (n - 1))
        }
    }
}
