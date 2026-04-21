package com.example.parkme

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.compose.setContent
import com.example.parkme.navigation.Navigation
import android.net.Uri
import androidx.activity.ComponentActivity
import com.example.parkme.utils.ShakeDetector

lateinit var sensorManager: SensorManager
var lightSensor: Sensor? = null
lateinit var geocoder: Geocoder
var accelerometer: Sensor? = null
lateinit var shakeDetector: ShakeDetector
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geocoder = Geocoder(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        shakeDetector = ShakeDetector {
            simulateEmergencyCall()
        }

        setContent {
            Navigation()
        }
    }


    private fun simulateEmergencyCall() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:123")
        }
        startActivity(intent)
    }


    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }
}