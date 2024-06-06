package org.kbroman.android.polarpvc2

class ECGdata(private val maxItems: Int) {
    public val volt = FixedSizeDList(maxItems)
    public val time = FixedSizeLList(maxItems)

    fun add(voltage : Double, timestamp : Long) {
        volt.add(voltage)
        time.add(timestamp)
    }

    fun size() : Int {
        return volt.size()
    }
}
