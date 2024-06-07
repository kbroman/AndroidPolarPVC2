package org.kbroman.android.polarpvc2

import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import java.io.FileWriter
import java.io.PrintWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WriteData(activity: MainActivity) {
    private var timeFileOpened: Long = -1
    private var mActivity = activity
    public var filePointer: PrintWriter? = null

    companion object {
        private const val TAG = "PolarPVC2write"
        private const val HOUR_IN_MILLI = 1000*60*60
    }

    fun writeData(filePath: String, polarEcgData: PolarEcgData)
    {
        if(filePath == "") return  // don't yet have a directory

        val currentTimeStamp: Long = Instant.now().toEpochMilli()
        val timeSinceOpen = currentTimeStamp - timeFileOpened
        if(timeFileOpened < 0 || timeSinceOpen > HOUR_IN_MILLI) {
            if(filePointer != null) {
                Log.i(TAG, "Closing file")
                filePointer!!.flush()
                filePointer!!.close()
            }

            openFile(filePath)
        }

        // write data to the file
        for (data in polarEcgData.samples) {
            val voltage: Double = (data.voltage.toFloat() / 1000.0)
            val timestamp = data.timeStamp + PeakDetection.TIMESTAMP_OFFSET

            filePointer?.write("${timestamp},${voltage}\n")
        }
    }

    private fun openFile(filePath: String)
    {
        Log.i(TAG, "Opening file")

        val fileName = getFileName()

        val dirUri = Uri.parse(filePath)
        val documentId = DocumentsContract.getTreeDocumentId(dirUri)
        val docTreeUri = DocumentsContract.buildDocumentUriUsingTree(dirUri, documentId)
        val resolver = mActivity.getContentResolver()
        val docUri = DocumentsContract.createDocument(resolver, docTreeUri, "text/csv", fileName)
        val pfd = mActivity.getContentResolver().openFileDescriptor(docUri!!, "w")
        val writer = FileWriter(pfd!!.getFileDescriptor())
        filePointer = PrintWriter(writer)

        timeFileOpened = Instant.now().toEpochMilli()
        Log.i(TAG, "opened file $docUri")
        filePointer?.write("timestamp,ecg_mV\n")
    }

    private fun getFileName(): String
    {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val currentTime = LocalDateTime.now().format(formatter)

        return "${currentTime}.csv"
    }







}