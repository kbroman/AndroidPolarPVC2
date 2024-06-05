package com.kbroman.android.polarpvc

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kbroman.android.polarpvc.databinding.ActivityMainBinding

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {

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
            if (isChecked) {
                open_connection()

            } else {
                close_connection()
            }
        }

        binding.recordSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                start_recording()
            } else {
                stop_recording()
            }
        }

    }


    private fun open_connection() {
        Log.i("PolarPVC2", "Opening connection")

        binding.deviceTextView.text = getString(R.string.device_id)
        binding.batteryTextView.text = getString(R.string.battery_text) // replace with battery level

    }

    private fun close_connection() {
        Log.i("PolarPVC2", "Closing connection")
        clear_device_text()

        if(binding.recordSwitch.isChecked) {
            Log.i("PolarPVC2", "currently recording")
            binding.recordSwitch.isChecked=false  // this will call stop_recording()
        }
    }

    private fun start_recording() {
        Log.i("PolarPVC2", "Starting recording")

        if(!binding.connectSwitch.isChecked) {
            Log.i("PolarPVC2", "not yet connected")
            binding.connectSwitch.isChecked = true   // this will call open_connection()
        }
    }

    private fun stop_recording() {
        Log.i("PolarPVC2", "Stopping recording")

    }

    private fun clear_device_text() {
        binding.deviceTextView.text = ""
        binding.batteryTextView.text = ""
    }

}