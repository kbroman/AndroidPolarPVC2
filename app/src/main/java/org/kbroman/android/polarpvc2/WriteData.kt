package org.kbroman.android.polarpvc2

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WriteData {
    public var fileOpen = false
    private var timeFileOpened: Long = -1
    companion object {
        private const val TAG = "PolarPVC2write"
        private const val HOUR_IN_MILLI = 1000*60*60
    }


    public fun writeData(polarEcgData: PolarEcgData)
    {
        val currentTimeStamp: Long = Instant.now().toEpochMilli()
        val timeSinceOpen = currentTimeStamp - timeFileOpened
        if(timeFileOpened > 0 && timeSinceOpen > HOUR_IN_MILLI) {
            // fp.flush()
            // fp.close()
            openFile()
        }

        // if file has been opened > 1 hr, close file
        // if file closed, create and open a new file

        // write data to the file
        for (data in polarEcgData.samples) {
            val voltage: Double = (data.voltage.toFloat() / 1000.0)
            val timestamp = data.timeStamp + PeakDetection.TIMESTAMP_OFFSET
        }
    }

    public fun openFile()
    {
        val fileName = getFileName()
        timeFileOpened: Long = Instant.now().toEpochMilli()

        Log.i(TAG, "Opening file $fileName")

        // also write the header ("timestamp,ecg")

    }

    public fun getFileName(): String
    {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val currentTime = LocalDateTime.now().format(formatter)

        return "${currentTime}.csv"
    }

    public fun openFile()

    // file handle
    // timeOpened
    // currentTime
    // current date/time -> filename
    // write data to file
    // open a file
    // close file


}