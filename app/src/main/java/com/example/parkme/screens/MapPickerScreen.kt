package com.example.parkme.screens

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parkme.R
import com.example.parkme.geocoder
import com.example.parkme.lightSensor
import com.example.parkme.sensorManager
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun MapPickerScreen(navController: NavController, onLocationPicked: (LatLng) -> Unit) {
    val context = LocalContext.current

    val lightMapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.lightmap)
    val darkMapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.darkmap)
    var currentMapStyle by remember { mutableStateOf(lightMapStyle) }

    val defaultLocation = LatLng(4.7110, -74.0721)
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var searchText by remember { mutableStateOf("") }

    var suggestions by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false, compassEnabled = true
            )
        )
    }

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

    DisposableEffect(Unit) {
        lightSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        onDispose { sensorManager.unregisterListener(sensorListener) }
    }


    LaunchedEffect(searchText) {
        if (searchText.length > 3) {
            delay(500)
            withContext(Dispatchers.IO) {
                try {
                    val results = geocoder.getFromLocationName(
                        searchText, 5, -4.22, -79.27, 12.59, -66.86
                    )
                    suggestions = results ?: emptyList()
                    isDropdownExpanded = suggestions.isNotEmpty()
                } catch (e: Exception) {
                    suggestions = emptyList()
                }
            }
        } else {
            suggestions = emptyList()
            isDropdownExpanded = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapStyleOptions = currentMapStyle),
            uiSettings = uiSettings,
            contentPadding = PaddingValues(top = 120.dp, bottom = 100.dp),
            onMapClick = { latLng ->
                markerPosition = latLng
            }) {
            markerPosition?.let { pos ->
                Marker(
                    state = MarkerState(position = pos), title = "Parqueadero aquí"
                )
            }
        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar dirección o presionar en el mapa") },
                shape = RoundedCornerShape(30.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorResource(R.color.grisClaro),
                    unfocusedContainerColor = colorResource(R.color.grisClaro),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Gray,
                    focusedLabelColor = Color.Gray,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        try {
                            val addresses = findLocation(searchText)
                            addresses?.let {
                                markerPosition = addresses
                                cameraPositionState.position =
                                    CameraPosition.fromLatLngZoom(addresses, 16f)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error buscando dirección", Toast.LENGTH_SHORT)
                                .show()
                        }
                    })
            )

            AnimatedVisibility(visible = isDropdownExpanded && suggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        suggestions.forEachIndexed { index, address ->
                            val placeName = address.featureName ?: "Dirección"
                            val fullAddress = address.getAddressLine(0) ?: ""

                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchText = fullAddress
                                    isDropdownExpanded = false
                                    val latLng = LatLng(address.latitude, address.longitude)
                                    markerPosition = latLng
                                    cameraPositionState.position =
                                        CameraPosition.fromLatLngZoom(latLng, 16f)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Ubicación",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = placeName,
                                        color = Color.Black,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = fullAddress,
                                        color = Color.DarkGray,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (index < suggestions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        horizontal = 56.dp, vertical = 4.dp
                                    ), color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                markerPosition?.let { latLng ->
                    onLocationPicked(latLng)
                    navController.popBackStack()
                }
            },
            enabled = markerPosition != null,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.blue), disabledContainerColor = Color.Gray
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(30.dp)
                .height(52.dp)
        ) {
            Text(
                if (markerPosition != null) "Confirmar ubicación" else "Toca el mapa primero",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

fun findLocation(address: String): LatLng? {
    val addresses = geocoder.getFromLocationName(address, 2)
    if (addresses != null && !addresses.isEmpty()) {
        val addr = addresses.get(0)
        val location = LatLng(addr.latitude, addr.longitude)
        return location
    }
    return null
}

@Preview(showBackground = true)
@Composable
fun PreviewPiccker() {
    val navControllerv: NavController
    navControllerv = rememberNavController()
    MapPickerScreen(navControllerv) { }
}