package org.kbroman.android.polarpvc2

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File

class WriteData {
    public var fileOpen = false
    private var timeFileOpened: Long = -1
    private var fp: File? = null

    companion object {
        private const val TAG = "PolarPVC2write"
        private const val HOUR_IN_MILLI = 1000*60*60
    }


    public fun writeData(polarEcgData: PolarEcgData)
    {
        val currentTimeStamp: Long = Instant.now().toEpochMilli()
        val timeSinceOpen = currentTimeStamp - timeFileOpened
        if(timeFileOpened < 0 || timeSinceOpen > HOUR_IN_MILLI) {
              openFile()
        }

        // write data to the file
        for (data in polarEcgData.samples) {
            val voltage: Double = (data.voltage.toFloat() / 1000.0)
            val timestamp = data.timeStamp + PeakDetection.TIMESTAMP_OFFSET

            fp.appendText("${voltage},${timestamp}\n")
        }
    }

    public fun openFile()
    {
        val fileName = getFileName()
        timeFileOpened: Long = Instant.now().toEpochMilli()

        Log.i(TAG, "Opening file $fileName")

        fp = File(fileName)

        // also write the header ("timestamp,ecg")
        fp.writeText("timestamp,ecg\n")
    }

    public fun getFileName(): String
    {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val currentTime = LocalDateTime.now().format(formatter)

        return "${currentTime}.csv"
    }
}