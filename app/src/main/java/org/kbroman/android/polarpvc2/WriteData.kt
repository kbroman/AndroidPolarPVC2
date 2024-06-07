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
    private var fp: File? = null

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

        createFile(filePath, fileName)
    }

    public fun getFileName(): String
    {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val currentTime = LocalDateTime.now().format(formatter)

        return "${currentTime}.csv"
    }


    private fun createFile(pickerInitialUri: Uri?, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, fileName)

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == CREATE_FILE_REQUEST_CODE
            && resultCode == Activity.RESULT_OK) {

            resultData?.data?.also { uri ->
                fp = File(uri.getPath(), "w")

                // write header
                fp?.writeText("timestamp,ecg\n")            }
        }
    }



}