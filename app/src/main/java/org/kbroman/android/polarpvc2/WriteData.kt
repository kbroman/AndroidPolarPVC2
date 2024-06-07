package org.kbroman.android.polarpvc2

import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.polar.sdk.api.model.PolarEcgData
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WriteData (){
    private var timeFileOpened: Long = -1
    private var fp: File? = null

    companion object {
        private const val TAG = "PolarPVC2write"
        private const val HOUR_IN_MILLI = 1000*60*60
    }


    public fun writeData(filePath: Uri?, polarEcgData: PolarEcgData)
    {
        val currentTimeStamp: Long = Instant.now().toEpochMilli()
        val timeSinceOpen = currentTimeStamp - timeFileOpened
        if(timeFileOpened < 0 || timeSinceOpen > HOUR_IN_MILLI) {
              openFile(filePath)
        }

        // write data to the file
        for (data in polarEcgData.samples) {
            val voltage: Double = (data.voltage.toFloat() / 1000.0)
            val timestamp = data.timeStamp + PeakDetection.TIMESTAMP_OFFSET

            fp?.appendText("${voltage},${timestamp}\n")
        }
    }

    public fun openFile(filePath: Uri?)
    {
        val fileName = getFileName()
        timeFileOpened = Instant.now().toEpochMilli()

        Log.i(TAG, "Opening file $fileName")

        fp = File(filePath?.toString() + "/" + fileName, "w")

        // write header
        fp?.writeText("timestamp,ecg\n")
    }

    public fun getFileName(): String
    {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val currentTime = LocalDateTime.now().format(formatter)

        return "${currentTime}.csv"
    }
}