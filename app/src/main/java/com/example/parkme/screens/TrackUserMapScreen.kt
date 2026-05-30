package com.example.parkme.screens

import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.parkme.R
import com.example.parkme.lightSensor
import com.example.parkme.sensorManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackUserMapScreen(navController: NavController, chatId: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var clientLocation by remember { mutableStateOf<LatLng?>(null) }
    var parkingLocation by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>?>(null) }

    var lastRouteFetchedLocation by remember { mutableStateOf<LatLng?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    var isDarkMode by remember { mutableStateOf(false) }
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                    val lux = event.values[0]
                    isDarkMode = lux < 2000
                }
            }
        }
    }

    DisposableEffect(Unit) {
        lightSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    val lightMapStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.lightmap) }
    val darkMapStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.darkmap) }
    val currentMapStyle = if (isDarkMode) darkMapStyle else lightMapStyle

    val carRotation = remember(routePoints, clientLocation, parkingLocation) {
        if (clientLocation != null) {
            if (!routePoints.isNullOrEmpty() && routePoints!!.size > 1) {
                SphericalUtil.computeHeading(clientLocation!!, routePoints!![1]).toFloat()
            } else if (parkingLocation != null) {
                SphericalUtil.computeHeading(clientLocation!!, parkingLocation!!).toFloat()
            } else {
                0f
            }
        } else {
            0f
        }
    }

    val carBitmap = remember(isDarkMode) {
        resizeMapIcon(
            context,
            resId = if (isDarkMode) R.drawable.whitecar else R.drawable.blackcar,
            widthDp = 35,
            heightDp = 70
        )
    }

    val bogota = LatLng(4.60971, -74.08175)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 13f)
    }

    val clientMarkerState = rememberMarkerState()

    DisposableEffect(chatId) {
        var userListener: ListenerRegistration? = null

        db.collection("reservas").document(chatId).get().addOnSuccessListener { resDoc ->
            val userId = resDoc.getString("userId") ?: ""
            val parkingId = resDoc.getString("parkingId") ?: ""

            userListener = db.collection("users").document(userId).addSnapshotListener { userDoc, _ ->
                if (userDoc != null && userDoc.exists()) {
                    val lat = userDoc.getDouble("latitude")
                    val lng = userDoc.getDouble("longitude")
                    if (lat != null && lng != null) {
                        clientLocation = LatLng(lat, lng)
                    }
                }
            }

            db.collection("parqueaderos").document(parkingId).get().addOnSuccessListener { parkDoc ->
                val lat = parkDoc.getDouble("latitud")
                val lng = parkDoc.getDouble("longitud")
                if (lat != null && lng != null) {
                    parkingLocation = LatLng(lat, lng)
                }
            }
        }

        onDispose {
            userListener?.remove()
        }
    }

    LaunchedEffect(clientLocation) {
        clientLocation?.let {
            clientMarkerState.position = it
        }
    }

    LaunchedEffect(clientLocation, isMapLoaded) {
        if (isMapLoaded && clientLocation != null) {
            try {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(clientLocation!!, 16f),
                    durationMs = 800
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(clientLocation, parkingLocation) {
        val currentClient = clientLocation
        val currentParking = parkingLocation

        if (currentClient != null && currentParking != null) {
            val lastLocation = lastRouteFetchedLocation

            val debeRecalcularRuta = if (lastLocation == null) {
                true
            } else {
                val resultadoDistancia = FloatArray(1)
                Location.distanceBetween(
                    lastLocation.latitude, lastLocation.longitude,
                    currentClient.latitude, currentClient.longitude,
                    resultadoDistancia
                )
                resultadoDistancia[0] > 30f
            }

            if (debeRecalcularRuta) {
                lastRouteFetchedLocation = currentClient

                try {
                    val applicationInfo = context.packageManager.getApplicationInfo(
                        context.packageName, PackageManager.GET_META_DATA
                    )
                    val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""

                    routePoints = fetchRouteFromGoogle(currentClient, currentParking, apiKey)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruta del Cliente") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (clientLocation == null || parkingLocation == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapStyleOptions = currentMapStyle
                    ),
                    onMapLoaded = { isMapLoaded = true }
                ) {

                    Marker(
                        state = clientMarkerState,
                        title = "Cliente",
                        icon = BitmapDescriptorFactory.fromBitmap(carBitmap),
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        rotation = carRotation
                    )

                    parkingLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Parqueadero",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }

                    if (!routePoints.isNullOrEmpty()) {
                        Polyline(
                            points = routePoints!!, color = Color.Blue, width = 12f, geodesic = true
                        )
                    }
                }
            }
        }
    }
}