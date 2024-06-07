package org.kbroman.android.polarpvc2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import com.polar.sdk.api.model.PolarEcgData
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WriteData : AppCompatActivity() {
    private var timeFileOpened: Long = -1
    private var fp: File = File("")
    private var fileReady = false

    companion object {
        private const val TAG = "PolarPVC2write"
        private const val HOUR_IN_MILLI = 1000*60*60
        const val CREATE_FILE_REQUEST_CODE = 101
    }


    public fun writeData(filePath: Uri?, polarEcgData: PolarEcgData)
    {
        val currentTimeStamp: Long = Instant.now().toEpochMilli()
        val timeSinceOpen = currentTimeStamp - timeFileOpened
        if(timeFileOpened < 0 || timeSinceOpen > HOUR_IN_MILLI) {
              openFile(filePath)
        }

        if(!fileReady) return

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
        Log.i(TAG, "Path selected? $pathSelected")
        Log.i(TAG, "path=${filePath.toString()} name=$fileName")

        if(pathSelected) {
            fp = File(filePath.toString(), fileName)
            Log.i(TAG, "hello")
            fp.writeText("timestamp,ecg\n")
            Log.i(TAG, "hello again")

            fileReady = true
        }
    }

    public fun getFileName(): String
    {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val currentTime = LocalDateTime.now().format(formatter)

        return "${currentTime}.csv"
    }







}