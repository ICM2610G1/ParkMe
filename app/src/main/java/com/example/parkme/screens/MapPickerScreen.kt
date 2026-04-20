package com.example.parkme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parkme.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapPickerScreen(
    navController: NavController,
    onLocationPicked: (LatLng) -> Unit
) {
    // Bogotá como centro por defecto
    val defaultLocation = LatLng(4.7110, -74.0721)
    var markerPosition by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
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

        // Instrucción arriba
        Text(
            text = "Toca el mapa para poner el pin",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Black
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
                .padding(24.dp)
                .fillMaxWidth(0.7f)
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