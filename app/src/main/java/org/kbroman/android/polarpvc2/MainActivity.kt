package org.kbroman.android.polarpvc2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import org.kbroman.android.polarpvc2.databinding.ActivityMainBinding
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID


private lateinit var binding: ActivityMainBinding



class MainActivity : AppCompatActivity() {
    var filePath: Uri? = null
    private var deviceId: String = "D45EC729"
    private var ecgDisposable: Disposable? = null
    private var deviceConnected = false
    private var bluetoothEnabled = false
    private var isRecording = false

    companion object {
        private const val TAG = "PolarPVC2main"
        private const val PERMISSION_REQUEST_CODE = 1
        private const val DIRECTORY_REQUEST_CODE = 501
        private const val RR_TO_HR_FACTOR: Double = 1.0e9 / 7682304.0 * 60
    }

    private val api: PolarBleApi by lazy {
        // Notice all features are enabled
        PolarBleApiDefaultImpl.defaultImplementation(
            applicationContext,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO)
        )
    }

    public val pd: PeakDetection = PeakDetection()
    public val wd: WriteData = WriteData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        api.setPolarFilter(false)
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.i(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    showToast("Phone Bluetooth on")
                } else {
                    showToast("Phone Bluetooth off")
                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.i(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                deviceId = polarDeviceInfo.deviceId
                deviceConnected = true
                binding.connectSwitch.isChecked = true
                binding.deviceTextView.text = deviceId

            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.i(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.i(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                deviceConnected = false
                binding.connectSwitch.isChecked = false
                binding.deviceTextView.text = ""
                binding.batteryTextView.text = ""
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.i(TAG, "DIS INFO uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.i(TAG, "BATTERY LEVEL: $level")
                binding.batteryTextView.text = "Battery level $level"


                // also set the local time on the device
                val timeZone = TimeZone.getTimeZone("UTC")  // I'm not sure why I need "UTC" here
                val calendar = Calendar.getInstance(timeZone)
                calendar.time = Date()
                api.setLocalTime(deviceId, calendar)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            val timeSetString = "time ${calendar.time} set to device"
                            Log.d(TAG, timeSetString)
                        },
                        { error: Throwable -> Log.e(TAG, "set time failed: $error") }
                    )
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d(TAG, "feature ready $feature")

                when (feature) {
                    PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING -> {
                        streamECG()
                    }

                    else -> {}
                }
            }

        })


        binding.connectSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { // open connection
                Log.i(TAG, "Opening connection")

                binding.deviceTextView.text = "Connecting..."
                binding.batteryTextView.text = "Battery level..."

                api.connectToDevice(deviceId)



            } else { // close connection
                Log.i(TAG, "Closing connection")

                if(binding.recordSwitch.isChecked) {
                    Log.i(TAG, "currently recording")

                    // FIX_ME: should open a dialog box to verify you want to stop recording
                    // (maybe always verify stopping recording)

                    binding.recordSwitch.isChecked=false  // this will call stop_recording()
                }

                ecgDisposable?.dispose()

                api.disconnectFromDevice(deviceId)
                binding.deviceTextView.text = ""
                binding.batteryTextView.text = ""
            }
        }

        binding.recordSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { // start recording
                Log.i(TAG, "Starting recording")

                if(!binding.connectSwitch.isChecked) {
                    Log.i(TAG, "not yet connected")
                    binding.connectSwitch.isChecked = true   // this will call open_connection()
                }
                isRecording = true

                if(!isExternalStorageWriteable()) {
                    Log.i(TAG, "External storage is not writable")
                    // can't write to SD card so skip
                    isRecording = false
                    binding.recordSwitch.isChecked = false
                } else {
                    Log.i(TAG, "gonna try to pick a directory")
                    openDirectory()
                }
            } else { // stop recording
                Log.i(TAG, "Stopping recording")
                isRecording = false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }

        // request permissions to write files to SD card
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (index in 0..grantResults.lastIndex) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    Log.w(TAG, "No sufficient permissions")
                    showToast("No sufficient permissions")
                    return
                }
            }
            Log.d(TAG, "Needed permissions are granted")
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == DIRECTORY_REQUEST_CODE
            && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.

            resultData?.data?.also { uri ->
                filePath = uri

                Log.i(TAG, "Chosen dir: $filePath")
            }
        }
    }


    private fun isExternalStorageWriteable(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == extStorageState && Environment.MEDIA_MOUNTED_READ_ONLY != extStorageState) {
            return true
        }
        return false
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        api.foregroundEntered()
    }

    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }

    fun openDirectory() {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {

        }

        startActivityForResult(intent, DIRECTORY_REQUEST_CODE)
    }



    fun streamECG() {
        val isDisposed = ecgDisposable?.isDisposed ?: true
        if (isDisposed) {
            ecgDisposable = api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ECG)
                .toFlowable()
                .flatMap { sensorSetting: PolarSensorSetting -> api.startEcgStreaming(deviceId, sensorSetting.maxSettings()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { polarEcgData: PolarEcgData ->
                        Log.i(TAG, "ecg update")

                        pd.processData(polarEcgData)  // PeakDetection -> find_peaks
                        if(isRecording && filePath != null) wd.writeData(filePath, polarEcgData)

                        if(pd.rrData.size() > 1) {
                            val hr_bpm = Math.round(60.0 / pd.rrData.average())
                            val pvc_ave = Math.round(pd.pvcData.average() * 100)
                            Log.i(TAG, "pvc = ${pvc_ave}   hr=${hr_bpm}")
                            binding.pvcTextView.text = "${pvc_ave}% pvc"
                            binding.hrTextView.text = "${hr_bpm} bpm"

                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Ecg stream failed $error")
                        ecgDisposable = null
                    },
                    {
                        Log.i(TAG, "Ecg stream complete")
                    }
                )
        } else {
            // NOTE stops streaming if it is "running"
            ecgDisposable?.dispose()
            ecgDisposable = null
        }
    }
}