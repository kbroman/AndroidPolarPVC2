package org.kbroman.android.polarpvc2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.androidplot.xy.XYPlot
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
import kotlin.math.pow
import kotlin.math.round


private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var deviceId: String = "D45EC729"
    private var ecgDisposable: Disposable? = null
    private var deviceConnected = false
    private var bluetoothEnabled = false
    private var isRecording = false
    private var filePath: String? = ""
    private var ECGplot: XYPlot? = null
    private var HRplot: XYPlot? = null
    private var PVCplot: XYPlot? = null

    companion object {
        private const val TAG = "PolarPVC2app_main"
        private const val PERMISSION_REQUEST_CODE = 1
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

    val pd: PeakDetection = PeakDetection(this)
    val wd: WriteData = WriteData(this)
    var ecgPlotter: ECGplotter? = null
    var hrPlotter: HRplotter? = null
    var pvcPlotter: PVCplotter? = null

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)  // try to keep screen on

        ECGplot = findViewById(R.id.ecgplot)
        HRplot = findViewById(R.id.hrplot)
        PVCplot = findViewById(R.id.pvcplot)

        api.setPolarFilter(false)
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    showToast("Phone Bluetooth on")
                } else {
                    showToast("Phone Bluetooth off")
                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Connected: ${polarDeviceInfo.deviceId}")
                deviceId = polarDeviceInfo.deviceId
                deviceConnected = true
                binding.connectSwitch.isChecked = true
                binding.deviceTextView.text = deviceId

            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Connecting: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Disconnected: ${polarDeviceInfo.deviceId}")
                deviceConnected = false
                binding.connectSwitch.isChecked = false
                binding.deviceTextView.text = ""
                binding.batteryTextView.text = ""
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.i(TAG, "Dis Info uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "Battery Level: $level")
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
                Log.d(TAG, "Opening connection")

                binding.deviceTextView.text = "Connecting..."
                binding.batteryTextView.text = "Battery level..."

                api.connectToDevice(deviceId)

            } else { // close connection
                Log.d(TAG, "Closing connection")

                if(binding.recordSwitch.isChecked) {
                    Log.d(TAG, "currently recording")

                    // FIX_ME: should open a dialog box to verify you want to stop recording
                    // (maybe always verify stopping recording)

                    binding.recordSwitch.isChecked=false  // this will call stop_recording()
                }

                ecgDisposable?.dispose()

                api.disconnectFromDevice(deviceId)
                pd.clear() // reset HR and RR running averages
                binding.deviceTextView.text = ""
                binding.batteryTextView.text = ""
            }
        }

        binding.recordSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { // start recording
                Log.d(TAG, "Starting recording")

                if(!binding.connectSwitch.isChecked) {
                    Log.d(TAG, "not yet connected")
                    binding.connectSwitch.isChecked = true   // this will call open_connection()
                }
                isRecording = true

                val prefs = getPreferences(MODE_PRIVATE)
                filePath = prefs.getString("PREF_FILE_PATH", "")
                if(filePath == "") chooseDataDirectory()
            } else { // stop recording
                Log.d(TAG, "Stopping recording")
                isRecording = false
                wd.closeFile()
                wd.timeFileOpened = -1 // to ensure file is opened when needed
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

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        if(api != null) api.foregroundEntered()

        if (ecgPlotter == null) {
            ECGplot!!.post({
                ecgPlotter = ECGplotter(this, ECGplot) })
        }
        if (hrPlotter == null) {
            HRplot!!.post({
                hrPlotter = HRplotter(this, HRplot) })
        }
        if (pvcPlotter == null) {
            PVCplot!!.post({
                pvcPlotter = PVCplotter(this, PVCplot) })
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
        wd.closeFile()
        binding.connectSwitch.isChecked = false
        binding.recordSwitch.isChecked = false
        pd.clear()  // clear HR and RR running average data
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }

    // this and the next are for selecting output directory
    private val openDirectoryLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->

         try {
            if (result.resultCode == RESULT_OK) {
                // Get Uri from Storage Access Framework.
                var uri = result.data!!.data
                Log.d(TAG, "got a filePath: $uri")

                // save to preferences
                val editor = getPreferences(MODE_PRIVATE).edit()
                if (uri == null) {
                    editor.putString("PREF_FILE_PATH", null)
                    editor.apply()
                }
                try {
                    this.getContentResolver().takePersistableUriPermission(
                        uri!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION +
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    editor.putString("PREF_FILE_PATH", uri.toString())
                    editor.apply()
                } catch (ex: Exception) {
                    Log.d(TAG,"Failed to save persistent uri permission")
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error in openDirectory")
        }
    }

    private fun chooseDataDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION and
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        openDirectoryLauncher.launch(intent)
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
                        Log.d(TAG, "ecg update")

                        pd.processData(polarEcgData)  // PeakDetection -> find_peaks
                        if(isRecording && filePath != "") {
                            Log.d(TAG, "writing data")
                            wd.writeData(filePath!!, polarEcgData)
                        }

                        if(pd.rrData.size() > 1) {
                            val hr_bpm: Double = 60.0 / pd.rrData.average()
                            val pvc_ave: Double = pd.pvcData.average() * 100.0
                            Log.d(TAG, "pvc = ${myround(pvc_ave, 0)}   hr=${myround(hr_bpm, 1)}")
                            binding.pvcTextView.text = "${Math.round(pvc_ave)}% pvc"
                            binding.hrTextView.text = "${Math.round(hr_bpm)} bpm"


                            if (pd.rrData.size() > 10) {
                                // add to hr and pvc plots
                                hrPlotter!!.addValues(pd.rrData.lastTime, hr_bpm)
                                pvcPlotter!!.addValues(pd.pvcData.lastTime, pvc_ave)
                            }
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Ecg stream failed $error")
                        ecgDisposable = null

                        // disconnected so turn switches off
                        binding.connectSwitch.isChecked = false
                        binding.recordSwitch.isChecked = false
                    },
                    {
                        Log.d(TAG, "Ecg stream complete")
                    }
                )
        } else {
            // NOTE stops streaming if it is "running"
            ecgDisposable?.dispose()
            ecgDisposable = null
        }
    }
}

fun myround(value: Double, digits: Int): String {
    val tens: Double = if(digits < 0) 10.0.pow(-digits) else 10.0.pow(digits)

    if(digits == 0) {
        return round(value).toInt().toString()
    } else if(digits < 0) {
        return (round(value/tens) *tens).toInt().toString()
    } else {
        return (round(value*tens) /tens).toString()
    }
}
