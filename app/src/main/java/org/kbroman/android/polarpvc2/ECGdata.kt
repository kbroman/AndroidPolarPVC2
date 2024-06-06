package org.kbroman.android.polarpvc2

class ECGdata(private val maxItems: Int) {
    public val volt = FixedSizedList<Double>(maxItems)
    public val time = FixedSizedList<Long>(maxItems)

    fun add(voltage : Double, timestamp : Long) {
        volt.add(voltage)
        time.add(timestamp)
    }

    fun size() : Int {
        return volt.size()
    }

    fun maxIndex(): Int = volt.maxIndex()

}