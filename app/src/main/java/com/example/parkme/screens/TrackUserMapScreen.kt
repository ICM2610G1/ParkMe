package com.example.parkme.screens
import android.content.pm.PackageManager
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackUserMapScreen(navController: NavController, chatId: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var clientLocation by remember { mutableStateOf<LatLng?>(null) }
    var parkingLocation by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>?>(null) }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(chatId) {
        db.collection("reservas").document(chatId).get().addOnSuccessListener { resDoc ->
            val userId = resDoc.getString("userId") ?: ""
            val parkingId = resDoc.getString("parkingId") ?: ""

            db.collection("users").document(userId).addSnapshotListener { userDoc, _ ->
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
    }

    LaunchedEffect(clientLocation, parkingLocation) {
        if (clientLocation != null && parkingLocation != null) {
            val applicationInfo = context.packageManager.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
            routePoints = fetchRouteFromGoogle(clientLocation!!, parkingLocation!!, apiKey)
        }
    }

    LaunchedEffect(routePoints) {
        if (!routePoints.isNullOrEmpty()) {
            val boundsBuilder = LatLngBounds.Builder()
            routePoints!!.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 150), durationMs = 1200
            )
        } else if (clientLocation != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(clientLocation!!, 15f), durationMs = 1000
            )
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
                    properties = MapProperties(isMyLocationEnabled = false)
                ) {
                    clientLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Cliente",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
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