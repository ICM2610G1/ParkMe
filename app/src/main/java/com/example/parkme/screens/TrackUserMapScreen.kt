package com.example.parkme.screens

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.parkme.R
import com.example.parkme.lightSensor
import com.example.parkme.sensorManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackUserMapScreen(navController: NavController, chatId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    var clientLocation by remember { mutableStateOf<LatLng?>(null) }
    var parkingLocation by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>?>(null) }
    var lastRouteFetchedLocation by remember { mutableStateOf<LatLng?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(false) }

    var isCurrentUserTheDriver by remember { mutableStateOf(false) }

    var hasArrivalAlertShown by remember { mutableStateOf(false) }
    var showArrivalUI by remember { mutableStateOf(false) }
    val distanceThreshold = 50f

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

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

    LaunchedEffect(showArrivalUI) {
        if (showArrivalUI) {
            delay(15000)
            showArrivalUI = false
        }
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

    val pinBitmap = remember(isDarkMode) {
        resizeMapIcon(
            context,
            resId = if (isDarkMode) R.drawable.pinmaplogoblanco else R.drawable.pinmaplogo,
            widthDp = 45,
            heightDp = 45
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

            isCurrentUserTheDriver = (currentUid == userId)

            if (!isCurrentUserTheDriver) {
                userListener = db.collection("users").document(userId).addSnapshotListener { userDoc, _ ->
                    if (userDoc != null && userDoc.exists()) {
                        val lat = userDoc.getDouble("latitude")
                        val lng = userDoc.getDouble("longitude")
                        if (lat != null && lng != null) {
                            clientLocation = LatLng(lat, lng)
                        }
                    }
                }
            }

            db.collection("parking lots").document(parkingId).get().addOnSuccessListener { parkDoc ->
                val lat = parkDoc.getDouble("latitude")
                val lng = parkDoc.getDouble("longitude")
                if (lat != null && lng != null) {
                    parkingLocation = LatLng(lat, lng)
                } else {
                    Log.e("MAPS_DEBUG", "No se pudieron leer las coordenadas del parqueadero")
                }
            }
        }

        onDispose {
            userListener?.remove()
        }
    }

    DisposableEffect(isCurrentUserTheDriver) {
        var locationCallback: LocationCallback? = null

        if (isCurrentUserTheDriver) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        val newLatLng = LatLng(location.latitude, location.longitude)
                        clientLocation = newLatLng

                        parkingLocation?.let { destino ->
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                location.latitude, location.longitude,
                                destino.latitude, destino.longitude,
                                results
                            )
                            val distanceInMeters = results[0]

                            if (distanceInMeters <= distanceThreshold && !hasArrivalAlertShown) {
                                hasArrivalAlertShown = true
                                showArrivalUI = true
                            }

                            if (distanceInMeters > 100f) {
                                hasArrivalAlertShown = false
                            }
                        }

                        db.collection("users").document(currentUid).update(
                            mapOf(
                                "latitude" to location.latitude,
                                "longitude" to location.longitude
                            )
                        ).addOnFailureListener {
                            Log.e("MAPS_DEBUG", "Error actualizando ubicación en Firebase")
                        }
                    }
                }
            }

            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPermission) {
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    3000
                ).setMinUpdateDistanceMeters(2f).build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
                )
            }
        }

        onDispose {
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        }
    }

    LaunchedEffect(clientLocation) {
        clientLocation?.let {
            clientMarkerState.position = it
        }
    }

    LaunchedEffect(clientLocation, routePoints, isMapLoaded) {
        if (isMapLoaded) {
            try {
                if (!routePoints.isNullOrEmpty()) {
                    val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    routePoints!!.forEach { boundsBuilder.include(it) }

                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150),
                        durationMs = 800
                    )
                } else if (clientLocation != null) {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(clientLocation!!, 16f),
                        durationMs = 800
                    )
                }
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
                title = { Text(if (isCurrentUserTheDriver) "Mi Ruta al Parqueadero" else "Ruta del Cliente") },
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
                        title = if (isCurrentUserTheDriver) "Mi Ubicación" else "Cliente",
                        icon = BitmapDescriptorFactory.fromBitmap(carBitmap),
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        rotation = carRotation
                    )

                    parkingLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Parqueadero",
                            icon = BitmapDescriptorFactory.fromBitmap(pinBitmap)
                        )
                    }

                    if (!routePoints.isNullOrEmpty()) {
                        Polyline(
                            points = routePoints!!,
                            color = colorResource(R.color.azulruta),
                            width = 12f
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showArrivalUI && isCurrentUserTheDriver,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .zIndex(10f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colorResource(R.color.blue),
                        contentColor = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Llegada",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "¡Estás llegando a tu destino!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}