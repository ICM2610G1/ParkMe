package com.example.parkme

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.parkme.navigation.Navigation
import com.example.parkme.utils.ShakeDetector

lateinit var sensorManager: SensorManager
var lightSensor: Sensor? = null
lateinit var geocoder: Geocoder
var accelerometer: Sensor? = null
lateinit var shakeDetector: ShakeDetector

class MainActivity : FragmentActivity() {

    private var canAuthenticate = false
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.d("FCM", "Permiso de notificaciones concedido por el usuario.")
                } else {
                    Log.w("FCM", "Permiso de notificaciones denegado.")
                }
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        geocoder = Geocoder(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        shakeDetector = ShakeDetector { simulateEmergencyCall() }

        setupAuth()

        setContent {
            Navigation()
        }
    }

    private fun setupAuth() {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                canAuthenticate = true
                promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Iniciar sesión en ParkMe")
                    .setSubtitle("Usa tu huella, Face ID o PIN")
                    .setAllowedAuthenticators(authenticators)
                    .build()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, authenticators)
                }
                startActivityForResult(enrollIntent, 100)
            }
            else -> { canAuthenticate = false }
        }
    }

    fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!canAuthenticate) {
            setupAuth()
            if (!canAuthenticate) {
                onError("Biometría no configurada en este dispositivo")
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError("Autenticación cancelada o fallida")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun simulateEmergencyCall() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:123")
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }
}