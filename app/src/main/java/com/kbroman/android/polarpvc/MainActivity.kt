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




        binding.connectSwitch.setOnCheckedChangeListener {
            buttonView, isChecked ->
            if(isChecked) {
                Log.e("PolarPVC2", "Connect checked")

            } else {
                Log.e("PolarPVC2", "Connect unchecked")

            }
        }

        binding.recordSwitch.setOnCheckedChangeListener {
            buttonView, isChecked ->
            if(isChecked) {
                Log.e("PolarPVC2", "Record checked")

            } else {
                Log.e("PolarPVC2", "Record unchecked")

            }
        }

    }

}