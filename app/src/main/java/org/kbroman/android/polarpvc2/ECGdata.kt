package org.kbroman.android.polarpvc2

class ECGdata(private val maxItems: Int) {
    val volt = FixedSizedList<Double>(maxItems)
    val time = FixedSizedList<Long>(maxItems)

    fun add(timestamp : Long, voltage : Double) {
        volt.add(voltage)
        time.add(timestamp)
    }

    fun size() : Int {
        return volt.size()
    }

    fun maxIndex(): Int = volt.maxIndex()

}
