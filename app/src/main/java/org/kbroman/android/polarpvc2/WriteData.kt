package org.kbroman.android.polarpvc2

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class WriteData {
    public var fileOpen = false
    private var timeOpened: Long = -1
    companion object {
        private const val TAG = "PolarPVC2write"
    }


    public fun writeData(polarEcgData: PolarEcgData)
    {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val currentTime = LocalDateTime.now().format(formatter)

        Log.i(TAG, "Current time: $currentTime")

        val currentTimeStamp: Long = Instant.now().toEpochMilli()

        if(timeOpened < 0) {
            Log.i(TAG, "Opening file")
            timeOpened = currentTimeStamp
        } else {
            Log.i(TAG, "Elapsed time: ${currentTimeStamp - timeOpened}")
            timeOpened = currentTimeStamp
        }
    }

    // file handle
    // timeOpened
    // currentTime
    // current date/time -> filename
    // write data to file
    // open a file
    // close file


}