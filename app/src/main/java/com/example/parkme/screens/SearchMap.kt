package com.example.parkme.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.parkme.R
import com.example.parkme.lightSensor
import com.example.parkme.models.ParkingLot
import com.example.parkme.models.ParkingLotHolder
import com.example.parkme.navigation.AppScreens
import com.example.parkme.sensorManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.example.parkme.models.SearchMapLocationHolder
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkme.viewmodel.AppViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun formatPriceWithDots(price: String): String {
    val cleanStr = price.replace(Regex("[^\\d]"), "")
    val value = cleanStr.toLongOrNull() ?: return price
    val format = java.text.NumberFormat.getNumberInstance(java.util.Locale("es", "CO"))
    return "$" + format.format(value)
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun SearchMap(navController: NavController, viewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    var currentSpeed by remember { mutableFloatStateOf(0f) }
    var isDarkMode by remember { mutableStateOf(false) }
    val lightMapStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.lightmap) }
    val darkMapStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.darkmap) }
    val currentMapStyle = if (isDarkMode) darkMapStyle else lightMapStyle

    if (!view.isInEditMode) {
        SideEffect {
            val window = context.findActivity()?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

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

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var myLocation by remember { mutableStateOf(LatLng(4.60971, -74.08175)) }

    val targetLocation = SearchMapLocationHolder.searchedLocation ?: myLocation
    val defaultLocation = SearchMapLocationHolder.searchedLocation ?: myLocation

    DisposableEffect(Unit) {
        onDispose { SearchMapLocationHolder.searchedLocation = null }
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLocation, 16f)
    }

    DisposableEffect(hasLocationPermission) {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    myLocation = LatLng(location.latitude, location.longitude)
                    currentSpeed = location.speed * 3.6f
                }
            }
        }

        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        myLocation = LatLng(location.latitude, location.longitude)
                        if (SearchMapLocationHolder.searchedLocation == null) {
                            cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(myLocation, 16f)
                        }
                    }
                }

                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    3000
                ).setMinUpdateDistanceMeters(2f)
                    .build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
                )

            } catch (e: SecurityException) {
                Log.e("MAPS_DEBUG", "Error obteniendo la ubicación por GPS: ${e.message}")
            }
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    val allParkingLots by viewModel.parkingLots.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    var filtersActive by remember { mutableStateOf(false) }
    var tempMaxDistance by remember { mutableStateOf(2000f) }
    var tempMaxPrice by remember { mutableStateOf(10000f) }
    var tempNeedElectric by remember { mutableStateOf(false) }
    var appliedMaxDistance by remember { mutableStateOf(2000f) }
    var appliedMaxPrice by remember { mutableStateOf(10000f) }
    var appliedNeedElectric by remember { mutableStateOf(false) }

    val nearbyParkingLots = remember(
        allParkingLots,
        targetLocation,
        filtersActive,
        appliedMaxDistance,
        appliedMaxPrice,
        appliedNeedElectric
    ) {
        allParkingLots.map { parking ->
            val results = FloatArray(1)
            Location.distanceBetween(
                targetLocation.latitude,
                targetLocation.longitude,
                parking.location.latitude,
                parking.location.longitude,
                results
            )
            parking to results[0]
        }.filter { item ->
            val dist = item.second
            val parking = item.first
            val price =
                parking.pricePerHour.replace("$", "").replace(".", "").trim().toFloatOrNull() ?: 0f
            if (filtersActive) {
                dist <= appliedMaxDistance && price <= appliedMaxPrice && (if (appliedNeedElectric) parking.electricCharges else true)
            } else {
                dist <= 2000f
            }
        }.sortedBy { it.second }.map { it.first }
    }

    var selectedForDetails by remember { mutableStateOf<ParkingLot?>(null) }
    var confirmedParkingLot by remember { mutableStateOf<ParkingLot?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<List<LatLng>?>(null) }

    LaunchedEffect(allParkingLots) {
        val preSelectedId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("preSelectedParkingId")
        val loc = SearchMapLocationHolder.searchedLocation

        if (preSelectedId != null && allParkingLots.isNotEmpty()) {
            val found = allParkingLots.find { it.id == preSelectedId }
            if (found != null && selectedForDetails == null) {
                selectedForDetails = found

                filtersActive = true
                appliedMaxDistance = 50000f
                tempMaxDistance = 5000f
                appliedMaxPrice = 100000f
                tempMaxPrice = 20000f
                appliedNeedElectric = false
                tempNeedElectric = false

                navController.previousBackStackEntry?.savedStateHandle?.remove<String>("preSelectedParkingId")
            }
        } else if (loc != null && allParkingLots.isNotEmpty()) {
            val foundLoc = allParkingLots.find {
                it.location.latitude == loc.latitude && it.location.longitude == loc.longitude
            }
            if (foundLoc != null && selectedForDetails == null) {
                selectedForDetails = foundLoc
            }
        }
    }

    BackHandler(enabled = showFilters || confirmedParkingLot != null || selectedForDetails != null) {
        when {
            showFilters -> {
                showFilters = false
            }
            confirmedParkingLot != null -> {
                confirmedParkingLot = null
                isSearching = false
            }
            selectedForDetails != null -> {
                selectedForDetails = null
                routePoints = null
                SearchMapLocationHolder.searchedLocation = null
            }
        }
    }
    var licensePlate by remember { mutableStateOf("") }
    var entryTime by remember { mutableStateOf("") }
    var exitTime by remember { mutableStateOf("") }

    val calendar = remember { java.util.Calendar.getInstance() }
    val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(java.util.Calendar.MINUTE)

    DisposableEffect(Unit) {
        onDispose {
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>("preSelectedParkingId")
        }
    }

    val carRotation = remember(routePoints, selectedForDetails) {
        if (!routePoints.isNullOrEmpty() && routePoints!!.size > 1) {
            SphericalUtil.computeHeading(myLocation, routePoints!![1]).toFloat()
        } else if (selectedForDetails != null) {
            SphericalUtil.computeHeading(myLocation, selectedForDetails!!.location).toFloat()
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

    val infiniteTransition = rememberInfiniteTransition(label = "buscando")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "BuscandoAlpha"
    )

    LaunchedEffect(selectedForDetails, myLocation) {
        if (selectedForDetails != null) {
            val applicationInfo = context.packageManager.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
            routePoints = fetchRouteFromGoogle(myLocation, selectedForDetails!!.location, apiKey)
        } else {
            routePoints = null
        }
    }

    LaunchedEffect(routePoints, confirmedParkingLot, myLocation) {
        if (confirmedParkingLot != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(myLocation, 17.5f),
                durationMs = 1000
            )
        } else if (routePoints != null && routePoints!!.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.Builder()
            routePoints!!.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 150),
                durationMs = 1200
            )
        } else if (selectedForDetails == null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(defaultLocation, 16f),
                durationMs = 1000
            )
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching && confirmedParkingLot != null) {
            delay(2500)
            isSearching = false
        }
    }
    LaunchedEffect(Unit) {
        viewModel.fetchParkingLots()
    }
    DisposableEffect(Unit) {
        lightSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    Box(
        modifier = Modifier
            .background(colorResource(R.color.back))
            .fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = currentMapStyle, isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false, compassEnabled = true, zoomControlsEnabled = false
            ),
            contentPadding = PaddingValues(top = 90.dp, bottom = 460.dp, start = 8.dp, end = 8.dp)
        ) {
            Marker(
                state = MarkerState(position = myLocation),
                title = "Mi Ubicación",
                icon = BitmapDescriptorFactory.fromBitmap(carBitmap),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                rotation = carRotation
            )
            nearbyParkingLots.forEach { parking ->
                Marker(
                    state = MarkerState(position = parking.location),
                    title = parking.name,
                    snippet = "Cupos: ${parking.slot} - Precio: ${formatPriceWithDots(parking.pricePerHour)}",
                    icon = BitmapDescriptorFactory.fromBitmap(pinBitmap),
                    onClick = { selectedForDetails = parking; false })
            }
            if (routePoints != null) {
                Polyline(
                    points = routePoints!!,
                    color = colorResource(R.color.azulruta),
                    width = 12f
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, bottom = 510.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(65.dp)
                    .align(Alignment.BottomStart),
                shape = RoundedCornerShape(50),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(
                    width = 3.dp,
                    color = if (currentSpeed > 60) Color.Red else colorResource(R.color.blue)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentSpeed.toInt().toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Text(
                        text = "km/h",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }

        Button(
            onClick = { showFilters = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 38.dp)
                .size(50.dp),
            shape = RoundedCornerShape(28),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú", tint = Color.Black)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    color = colorResource(R.color.grisClaro),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .imePadding()
                .padding(24.dp)
                .fillMaxWidth()
                .heightIn(max = 450.dp)
        ) {
            if (confirmedParkingLot != null) {
                if (isSearching) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = colorResource(R.color.blue),
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("Conectando con\n")
                                withStyle(
                                    style = SpanStyle(
                                        color = colorResource(R.color.blue),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp
                                    )
                                ) { append(confirmedParkingLot!!.name) }
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(alphaAnim)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = confirmedParkingLot!!.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.blue).copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Cupos: ${confirmedParkingLot!!.slot}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = colorResource(R.color.blue),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = formatPriceWithDots(confirmedParkingLot!!.pricePerHour),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = licensePlate,
                            onValueChange = { newValue ->
                                var text = newValue.uppercase().replace(Regex("[^A-Z0-9]"), "")
                                var formatted = ""

                                for (i in text.indices) {
                                    if (i < 3) {
                                        if (text[i].isLetter()) formatted += text[i]
                                    } else if (i < 6) {
                                        if (i == 3) formatted += "-"
                                        if (text[i].isDigit()) formatted += text[i]
                                    }
                                }
                                licensePlate = formatted
                            },
                            label = { Text("Placa (ej. ABC-123)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(R.color.blue),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color(0xFFF9F9F9),
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = entryTime,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Entry Time") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorResource(R.color.blue),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        focusedContainerColor = Color(0xFFF9F9F9),
                                        unfocusedContainerColor = Color(0xFFF9F9F9)
                                    )
                                )
                                Box(modifier = Modifier
                                    .matchParentSize()
                                    .clickable {
                                        val rightNow = java.util.Calendar.getInstance()
                                        val realCurrentHour = rightNow.get(java.util.Calendar.HOUR_OF_DAY)
                                        val realCurrentMinute = rightNow.get(java.util.Calendar.MINUTE)

                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minuteOfHour ->

                                                if (hourOfDay < realCurrentHour || (hourOfDay == realCurrentHour && minuteOfHour < realCurrentMinute)) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "La hora de entrada no puede ser en el pasado",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    entryTime = String.format(
                                                        java.util.Locale.getDefault(),
                                                        "%02d:%02d",
                                                        hourOfDay,
                                                        minuteOfHour
                                                    )

                                                    if (exitTime.isNotBlank()) {
                                                        val eParts = entryTime.split(":")
                                                        val exParts = exitTime.split(":")
                                                        val eMins = eParts[0].toInt() * 60 + eParts[1].toInt()
                                                        val exMins = exParts[0].toInt() * 60 + exParts[1].toInt()

                                                        if (exMins <= eMins) {
                                                            exitTime = ""
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Hora de salida reiniciada por ser menor a la entrada",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            },
                                            realCurrentHour,
                                            realCurrentMinute,
                                            false
                                        ).show()
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = exitTime,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Exit Time") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorResource(R.color.blue),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        focusedContainerColor = Color(0xFFF9F9F9),
                                        unfocusedContainerColor = Color(0xFFF9F9F9)
                                    )
                                )
                                Box(modifier = Modifier
                                    .matchParentSize()
                                    .clickable {
                                        if (entryTime.isBlank()) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Por favor selecciona primero la hora de entrada",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            return@clickable
                                        }

                                        val eParts = entryTime.split(":")
                                        val defaultHour = eParts[0].toInt()
                                        val defaultMinute = eParts[1].toInt()

                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minuteOfHour ->
                                                val selectedMins = hourOfDay * 60 + minuteOfHour
                                                val eMins = defaultHour * 60 + defaultMinute

                                                if (selectedMins <= eMins) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "La hora de salida debe ser mayor a la hora de entrada",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    exitTime = String.format(
                                                        java.util.Locale.getDefault(),
                                                        "%02d:%02d",
                                                        hourOfDay,
                                                        minuteOfHour
                                                    )
                                                }
                                            },
                                            defaultHour,
                                            defaultMinute,
                                            false
                                        ).show()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.createReservation(
                                    parking = confirmedParkingLot!!,
                                    licensePlate = licensePlate,
                                    startTime = entryTime,
                                    endTime = exitTime,
                                    onSuccess = {
                                        android.widget.Toast.makeText(context, "¡Reserva confirmada exitosamente!", android.widget.Toast.LENGTH_LONG).show()

                                        confirmedParkingLot = null
                                        selectedForDetails = null
                                        licensePlate = ""
                                        entryTime = ""
                                        exitTime = ""
                                        isSearching = false

                                        navController.navigate(AppScreens.HomeUser.name)
                                    },
                                    onFailure = { error ->
                                        android.widget.Toast.makeText(context, "Error al reservar: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            enabled = licensePlate.isNotBlank() && entryTime.isNotBlank() && exitTime.isNotBlank()
                        ) {
                            Text("Confirm Reservation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = {
                        confirmedParkingLot = null
                        selectedForDetails = null
                        isSearching = false
                        routePoints = null
                        licensePlate = ""
                        entryTime = ""
                        exitTime = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(text = "Cancelar conexión", color = Color.Red, fontWeight = FontWeight.Bold)
                }

            } else if (selectedForDetails != null) {
                val p = selectedForDetails!!

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (p.slot > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (p.slot > 0) "${p.slot} Cupos" else "Lleno",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (p.slot > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Tarifas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PriceCard(title = "Minuto", price = formatPriceWithDots(p.pricePerMin), modifier = Modifier.weight(1f))
                        PriceCard(title = "Hora", price = formatPriceWithDots(p.pricePerHour), modifier = Modifier.weight(1f))
                        PriceCard(title = "Fija", price = formatPriceWithDots(p.fixedPrice), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Disponibilidad", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.width(75.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(text = "Horario", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = p.hourStart,
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "a ${p.hourFinish}",
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(1.dp)
                                    .background(Color(0xFFE0E0E0))
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(text = "Días de servicio", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val diasList = p.weekAvailability.split(",").filter { it.isNotBlank() }
                                    diasList.take(7).forEach { dia ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(colorResource(R.color.blue).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dia.trim().uppercase().take(1),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colorResource(R.color.blue)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Detalles del lugar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(16.dp))

                    RowInfo(
                        icon = if (p.slot > 0) Icons.Default.Check else Icons.Default.Close,
                        title = "Capacidad Total",
                        detail = if (p.slot > 0) "${p.slot} espacios" else "Lleno / Sin servicio",
                        iconColor = if (p.slot > 0) Color.Black else Color.Red
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    RowInfo(
                        icon = if (p.electricCharges) Icons.Default.Check else Icons.Default.Close,
                        title = "Carga Eléctrica",
                        detail = if (p.electricCharges) "Estación disponible" else "No disponible",
                        iconColor = if (p.electricCharges) colorResource(R.color.blue) else Color.Red
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Reglas del parqueadero", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    val rulesList = if (p.terms.isNotBlank()) p.terms.split("\n", ". ").filter { it.isNotBlank() } else listOf("Sin reglas definidas.")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.blue).copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, colorResource(R.color.blue).copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rulesList.forEach { rule ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        tint = colorResource(R.color.blue),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rule.trim().replaceFirstChar { it.uppercase() },
                                        fontSize = 14.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Fotos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)

                        TextButton(
                            onClick = {
                                ParkingLotHolder.selected = p
                                navController.navigate(AppScreens.ParkingLotDetail.name)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Ver todo", color = colorResource(R.color.blue), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        val previewPhotos = p.photos.take(3)

                        if (previewPhotos.isEmpty()) {
                            repeat(3) {
                                Image(
                                    painter = painterResource(id = R.drawable.default1),
                                    contentDescription = "Sin foto",
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            previewPhotos.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Foto parqueadero",
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (previewPhotos.size < 3) {
                                repeat(3 - previewPhotos.size) {
                                    Spacer(modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { selectedForDetails = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(50)
                        ) { Text("Volver", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                        Button(
                            onClick = { confirmedParkingLot = p; isSearching = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp),
                            shape = RoundedCornerShape(50)
                        ) { Text("Aceptar y Conectar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Parqueaderos Cercanos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (filtersActive) {
                        Text(
                            text = "Filtros Activos",
                            fontSize = 12.sp,
                            color = colorResource(R.color.blue),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (nearbyParkingLots.isEmpty()) {
                    Text(
                        "No hay parqueaderos que cumplan tus filtros.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(nearbyParkingLots) { parking ->
                            val distance = calculateDistance(defaultLocation, parking.location)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedForDetails = parking },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = parking.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Hora: ${formatPriceWithDots(parking.pricePerHour)}",
                                        fontSize = 14.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "Distancia: $distance",
                                        fontSize = 14.sp,
                                        color = colorResource(R.color.blue)
                                    )
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { SearchMapLocationHolder.searchedLocation = null
                        navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(50)
                ) { Text(text = "Salir", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }

        if (showFilters) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showFilters = false })
        }

        AnimatedVisibility(
            visible = showFilters,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f)
                    .background(Color.White)
                    .padding(top = 64.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        "Filtros",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = { showFilters = false },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Distancia Máxima: ${tempMaxDistance.toInt()} m",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Slider(
                    value = tempMaxDistance,
                    onValueChange = { tempMaxDistance = it },
                    valueRange = 500f..5000f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = colorResource(R.color.blue),
                        activeTrackColor = colorResource(R.color.blue)
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))

                val format = java.text.NumberFormat.getNumberInstance(java.util.Locale("es", "CO"))
                Text(
                    "Precio por Hora (Max): $${format.format(tempMaxPrice.toInt())}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Slider(
                    value = tempMaxPrice,
                    onValueChange = { tempMaxPrice = it },
                    valueRange = 2000f..20000f,
                    steps = 35,
                    colors = SliderDefaults.colors(
                        thumbColor = colorResource(R.color.blue),
                        activeTrackColor = colorResource(R.color.blue)
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Requiere Carga Eléctrica",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Switch(
                        checked = tempNeedElectric,
                        onCheckedChange = { tempNeedElectric = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colorResource(R.color.blue)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            filtersActive = false
                            tempMaxDistance = 2000f
                            tempMaxPrice = 10000f
                            tempNeedElectric = false
                            showFilters = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        modifier = Modifier.weight(1f)
                    ) { Text("Limpiar", color = Color.Black) }
                    Button(
                        onClick = {
                            appliedMaxDistance = tempMaxDistance
                            appliedMaxPrice = tempMaxPrice
                            appliedNeedElectric = tempNeedElectric
                            filtersActive = true
                            showFilters = false
                            selectedForDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Aplicar", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

fun calculateDistance(start: LatLng, end: LatLng): String {
    val results = FloatArray(1)
    Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
    val distanceInMeters = results[0]
    return if (distanceInMeters > 1000) String.format(
        "%.1f km", distanceInMeters / 1000
    ) else "${distanceInMeters.toInt()} m"
}

fun resizeMapIcon(
    context: android.content.Context, resId: Int, widthDp: Int, heightDp: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val widthPx = (widthDp * density).toInt()
    val heightPx = (heightDp * density).toInt()
    val imageBitmap = BitmapFactory.decodeResource(context.resources, resId)
    return Bitmap.createScaledBitmap(imageBitmap, widthPx, heightPx, false)
}

suspend fun fetchRouteFromGoogle(
    origin: LatLng, destination: LatLng, apiKey: String
): List<LatLng>? {
    return withContext(Dispatchers.IO) {
        try {

            val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&mode=driving&key=$apiKey"
            val response = java.net.URL(url).readText()
            val jsonObject = JSONObject(response)
            val status = jsonObject.getString("status")

            if (status != "OK") {
                Log.e("MAPS_DEBUG", "Error de API: ${jsonObject.optString("error_message")}")
                return@withContext null
            }

            val routesArray = jsonObject.getJSONArray("routes")
            if (routesArray.length() > 0) {
                val route = routesArray.getJSONObject(0)
                val legsArray = route.getJSONArray("legs")
                val detailedPath = mutableListOf<LatLng>()

                for (i in 0 until legsArray.length()) {
                    val leg = legsArray.getJSONObject(i)
                    val stepsArray = leg.getJSONArray("steps")

                    for (j in 0 until stepsArray.length()) {
                        val step = stepsArray.getJSONObject(j)
                        val polylineEncoded = step.getJSONObject("polyline").getString("points")
                        detailedPath.addAll(PolyUtil.decode(polylineEncoded))
                    }
                }
                return@withContext detailedPath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}


@Composable
fun PriceCard(title: String, price: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = price, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}

@Composable
fun RowInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = detail, fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.ExtraBold)
        }
    }
}