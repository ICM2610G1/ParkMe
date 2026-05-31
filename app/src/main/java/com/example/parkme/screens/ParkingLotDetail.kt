package com.example.parkme.screens

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
    var mostrarFormulario by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var mensajeReserva by remember { mutableStateOf("") }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

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
                    .padding(bottom = if (mostrarFormulario) 380.dp else 100.dp)
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

                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp, end = 12.dp)
                            .size(6.dp)
                            .background(Color.Black, CircleShape)
                    )
                    Column {
                        Text("Reglas del parqueadero", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = parking.terms.ifEmpty { "Sin reglas definidas" }, fontSize = 16.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                if (mostrarFormulario) {
                    if (isSubmitting) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colorResource(R.color.grisClaro), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colorResource(R.color.blue))
                        }
                    } else {
                        ReservationBottomBox(
                            parking = parking,
                            onCancel = { mostrarFormulario = false },
                            onConfirm = { placa, horaLlegada, horaSalida ->
                                isSubmitting = true
                                mensajeReserva = ""

                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val hoy = sdf.format(java.util.Date())

                                val llegadaFull = "$hoy $horaLlegada"
                                val salidaFull = "$hoy $horaSalida"

                                val nuevaReserva = Reservation(
                                    parkingId = parking.id,
                                    parkingName = parking.name,
                                    userId = uid,
                                    operatorId = parking.operatorId,
                                    licensePlate = placa,
                                    startTime = llegadaFull,
                                    endTime = salidaFull,
                                    status = "Activa"
                                )


                                reserveSlot(
                                    reserva = nuevaReserva,
                                    maxSlots = parking.slot,
                                    onSuccess = {
                                        isSubmitting = false
                                        mostrarFormulario = false
                                        navController.navigate(AppScreens.MyActivity.name)
                                    },
                                    onError = { e ->
                                        isSubmitting = false
                                        mensajeReserva = "Error: ${e.message}"
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
                            onClick = { mostrarFormulario = true },
                            modifier = Modifier.fillMaxWidth(0.85f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Reservar espacio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            if (mensajeReserva.isNotEmpty() && !mostrarFormulario) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)) {
                    Text(text = mensajeReserva, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReservationBottomBox(
    parking: ParkingLot,
    onCancel: () -> Unit,
    onConfirm: (placa: String, horaLlegada: String, horaSalida: String) -> Unit
) {
    var placa by remember { mutableStateOf("") }
    var horaLlegada by remember { mutableStateOf(parking.hourStart) }
    var horaSalida by remember { mutableStateOf(parking.hourFinish) }

    var mostrarDialogoLlegada by remember { mutableStateOf(false) }
    var mostrarDialogoSalida by remember { mutableStateOf(false) }

    var cuposDisponibles by remember { mutableIntStateOf(parking.slot) }
    var mensajeErrorHorario by remember { mutableStateOf("") }
    var isCheckingDisponibilidad by remember { mutableStateOf(false) }

    LaunchedEffect(horaLlegada, horaSalida) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val hoy = sdf.format(java.util.Date())
        val llegadaFull = "$hoy $horaLlegada"
        val salidaFull = "$hoy $horaSalida"

        FirebaseFirestore.getInstance().collection("reservas")
            .whereEqualTo("parkingId", parking.id)
            .whereEqualTo("status", "Activa")
            .get()
            .addOnSuccessListener { snapshot ->
                var reservasSuperpuestas = 0
                for (doc in snapshot.documents) {
                    var rStart = doc.getString("startTime") ?: ""
                    var rEnd = doc.getString("endTime") ?: ""

                    if (rStart.length <= 5) rStart = "$hoy $rStart"
                    if (rEnd.length <= 5) rEnd = "$hoy $rEnd"

                    if (llegadaFull < rEnd && salidaFull > rStart) {
                        reservasSuperpuestas++
                    }
                }

                cuposDisponibles = parking.slot - reservasSuperpuestas

                if (cuposDisponibles <= 0) {
                    mensajeErrorHorario = "Agotado en este horario, selecciona otro."
                }
                isCheckingDisponibilidad = false
            }
            .addOnFailureListener {
                mensajeErrorHorario = "Error al conectar con la base de datos"
                isCheckingDisponibilidad = false
            }
    }

    if (mostrarDialogoLlegada) {
        TimeEditDialog("Hora de llegada", horaLlegada, { horaLlegada = it; mostrarDialogoLlegada = false }, { mostrarDialogoLlegada = false })
    }
    if (mostrarDialogoSalida) {
        TimeEditDialog("Hora de salida", horaSalida, { horaSalida = it; mostrarDialogoSalida = false }, { mostrarDialogoSalida = false })
    }

    Column(
        modifier = Modifier
            .background(
                color = colorResource(R.color.grisClaro),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Text("Detalles de tu reserva", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(parking.name, fontSize = 14.sp,  color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = placa,
            onValueChange = { newValue ->
                val textoBase = newValue.replace("-", "").uppercase()

                var textoFiltrado = ""
                for (i in textoBase.indices) {
                    if (i < 3 && textoBase[i].isLetter()) {
                        textoFiltrado += textoBase[i]
                    } else if (i in 3..5 && textoBase[i].isDigit()) {
                        textoFiltrado += textoBase[i]
                    }
                }

                placa = if (textoFiltrado.length > 3) {
                    "${textoFiltrado.substring(0, 3)}-${textoFiltrado.substring(3)}"
                } else {
                    textoFiltrado
                }
            },
            label = { Text("Placa (Ej: ABC-123)", fontSize = 14.sp) },
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(R.color.blue),
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabelAndRight("Llegada", right = {
            Box(modifier = Modifier.clickable { mostrarDialogoLlegada = true }) { TimePill(horaLlegada) }
        })
        LabelAndRight("Salida", right = {
            Box(modifier = Modifier.clickable { mostrarDialogoSalida = true }) { TimePill(horaSalida) }
        })

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (isCheckingDisponibilidad) {
                Text("Calculando disponibilidad...", color = Color.Gray, fontSize = 13.sp)
            } else if (mensajeErrorHorario.isNotEmpty()) {
                Text(mensajeErrorHorario, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("Hay $cuposDisponibles cupos en este horario", color = Color(0xFF008000), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50)
            ) { Text("Cancelar", color = Color.White, fontWeight = FontWeight.Bold) }

            Button(
                enabled = placa.length == 7 && mensajeErrorHorario.isEmpty() && !isCheckingDisponibilidad,
                onClick = { onConfirm(placa, horaLlegada, horaSalida) },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50)
            ) { Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

fun reserveSlot(
    reserva: Reservation,
    maxSlots: Int,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val hoy = sdf.format(java.util.Date())

    db.collection("reservas")
        .whereEqualTo("parkingId", reserva.parkingId)
        .whereEqualTo("status", "Activa")
        .get()
        .addOnSuccessListener { snapshot ->
            var reservasSuperpuestas = 0
            for (doc in snapshot.documents) {
                var rStart = doc.getString("startTime") ?: ""
                var rEnd = doc.getString("endTime") ?: ""

                if (rStart.length <= 5 && rStart.isNotEmpty()) rStart = "$hoy $rStart"
                if (rEnd.length <= 5 && rEnd.isNotEmpty()) rEnd = "$hoy $rEnd"

                if (reserva.startTime < rEnd && reserva.endTime > rStart) {
                    reservasSuperpuestas++
                }
            }

            if (reservasSuperpuestas < maxSlots) {
                val reservationRef = db.collection("reservas").document()
                val reservaFinal = reserva.copy(id = reservationRef.id)

                reservationRef.set(reservaFinal)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e) }
            } else {
                onError(Exception("Lo sentimos, alguien acaba de tomar el último cupo en este horario."))
            }
        }
        .addOnFailureListener { e -> onError(e) }
}
