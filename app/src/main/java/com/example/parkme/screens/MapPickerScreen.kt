package com.example.parkme.screens

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

@Composable
fun MapPickerScreen(navController: NavController, onLocationPicked: (LatLng) -> Unit) {
    val context = LocalContext.current

    val lightMapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.lightmap)
    val darkMapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.darkmap)
    var currentMapStyle by remember { mutableStateOf(lightMapStyle) }

    val defaultLocation = LatLng(4.7110, -74.0721)
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }
    var searchText by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    // << CAMBIO 1: Se crea un objeto para configurar la UI del mapa
    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false, // Se ocultan los botones de zoom (+/-)
                compassEnabled = true
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

    // El Column vacío se puede eliminar, ya que el Box lo contiene todo.
    // Column(
    //     modifier = Modifier.fillMaxSize(),
    // ) {}

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapStyleOptions = currentMapStyle),
            uiSettings = uiSettings, // << CAMBIO 1: Se aplican los ajustes de UI
            // << CAMBIO 2: Se añade padding para bajar la brújula y el logo de Google
            contentPadding = PaddingValues(top = 120.dp, bottom = 100.dp),
            onMapClick = { latLng ->
                markerPosition = latLng
            }
        ) {
            markerPosition?.let { pos ->
                Marker(
                    state = MarkerState(position = pos),
                    title = "Parqueadero aquí"
                )
            }
        }
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp) // << CAMBIO 3: La barra de búsqueda ahora está más arriba (80dp -> 50dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter),
            placeholder = { Text("Buscar dirección o Presionar en el Mapa") }, // << CAMBIO: Usamos placeholder en lugar de label para mejor estética
            shape = RoundedCornerShape(30.dp), // << CAMBIO 4: Bordes más redondeados (12dp -> 30dp)
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colorResource(R.color.grisClaro),
                unfocusedContainerColor = colorResource(R.color.grisClaro),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.Gray,
                focusedLabelColor = Color.Gray, // Label ya no se usa, pero lo dejamos por si acaso
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
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(addresses, 16f)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error buscando dirección", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        )

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
                containerColor = colorResource(R.color.blue),
                disabledContainerColor = Color.Gray
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