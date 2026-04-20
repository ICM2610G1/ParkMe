package com.example.parkme

import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.parkme.navigation.Navigation

lateinit var sensorManager: SensorManager
var lightSensor: Sensor? = null
lateinit var geocoder: Geocoder
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geocoder = Geocoder(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        enableEdgeToEdge()
        setContent {
            Navigation()
        }
    }
}