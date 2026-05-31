package com.example.parkme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.models.ParkingLot
import com.example.parkme.models.Reservation
import com.example.parkme.navigation.AppScreens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ParkingLotDetail(navController: NavController, parking: ParkingLot) {
    var showForm by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var reservationMessage by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.back)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .padding(bottom = if (showForm) 380.dp else 100.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logoparkme),
                        contentDescription = "Logo de la app",
                        modifier = Modifier.width(130.dp).height(80.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.height(60.dp).width(2.dp).background(Color.Black))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Detalles del\nparqueadero",
                        color = colorResource(R.color.black),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = parking.pricePerHour,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.black)
                        )
                        Text(text = "por hora", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "${parking.pricePerMin} / min", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.black))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Plena: ${parking.fixedPrice}", fontSize = 14.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Horario de atención", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${parking.hourStart} - ${parking.hourFinish}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.black))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Días: ${parking.weekAvailability}", fontSize = 14.sp, color = Color.DarkGray)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                Text("Detalles del lugar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.black))
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Box(Modifier.size(24.dp).border(1.dp, if(parking.slot > 0) Color.Black else Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                        if (parking.slot > 0) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        else Icon(Icons.Default.Cancel, null, Modifier.size(16.dp), tint = Color.Red)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (parking.slot > 0) "Capacidad total: ${parking.slot} espacios" else "Sin servicio", fontSize = 16.sp, color = Color.DarkGray)
                }

                if (parking.electricCharges) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Box(Modifier.size(24.dp).border(1.dp, Color.Black, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cargador para vehículos eléctricos", fontSize = 16.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Reglas del parqueadero", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorResource(R.color.black))
                Spacer(modifier = Modifier.height(12.dp))

                val rulesList = if (parking.terms.isNotBlank()) {
                    parking.terms.split("\n", ". ").filter { it.isNotBlank() }
                } else {
                    listOf("No hay reglas específicas definidas para este lugar.")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.blue).copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, colorResource(R.color.blue).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rulesList.forEach { rule ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Regla",
                                    tint = colorResource(R.color.blue),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = rule.trim().replaceFirstChar { it.uppercase() },
                                    fontSize = 15.sp,
                                    color = colorResource(R.color.black),
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Fotos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colorResource(R.color.black))
                    TextButton(onClick = {}, contentPadding = PaddingValues(0.dp)) {
                        Text("Ver todo", color = colorResource(R.color.blue), fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (parking.photos.isEmpty()) {
                        repeat(3) {
                            Image(
                                painter = painterResource(id = R.drawable.default2),
                                contentDescription = "Sin foto",
                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        parking.photos.take(3).forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Foto parqueadero",
                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                if (showForm) {
                    if (isSubmitting) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .padding(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colorResource(R.color.azulruta))
                        }
                    } else {
                        ReservationBottomBox(
                            parking = parking,
                            onCancel = { showForm = false },
                            onConfirm = { licensePlate, arrivalTime, departureTime ->
                                isSubmitting = true
                                reservationMessage = ""

                                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val today = dateFormat.format(java.util.Date())

                                val fullArrivalTime = "$today $arrivalTime"
                                val fullDepartureTime = "$today $departureTime"

                                val newReservation = Reservation(
                                    parkingId = parking.id,
                                    parkingName = parking.name,
                                    userId = currentUserId,
                                    operatorId = parking.operatorId,
                                    licensePlate = licensePlate,
                                    startTime = fullArrivalTime,
                                    endTime = fullDepartureTime,
                                    status = "Activa"
                                )

                                reserveSlot(
                                    reservation = newReservation,
                                    maxSlots = parking.slot,
                                    onSuccess = {
                                        isSubmitting = false
                                        showForm = false
                                        navController.navigate(AppScreens.MyActivity.name)
                                    },
                                    onError = { e ->
                                        isSubmitting = false
                                        reservationMessage = "Error: ${e.message}"
                                    }
                                )
                            }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { showForm = true },
                            modifier = Modifier.fillMaxWidth(0.85f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Reservar espacio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            if (reservationMessage.isNotEmpty() && !showForm) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)) {
                    Text(text = reservationMessage, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReservationBottomBox(
    parking: ParkingLot,
    onCancel: () -> Unit,
    onConfirm: (licensePlate: String, arrivalTime: String, departureTime: String) -> Unit
) {
    var licensePlate by remember { mutableStateOf("") }
    var arrivalTime by remember { mutableStateOf(parking.hourStart) }
    var departureTime by remember { mutableStateOf(parking.hourFinish) }

    var showArrivalDialog by remember { mutableStateOf(false) }
    var showDepartureDialog by remember { mutableStateOf(false) }

    var availableSlots by remember { mutableIntStateOf(parking.slot) }
    var scheduleErrorMessage by remember { mutableStateOf("") }
    var isCheckingAvailability by remember { mutableStateOf(false) }

    LaunchedEffect(arrivalTime, departureTime) {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())
        val fullArrivalTime = "$today $arrivalTime"
        val fullDepartureTime = "$today $departureTime"

        FirebaseFirestore.getInstance().collection("reservas")
            .whereEqualTo("parkingId", parking.id)
            .whereEqualTo("status", "Activa")
            .get()
            .addOnSuccessListener { snapshot ->
                var overlappingReservations = 0
                for (doc in snapshot.documents) {
                    var rStart = doc.getString("startTime") ?: ""
                    var rEnd = doc.getString("endTime") ?: ""

                    if (rStart.length <= 5) rStart = "$today $rStart"
                    if (rEnd.length <= 5) rEnd = "$today $rEnd"

                    if (fullArrivalTime < rEnd && fullDepartureTime > rStart) {
                        overlappingReservations++
                    }
                }

                availableSlots = parking.slot - overlappingReservations

                if (availableSlots <= 0) {
                    scheduleErrorMessage = "Agotado en este horario, selecciona otro."
                }
                isCheckingAvailability = false
            }
            .addOnFailureListener {
                scheduleErrorMessage = "Error al conectar con la base de datos"
                isCheckingAvailability = false
            }
    }

    if (showArrivalDialog) {
        TimeEditDialog("Hora de llegada", arrivalTime, { arrivalTime = it; showArrivalDialog = false }, { showArrivalDialog = false })
    }
    if (showDepartureDialog) {
        TimeEditDialog("Hora de salida", departureTime, { departureTime = it; showDepartureDialog = false }, { showDepartureDialog = false })
    }

    Column(
        modifier = Modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(28.dp)
            .fillMaxWidth()
    ) {
        Text("Detalles de tu reserva", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(parking.name, fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = licensePlate,
            onValueChange = { newValue ->
                val baseText = newValue.replace("-", "").uppercase()
                var filteredText = ""
                for (i in baseText.indices) {
                    if (i < 3 && baseText[i].isLetter()) {
                        filteredText += baseText[i]
                    } else if (i in 3..5 && baseText[i].isDigit()) {
                        filteredText += baseText[i]
                    }
                }
                licensePlate = if (filteredText.length > 3) {
                    "${filteredText.substring(0, 3)}-${filteredText.substring(3)}"
                } else {
                    filteredText
                }
            },
            label = { Text("Placa del vehículo (Ej: ABC-123)", fontSize = 14.sp) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(R.color.azulruta),
                focusedLabelColor = colorResource(R.color.azulruta),
                unfocusedBorderColor = Color.LightGray
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (licensePlate.length == 7) colorResource(R.color.azulruta) else Color.LightGray
                )
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showArrivalDialog = true },
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.azulruta).copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorResource(R.color.azulruta).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Llegada", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(arrivalTime, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colorResource(R.color.azulruta))
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showDepartureDialog = true },
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.azulruta).copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorResource(R.color.azulruta).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Salida", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(departureTime, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colorResource(R.color.azulruta))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (isCheckingAvailability) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), shape = CircleShape) {
                    Text("Calculando disponibilidad...", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            } else if (scheduleErrorMessage.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = CircleShape) {
                    Text(scheduleErrorMessage, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color(0xFFC62828), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = CircleShape) {
                    Text("¡Hay $availableSlots cupos disponibles!", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Cancelar", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) }

            Button(
                enabled = licensePlate.length == 7 && scheduleErrorMessage.isEmpty() && !isCheckingAvailability,
                onClick = { onConfirm(licensePlate, arrivalTime, departureTime) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.azulruta),
                    disabledContainerColor = colorResource(R.color.azulruta).copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Confirmar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

fun reserveSlot(
    reservation: Reservation,
    maxSlots: Int,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val today = dateFormat.format(java.util.Date())

    db.collection("reservas")
        .whereEqualTo("parkingId", reservation.parkingId)
        .whereEqualTo("status", "Activa")
        .get()
        .addOnSuccessListener { snapshot ->
            var overlappingReservations = 0
            for (doc in snapshot.documents) {
                var rStart = doc.getString("startTime") ?: ""
                var rEnd = doc.getString("endTime") ?: ""

                if (rStart.length <= 5 && rStart.isNotEmpty()) rStart = "$today $rStart"
                if (rEnd.length <= 5 && rEnd.isNotEmpty()) rEnd = "$today $rEnd"

                if (reservation.startTime < rEnd && reservation.endTime > rStart) {
                    overlappingReservations++
                }
            }

            if (overlappingReservations < maxSlots) {
                val reservationRef = db.collection("reservas").document()
                val finalReservation = reservation.copy(id = reservationRef.id)

                reservationRef.set(finalReservation)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e) }
            } else {
                onError(Exception("Lo sentimos, alguien acaba de tomar el último cupo en este horario."))
            }
        }
        .addOnFailureListener { e -> onError(e) }
}