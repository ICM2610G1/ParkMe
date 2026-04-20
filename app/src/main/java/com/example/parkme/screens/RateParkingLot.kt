package com.example.parkme.screens

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkme.lightSensor
import com.example.parkme.sensorManager
import com.example.parkme.viewmodel.AppViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

@SuppressLint("UnrememberedMutableState")
@Composable
fun RateParkingLot(navController: NavController,parkingLotId: String,viewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    var targetLocation by remember { mutableStateOf<LatLng?>(null) }
    val lightMapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.lightmap)
    val darkMapStyle = MapStyleOptions.loadRawResourceStyle(context, R.raw.darkmap)
    var currentMapStyle by remember { mutableStateOf(lightMapStyle) }

    LaunchedEffect(parkingLotId) {
        if (parkingLotId.isNotEmpty()) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("parqueaderos").document(parkingLotId).get().await()
                if (doc.exists()) {
                    val lat = doc.getDouble("latitud") ?: 4.6097
                    val lng = doc.getDouble("longitud") ?: -74.0817
                    targetLocation = LatLng(lat, lng)
                }
            } catch (e: Exception) {
                targetLocation = LatLng(4.6097, -74.0817) // Coordenada por defecto si falla
            }
        }
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLocation ?: LatLng(4.6097, -74.0817), 17f)
    }

    LaunchedEffect(targetLocation) {
        targetLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 17f)
        }
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
    val pinBitmap = remember { resizeMapIcon(context, if(currentMapStyle==lightMapStyle){ R.drawable.pinmaplogo} else { R.drawable.pinmaplogoblanco}, 45, 45) }

    Box(modifier = Modifier
        .background(colorResource(R.color.back))
        .fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties( mapStyleOptions = currentMapStyle),
            uiSettings = MapUiSettings(
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = false
            )
        ) {
            targetLocation?.let {
                Marker(
                    state = MarkerState(it),
                    icon = BitmapDescriptorFactory.fromBitmap(pinBitmap),
                    title = "Parqueadero"
                )
            }
        }

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 38.dp)
                .size(50.dp),
            shape = RoundedCornerShape(28),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Atras",
                tint = Color.Black
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.grisClaro),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "¿Qué tal fue tu experiencia con el Operador?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = Color.Gray
            )
            var currentRating by remember { mutableIntStateOf(0) }
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..5) {
                    val isSelected = i <= currentRating
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Estrella de calificación $i",
                        tint = if (isSelected) Color(0xFFFFC107) else Color.Black, // Amarillo o negro
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                currentRating = i
                            }
                    )
                }
            }

            Text(
                text = "Calificar ayuda a otros usuarios.",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            Button(
                onClick = {
                    if (currentRating > 0) {
                        viewModel.rateParkingLot(parkingLotId, currentRating.toFloat())
                        Toast.makeText(context, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show()
                        navController.navigate(AppScreens.HomeUser.name) {
                            popUpTo(AppScreens.HomeUser.name) { inclusive = true }
                        }
                    } else {
                        Toast.makeText(context, "Por favor selecciona al menos 1 estrella", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.blue)),
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(bottom = 16.dp)
            ) {
                Text(text = "Calificar", color = Color.White, fontWeight = FontWeight.Bold)
            }


            Text(
                text = "Ayuda",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 12.dp)
            )

            HelpOptionButton(text = "¿Se te perdió un objeto?", hasArrow = true)
            HelpOptionButton(text = "Me cobraron más de lo debido", hasArrow = true)
            HelpOptionButton(text = "El arrendador no ofrecía lo listado", hasArrow = true)
        }
    }
}

@Composable
fun HelpOptionButton(text: String, hasArrow: Boolean) {
    val context = LocalContext.current
    Button(
        onClick = {
            Toast.makeText(context, "Mandar correo a: parkme.company@gmail.com", Toast.LENGTH_LONG).show()
        },
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.grisB)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .height(50.dp),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (hasArrow) Arrangement.SpaceBetween else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasArrow) Text("")

            Text(
                text = text,
                color = Color.Black,
                fontSize = 14.sp
            )

            if (hasArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Ir",
                    tint = Color.Black
                )
            }
        }
    }
}
