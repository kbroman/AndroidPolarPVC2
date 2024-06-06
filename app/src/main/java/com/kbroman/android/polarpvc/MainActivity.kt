package com.kbroman.android.polarpvc

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.kbroman.android.polarpvc.databinding.ActivityMainBinding
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.Pair
import com.google.android.material.snackbar.Snackbar

private lateinit var binding: ActivityMainBinding
private var ecgDisposable: Disposable? = null
private var deviceConnected = false
private var bluetoothEnabled = false


class MainActivity : AppCompatActivity() {
    private var deviceId: String = getString(R.string.device_id)
    private val TAG: String = getString(R.string.app_name)

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

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

        clear_device_text()

        binding.connectSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) { // start connection
                Log.i(TAG, "Opening connection")

                //    api.connectToDevice(deviceId)

                binding.deviceTextView.text = getString(R.string.device_id)
                binding.batteryTextView.text =
                    getString(R.string.battery_text) // replace with battery level

            } else { // close connection

                Log.i(TAG, "Closing connection")

                if (binding.recordSwitch.isChecked) {
                    Log.i(TAG, "currently recording")

                    // FIX_ME: should open a dialog box to verify you want to stop recording
                    // (maybe always verify stopping recording)

                    binding.recordSwitch.isChecked = false  // this will call stop_recording()
                }

                //    api.disconnectFromDevice(deviceId)
                clear_device_text()
            }
        }

        binding.recordSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) { // start recording
                Log.i(TAG, "Starting recording")

                if (!binding.connectSwitch.isChecked) {
                    Log.i(TAG, "not yet connected")
                    binding.connectSwitch.isChecked = true   // this will call open_connection()
                }
            } else { // stop recording
                Log.i(TAG, "Stopping recording")

                // FIX_ME: open dialog box to verify that you want to stop recording?            }
            }
        }


        // Activity life cycle https://developer.android.com/reference/android/app/Activity
        // onCreate, onStart, onResume, onPause, onStop, onDestroy, onRestart

    }



    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        //   api.foregroundEntered()
    }

    public override fun onDestroy() {
        super.onDestroy()
        //   api.shutDown()
    }


    private fun clear_device_text() {
        binding.deviceTextView.text = ""
        binding.batteryTextView.text = ""
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }

}