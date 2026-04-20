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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun SearchMap(navController: NavController, viewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val lightMapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.lightmap)
    val darkMapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.darkmap)
    var currentMapStyle by remember { mutableStateOf(lightMapStyle) }
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
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                    val lux = event.values[0]
                    currentMapStyle = if (lux < 2000) darkMapStyle else lightMapStyle
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        viewModel.fetchParkingLots()
    }
    DisposableEffect(Unit) {
        lightSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    val myLocation = remember { LatLng(4.626072, -74.071427) }
    val targetLocation = remember {
        SearchMapLocationHolder.searchedLocation ?: myLocation
    }
    DisposableEffect(Unit) {
        onDispose { SearchMapLocationHolder.searchedLocation = null }
    }

    val defaultLocation = remember {
        SearchMapLocationHolder.searchedLocation ?: LatLng(4.626072, -74.071427)
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

    val nearbyParkingLots = remember(allParkingLots, targetLocation, filtersActive, appliedMaxDistance, appliedMaxPrice, appliedNeedElectric) {
        allParkingLots.map { parking ->
            val results = FloatArray(1)
            // Calculamos la distancia desde el punto que buscaste (targetLocation)
            Location.distanceBetween(
                targetLocation.latitude, targetLocation.longitude,
                parking.location.latitude, parking.location.longitude,
                results
            )
            parking to results[0]
        }
            .filter { item ->
                val dist = item.second
                val parking = item.first
                val price = parking.pricePerHour.replace("$", "").replace(".", "").trim().toFloatOrNull() ?: 0f
                if (filtersActive) {
                    dist <= appliedMaxDistance && price <= appliedMaxPrice && (if (appliedNeedElectric) parking.electricCharges else true)
                } else {
                    dist <= 2000f
                }
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    var selectedForDetails by remember { mutableStateOf<ParkingLot?>(null) }
    var confirmedParkingLot by remember { mutableStateOf<ParkingLot?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<List<LatLng>?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLocation, 16f)
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

    val carBitmap = remember { resizeMapIcon(context,
        if(currentMapStyle==lightMapStyle){ R.drawable.blackcar} else { R.drawable.whitecar}, 35, 70) }
    val pinBitmap = remember { resizeMapIcon(context, if(currentMapStyle==lightMapStyle){ R.drawable.pinmaplogo} else { R.drawable.pinmaplogoblanco}, 45, 45) }

    val infiniteTransition = rememberInfiniteTransition(label = "buscando")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "BuscandoAlpha"
    )

    LaunchedEffect(selectedForDetails) {
        if (selectedForDetails != null) {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
            routePoints = fetchRouteFromGoogle(myLocation, selectedForDetails!!.location, apiKey)
        } else {
            routePoints = null
        }
    }

    LaunchedEffect(routePoints) {
        if (routePoints != null && routePoints!!.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.Builder()
            routePoints!!.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            cameraPositionState.animate(update = CameraUpdateFactory.newLatLngBounds(bounds, 150), durationMs = 1200)
        } else if (selectedForDetails == null) {
            cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(defaultLocation, 16f), durationMs = 1000)
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching && confirmedParkingLot != null) {
            delay(2500)
            isSearching = false
        }
    }

    Box(modifier = Modifier.background(colorResource(R.color.back)).fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties( mapStyleOptions = currentMapStyle,isMyLocationEnabled = false),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false, compassEnabled = true, zoomControlsEnabled = false),
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
                    snippet = "Cupos: ${parking.slot} - Precio: ${parking.pricePerHour}",
                    icon = BitmapDescriptorFactory.fromBitmap(pinBitmap),
                    onClick = { selectedForDetails = parking; false }
                )
            }
            if (routePoints != null) {
                Polyline(points = routePoints!!, color = Color(0xFF0056D2), width = 12f, geodesic = true)
            }
        }

        Button(
            onClick = { showFilters = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 38.dp).size(50.dp),
            shape = RoundedCornerShape(28),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú", tint = Color.Black)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(color = colorResource(R.color.grisClaro), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp)
                .fillMaxWidth()
                .heightIn(max = 450.dp)
        ) {
            if (confirmedParkingLot != null) {
                if (isSearching) {
                    Text(
                        text = buildAnnotatedString {
                            append("Conectando con el ")
                            withStyle(style = SpanStyle(color = colorResource(R.color.blue), fontWeight = FontWeight.Bold)) { append("parqueadero seleccionado") }
                            append("...")
                        },
                        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, modifier = Modifier.alpha(alphaAnim)
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), color = colorResource(R.color.blue))
                } else {
                    Text(text = confirmedParkingLot!!.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "Cupos disponibles: ${confirmedParkingLot!!.slot} | Tarifa: ${confirmedParkingLot!!.pricePerHour}", fontSize = 16.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 8.dp))
                    Button(
                        onClick = {
                            // ← CAMBIO CLAVE: usar ParkingLotHolder en lugar de savedStateHandle
                            ParkingLotHolder.selected = confirmedParkingLot
                            navController.navigate(AppScreens.ParkingLotDetail.name)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(50)
                    ) { Text("Ver Detalles Completos", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                Button(
                    onClick = {
                        confirmedParkingLot = null
                        selectedForDetails = null
                        isSearching = false
                        routePoints = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth().padding(top = if (isSearching) 24.dp else 12.dp),
                    shape = RoundedCornerShape(50)
                ) { Text(text = "Cancelar conexión", color = Color.White, fontWeight = FontWeight.Bold) }

            } else if (selectedForDetails != null) {
                val p = selectedForDetails!!
                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Text(text = p.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Cupos disponibles: ${p.slot}", fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Tarifas:", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "• Por Minuto: ${p.pricePerMin}\n• Por Hora: ${p.pricePerHour}\n• Tarifa Fija: ${p.fixedPrice}", color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Horario:", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "${p.weekAvailability} | ${p.hourStart} - ${p.hourFinish}", color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Carga Eléctrica: ${if (p.electricCharges) "Sí" else "No"}", fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Términos y condiciones:", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = p.terms, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { selectedForDetails = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f)
                        ) { Text("Volver", color = Color.White) }
                        Button(
                            onClick = { confirmedParkingLot = p; isSearching = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Aceptar", color = Color.White) }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Parqueaderos Cercanos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    if (filtersActive) {
                        Text(text = "Filtros Activos", fontSize = 12.sp, color = colorResource(R.color.blue), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (nearbyParkingLots.isEmpty()) {
                    Text("No hay parqueaderos que cumplan tus filtros.", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(nearbyParkingLots) { parking ->
                            val distance = calculateDistance(defaultLocation, parking.location)
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedForDetails = parking },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = parking.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Hora: ${parking.pricePerHour}", fontSize = 14.sp, color = Color.DarkGray)
                                    Text(text = "Distancia: $distance", fontSize = 14.sp, color = colorResource(R.color.blue))
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { navController.navigate(AppScreens.HomeUser.name) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(50)
                ) { Text(text = "Salir", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }

        if (showFilters) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showFilters = false })
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
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text("Filtros", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.align(Alignment.Center))
                    IconButton(onClick = { showFilters = false }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Distancia Máxima: ${tempMaxDistance.toInt()} m", fontWeight = FontWeight.Bold, color = Color.Black)
                Slider(
                    value = tempMaxDistance, onValueChange = { tempMaxDistance = it },
                    valueRange = 500f..5000f, steps = 8,
                    colors = SliderDefaults.colors(thumbColor = colorResource(R.color.blue), activeTrackColor = colorResource(R.color.blue))
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Precio por Hora (Max): $${tempMaxPrice.toInt()}", fontWeight = FontWeight.Bold, color = Color.Black)
                Slider(
                    value = tempMaxPrice, onValueChange = { tempMaxPrice = it },
                    valueRange = 2000f..20000f, steps = 35,
                    colors = SliderDefaults.colors(thumbColor = colorResource(R.color.blue), activeTrackColor = colorResource(R.color.blue))
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Requiere Carga Eléctrica", fontWeight = FontWeight.Bold, color = Color.Black)
                    Switch(
                        checked = tempNeedElectric, onCheckedChange = { tempNeedElectric = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colorResource(R.color.blue))
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
    return if (distanceInMeters > 1000) String.format("%.1f km", distanceInMeters / 1000) else "${distanceInMeters.toInt()} m"
}

fun resizeMapIcon(context: android.content.Context, resId: Int, widthDp: Int, heightDp: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val widthPx = (widthDp * density).toInt()
    val heightPx = (heightDp * density).toInt()
    val imageBitmap = BitmapFactory.decodeResource(context.resources, resId)
    return Bitmap.createScaledBitmap(imageBitmap, widthPx, heightPx, false)
}

suspend fun fetchRouteFromGoogle(origin: LatLng, destination: LatLng, apiKey: String): List<LatLng>? {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=$apiKey"
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
                val polylineEncoded = route.getJSONObject("overview_polyline").getString("points")
                return@withContext PolyUtil.decode(polylineEncoded)
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext null
    }
}