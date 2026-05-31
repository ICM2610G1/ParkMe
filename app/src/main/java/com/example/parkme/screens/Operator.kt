package com.example.parkme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun CreateParkingVisual(navController: NavController, modifier: Modifier = Modifier) {
    val pillShape = RoundedCornerShape(50)
    val cardShape = RoundedCornerShape(24)
    val backStackEntry = navController.currentBackStackEntry

    val scrollState = rememberScrollState()
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val context = LocalContext.current

    val name = rememberSaveable { mutableStateOf("") }
    val pricePerHour = rememberSaveable { mutableStateOf("") }
    val pricePerMin = rememberSaveable { mutableStateOf("") }
    val fixedPrice = rememberSaveable { mutableStateOf("") }
    val terms = rememberSaveable { mutableStateOf("") }
    val electricCharges = rememberSaveable { mutableStateOf(false) }
    val hourStart = rememberSaveable { mutableStateOf("00:00") }
    val hourFinish = rememberSaveable { mutableStateOf("23:59") }
    val slot = rememberSaveable { mutableStateOf("0") }
    val message = rememberSaveable { mutableStateOf("") }
    val isUploading = rememberSaveable { mutableStateOf(false) }
    val location = remember { mutableStateOf<LatLng?>(null) }
    val address = rememberSaveable { mutableStateOf("") }

    val photoUris = remember {
        mutableStateOf<List<Uri>>(
            backStackEntry?.savedStateHandle?.get<ArrayList<Uri>>("photoUris") ?: emptyList()
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val availableSpaces = 15 - photoUris.value.size
        val allowedUris = uris.take(availableSpaces)

        if (uris.size > availableSpaces) {
            android.widget.Toast.makeText(
                context,
                "Solo se permitieron ${allowedUris.size} fotos para no exceder el límite de 15",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        val newList = photoUris.value + allowedUris
        photoUris.value = newList
        backStackEntry?.savedStateHandle?.set("photoUris", ArrayList(newList))
    }

    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showOpeningDialog by rememberSaveable { mutableStateOf(false) }
    var showClosingDialog by rememberSaveable { mutableStateOf(false) }

    val latLng by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<LatLng?>("latLng", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    val selectedDays = remember {
        mutableStateOf<List<Boolean>>(
            backStackEntry?.savedStateHandle?.get<ArrayList<Boolean>>("days")
                ?: listOf(true, false, true, false, true, false, true)
        )
    }
    LaunchedEffect(selectedDays.value) {
        backStackEntry?.savedStateHandle?.set("selected_days", selectedDays.value)
    }

    LaunchedEffect(latLng) {
        if (latLng != null) {
            location.value = latLng
            try {
                val geocoder = android.location.Geocoder(context)
                val addresses = geocoder.getFromLocation(latLng!!.latitude, latLng!!.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    address.value = addresses[0].getAddressLine(0) ?: ""
                } else {
                    address.value = "Coordenadas: ${latLng!!.latitude}, ${latLng!!.longitude}"
                }
            } catch (e: Exception) {
                address.value = "Coordenadas: ${latLng!!.latitude}, ${latLng!!.longitude}"
            }
        }
    }

    Column(
        modifier = Modifier
            .background(color = colorResource(R.color.back))
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier
                    .width(130.dp)
                    .height(80.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier
                .height(60.dp)
                .width(2.dp)
                .background(Color.Black))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Crear\nparqueadero",
                color = colorResource(R.color.black),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("Nombre del parqueadero", fontSize = 12.sp) },
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(R.color.grisClaro),
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = pricePerHour.value,
                onValueChange = { pricePerHour.value = it },
                label = { Text("$ Precio por hora", fontSize = 12.sp) },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(R.color.grisClaro),
                    unfocusedBorderColor = Color.Gray
                )
            )
            OutlinedTextField(
                value = pricePerMin.value,
                onValueChange = { pricePerMin.value = it },
                label = { Text("$ Precio por minuto", fontSize = 12.sp) },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(R.color.grisClaro),
                    unfocusedBorderColor = Color.Gray
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = slot.value,
            onValueChange = { slot.value = it },
            label = { Text("Cupos disponibles", fontSize = 12.sp) },
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(R.color.grisClaro),
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        LabelAndRight(
            label = "Cargador EV",
            right = {
                CircleCheckClickable(
                    checked = electricCharges.value,
                    onClick = { electricCharges.value = !electricCharges.value }
                )
            }
        )

        LabelAndRight(
            label = "Tarifa plena",
            right = {
                OutlinedTextField(
                    value = fixedPrice.value,
                    onValueChange = { fixedPrice.value = it },
                    label = { Text("$ Tarifa", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .width(140.dp)
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.grisClaro),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        )

        LabelAndRight(
            label = "Reglas del parqueadero",
            right = {
                Button(
                    onClick = { showTermsDialog = true },
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.grisClaro)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Ingresar", color = Color.Blue, fontWeight = FontWeight.Bold)
                }
            }
        )

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { Text("Reglas del parqueadero") },
                text = {
                    OutlinedTextField(
                        value = terms.value,
                        onValueChange = { terms.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        label = { Text("Escribe las reglas aquí") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showTermsDialog = false }) { Text("Listo") }
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colorResource(R.color.grisClaro), cardShape)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fotos del parqueadero", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${photoUris.value.size}/15", color = Color.Gray)
                }

                Spacer(Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 4.dp, end = 4.dp)
                ) {
                    items(photoUris.value) { uri ->
                        SelectedImage(
                            model = uri,
                            onDelete = {
                                val newList = photoUris.value.toMutableList()
                                newList.remove(uri)
                                photoUris.value = newList
                            }
                        )
                    }

                    if (photoUris.value.size < 15) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                                    .clickable { launcher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Agregar foto", modifier = Modifier.size(44.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                navController.navigate(AppScreens.MapPicker.name) {
                    launchSingleTop = true
                }
            },
            shape = pillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (location.value != null) colorResource(R.color.blue) else colorResource(R.color.grisClaro)
            ),
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (location.value != null) "Ubicación agregada" else "Agregar Ubicación",
                color = if (location.value != null) Color.White else Color.DarkGray,
                fontWeight = FontWeight.Bold
            )
        }

        if (location.value != null && address.value.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = address.value,
                fontSize = 14.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        LabelAndRight(
            label = "Hora de apertura",
            right = {
                Box(modifier = Modifier.clickable { showOpeningDialog = true }) {
                    TimePill(hourStart.value)
                }
            }
        )
        LabelAndRight(
            label = "Hora de cierre",
            right = {
                Box(modifier = Modifier.clickable { showClosingDialog = true }) {
                    TimePill(hourFinish.value)
                }
            }
        )

        if (showOpeningDialog) {
            TimeEditDialog(
                title = "Hora de apertura",
                currentTime = hourStart.value,
                onConfirm = { hourStart.value = it; showOpeningDialog = false },
                onDismiss = { showOpeningDialog = false }
            )
        }
        if (showClosingDialog) {
            TimeEditDialog(
                title = "Hora de cierre",
                currentTime = hourFinish.value,
                onConfirm = { hourFinish.value = it; showClosingDialog = false },
                onDismiss = { showClosingDialog = false }
            )
        }

        Spacer(Modifier.height(12.dp))

        val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEach { d -> DayLetter(d) }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            selectedDays.value.forEachIndexed { index, seleccionado ->
                CircleCheckClickable(
                    checked = seleccionado,
                    onClick = {
                        val newList = selectedDays.value.toMutableList()
                        newList[index] = !seleccionado
                        selectedDays.value = newList
                        backStackEntry?.savedStateHandle?.set("days", ArrayList(newList))
                    }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            enabled = !isUploading.value,
            onClick = {
                if (uid.isEmpty()) {
                    message.value = "Usuario no autenticado"
                    return@Button
                }
                if (location.value == null) {
                    message.value = "Debes agregar una ubicación en el mapa"
                    return@Button
                }

                isUploading.value = true

                val daysString = selectedDays.value
                    .mapIndexed { i, b -> if (b) listOf("L", "M", "M", "J", "V", "S", "D")[i] else "" }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")

                val baseParkingLot = hashMapOf(
                    "operatorId" to uid,
                    "name" to name.value,
                    "pricePerHour" to pricePerHour.value,
                    "pricePerMin" to pricePerMin.value,
                    "fixedPrice" to fixedPrice.value,
                    "terms" to terms.value,
                    "electricCharges" to electricCharges.value,
                    "hourStart" to hourStart.value,
                    "hourFinish" to hourFinish.value,
                    "weekAvailability" to daysString,
                    "slot" to (slot.value.toIntOrNull() ?: 0),
                    "fotos" to emptyList<String>(),
                    "photos" to emptyList<String>(),
                    "latitud" to (location.value?.latitude ?: 0.0),
                    "latitude" to (location.value?.latitude ?: 0.0),
                    "longitud" to (location.value?.longitude ?: 0.0),
                    "longitude" to (location.value?.longitude ?: 0.0),
                    "direccion" to address.value,
                    "address" to address.value
                )

                db.collection("parking lots").add(baseParkingLot)
                    .addOnSuccessListener { docRef ->
                        val totalPhotos = photoUris.value.size
                        if (totalPhotos == 0) {
                            message.value = " Parqueadero creado"
                            isUploading.value = false
                            navController.popBackStack()
                            return@addOnSuccessListener
                        }

                        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
                        scope.launch {
                            try {
                                val storageRef = FirebaseStorage.getInstance().reference
                                val uploadedPhotos = mutableListOf<String>()

                                photoUris.value.forEach { uri ->
                                    val fileName = UUID.randomUUID().toString() + ".jpg"
                                    val imageRef = storageRef.child("parking lots/$fileName")
                                    imageRef.putFile(uri).await()
                                    val downloadUrl = imageRef.downloadUrl.await()
                                    uploadedPhotos.add(downloadUrl.toString())
                                }

                                withContext(Dispatchers.Main) {
                                    docRef.update(mapOf(
                                        "fotos" to uploadedPhotos,
                                        "photos" to uploadedPhotos
                                    )).addOnSuccessListener {
                                        message.value = "Parqueadero creado con fotos"
                                        isUploading.value = false
                                        navController.popBackStack()
                                    }
                                        .addOnFailureListener { e ->
                                            message.value = "Error guardando fotos: ${e.message}"
                                            isUploading.value = false
                                        }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    message.value = "Error subiendo fotos a Firebase: ${e.message}"
                                    isUploading.value = false
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        message.value = "Error creando parqueadero: ${e.message}"
                        isUploading.value = false
                    }
            },
            shape = pillShape,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.6f)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue))
        ) {
            if (isUploading.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Crear", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        if (message.value.isNotEmpty()) {
            Text(
                text = message.value,
                color = if (message.value.startsWith("P")) Color.Green else Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun EditParkingVisual(
    parkingId: String = "", navController: NavController? = null, modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(50)
    val cardShape = RoundedCornerShape(24)
    val scrollState = rememberScrollState()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val name = remember { mutableStateOf("") }
    val pricePerHour = remember { mutableStateOf("") }
    val pricePerMin = remember { mutableStateOf("") }
    val fixedPrice = remember { mutableStateOf("") }
    val terms = remember { mutableStateOf("") }
    val electricCharges = remember { mutableStateOf(false) }
    val hourStart = remember { mutableStateOf("00:00") }
    val hourFinish = remember { mutableStateOf("23:59") }
    val slot = remember { mutableStateOf("0") }
    val selectedDays = remember { mutableStateOf(listOf(true, false, true, false, true, false, true)) }
    val message = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(true) }
    val isUploading = remember { mutableStateOf(false) }
    val photoUris = remember { mutableStateOf<List<Uri>>(emptyList()) }
    val photoUrls = remember { mutableStateOf<List<String>>(emptyList()) }
    val location = remember { mutableStateOf<LatLng?>(null) }
    val address = rememberSaveable { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val currentTotal = photoUrls.value.size + photoUris.value.size
        val availableSpaces = 15 - currentTotal
        val allowedUris = uris.take(availableSpaces)

        if (uris.size > availableSpaces) {
            android.widget.Toast.makeText(
                context,
                "Solo se agregaron ${allowedUris.size} fotos para respetar el límite de 15",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        photoUris.value = photoUris.value + allowedUris
    }

    val latLng by navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<LatLng?>("latLng", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(latLng) {
        if (latLng != null) {
            location.value = latLng
            try {
                val geocoder = android.location.Geocoder(context)
                val addresses = geocoder.getFromLocation(latLng!!.latitude, latLng!!.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    address.value = addresses[0].getAddressLine(0) ?: ""
                } else {
                    address.value = "Coordenadas: ${latLng!!.latitude}, ${latLng!!.longitude}"
                }
            } catch (e: Exception) {
                address.value = "Coordenadas: ${latLng!!.latitude}, ${latLng!!.longitude}"
            }
        }
    }

    LaunchedEffect(parkingId) {
        if (parkingId.isNotEmpty()) {
            db.collection("parking lots").document(parkingId).get()
                .addOnSuccessListener { doc ->
                    name.value = doc.getString("name") ?: ""
                    pricePerHour.value = doc.getString("pricePerHour") ?: ""
                    pricePerMin.value = doc.getString("pricePerMin") ?: ""
                    fixedPrice.value = doc.getString("fixedPrice") ?: ""
                    terms.value = doc.getString("terms") ?: ""
                    electricCharges.value = doc.getBoolean("electricCharges") ?: false
                    hourStart.value = doc.getString("hourStart") ?: "00:00"
                    hourFinish.value = doc.getString("hourFinish") ?: "23:59"
                    slot.value = doc.getLong("slot")?.toString() ?: "0"
                    address.value = doc.getString("address") ?: doc.getString("direccion") ?: ""

                    val lat = doc.getDouble("latitude") ?: doc.getDouble("latitud")
                    val lng = doc.getDouble("longitude") ?: doc.getDouble("longitud")
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        location.value = LatLng(lat, lng)
                    }

                    val weekAvail = doc.getString("weekAvailability") ?: ""
                    val days = listOf("L", "M", "M", "J", "V", "S", "D")
                    selectedDays.value = days.map { weekAvail.contains(it) }

                    val firestorePhotos = doc.get("photos") ?: doc.get("fotos")
                    if (firestorePhotos is List<*>) {
                        photoUrls.value = firestorePhotos.filterIsInstance<String>()
                    }

                    isLoading.value = false
                }
                .addOnFailureListener {
                    message.value = "Error al cargar datos"
                    isLoading.value = false
                }
        } else {
            isLoading.value = false
        }
    }

    if (isLoading.value) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .background(color = colorResource(R.color.back))
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier
                    .width(130.dp)
                    .height(80.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .width(2.dp)
                    .background(Color.Black)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Editar\nparqueadero",
                color = colorResource(R.color.black),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("Nombre del parqueadero", fontSize = 12.sp) },
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(R.color.grisClaro),
                unfocusedBorderColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = pricePerHour.value,
                onValueChange = { pricePerHour.value = it },
                label = { Text("$ Precio por hora", fontSize = 12.sp) },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(R.color.grisClaro),
                    unfocusedBorderColor = Color.Gray
                )
            )
            OutlinedTextField(
                value = pricePerMin.value,
                onValueChange = { pricePerMin.value = it },
                label = { Text("$ Precio por minuto", fontSize = 12.sp) },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(R.color.grisClaro),
                    unfocusedBorderColor = Color.Gray
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = slot.value,
            onValueChange = { slot.value = it },
            label = { Text("Cupos disponibles", fontSize = 12.sp) },
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(R.color.grisClaro),
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        LabelAndRight(
            label = "Cargador EV",
            right = {
                CircleCheckClickable(
                    checked = electricCharges.value,
                    onClick = { electricCharges.value = !electricCharges.value }
                )
            }
        )

        LabelAndRight(
            label = "Tarifa plena",
            right = {
                OutlinedTextField(
                    value = fixedPrice.value,
                    onValueChange = { fixedPrice.value = it },
                    label = { Text("$ Tarifa", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .width(140.dp)
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.grisClaro),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        )

        var showTermsDialog by remember { mutableStateOf(false) }

        LabelAndRight(
            label = "Reglas del parqueadero",
            right = {
                Button(
                    onClick = { showTermsDialog = true },
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.grisClaro)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Ingresar", color = Color.Blue, fontWeight = FontWeight.Bold)
                }
            }
        )
        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { Text("Reglas del parqueadero") },
                text = {
                    OutlinedTextField(
                        value = terms.value,
                        onValueChange = { terms.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        label = { Text("Escribe las reglas aquí") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showTermsDialog = false }) { Text("Listo") }
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colorResource(R.color.grisClaro), cardShape)
                .padding(20.dp)
        ) {
            Column {
                val totalPhotos = photoUrls.value.size + photoUris.value.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fotos del parqueadero", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("$totalPhotos/15", color = Color.Gray)
                }

                Spacer(Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 4.dp, end = 4.dp)
                ) {
                    items(photoUrls.value) { url ->
                        SelectedImage(
                            model = url,
                            onDelete = { photoUrls.value = photoUrls.value.filter { it != url } }
                        )
                    }
                    items(photoUris.value) { uri ->
                        SelectedImage(
                            model = uri,
                            onDelete = { photoUris.value = photoUris.value.filter { it != uri } }
                        )
                    }

                    if (totalPhotos < 15) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                                    .clickable { launcher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Agregar foto", modifier = Modifier.size(44.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { navController?.navigate(AppScreens.MapPicker.name) },
            shape = pillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (location.value != null) colorResource(R.color.blue) else colorResource(R.color.grisClaro)
            ),
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (location.value != null) "Ubicación agregada" else "Agregar Ubicación",
                color = if (location.value != null) Color.White else Color.DarkGray,
                fontWeight = FontWeight.Bold
            )
        }

        if (location.value != null && address.value.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = address.value,
                fontSize = 14.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        var showOpeningDialog by remember { mutableStateOf(false) }
        var showClosingDialog by remember { mutableStateOf(false) }

        LabelAndRight(
            label = "Hora de apertura",
            right = {
                Box(modifier = Modifier.clickable { showOpeningDialog = true }) {
                    TimePill(hourStart.value)
                }
            }
        )
        LabelAndRight(
            label = "Hora de cierre",
            right = {
                Box(modifier = Modifier.clickable { showClosingDialog = true }) {
                    TimePill(hourFinish.value)
                }
            }
        )

        if (showOpeningDialog) {
            TimeEditDialog(
                title = "Hora de apertura",
                currentTime = hourStart.value,
                onConfirm = { hourStart.value = it; showOpeningDialog = false },
                onDismiss = { showOpeningDialog = false }
            )
        }
        if (showClosingDialog) {
            TimeEditDialog(
                title = "Hora de cierre",
                currentTime = hourFinish.value,
                onConfirm = { hourFinish.value = it; showClosingDialog = false },
                onDismiss = { showClosingDialog = false }
            )
        }

        Spacer(Modifier.height(12.dp))

        val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEach { d -> DayLetter(d) }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            selectedDays.value.forEachIndexed { index, seleccionado ->
                CircleCheckClickable(
                    checked = seleccionado,
                    onClick = {
                        val newList = selectedDays.value.toMutableList()
                        newList[index] = !seleccionado
                        selectedDays.value = newList
                    }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            enabled = !isUploading.value,
            onClick = {
                if (parkingId.isEmpty()) {
                    message.value = "ID de parqueadero inválido"
                    return@Button
                }
                isUploading.value = true

                val daysString = selectedDays.value
                    .mapIndexed { i, b -> if (b) listOf("L", "M", "M", "J", "V", "S", "D")[i] else "" }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")

                val totalPhotos = photoUris.value.size
                if (totalPhotos == 0) {
                    saveParkingDetails(
                        db, parkingId, name.value, pricePerHour.value, pricePerMin.value,
                        fixedPrice.value, terms.value, electricCharges.value,
                        hourStart.value, hourFinish.value, daysString,
                        slot.value.toIntOrNull() ?: 0,
                        photoUrls.value, location.value, address,
                        message, isUploading, navController
                    )
                } else {
                    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        try {
                            val storageRef = FirebaseStorage.getInstance().reference
                            val uploadedPhotos = mutableListOf<String>()

                            photoUris.value.forEach { uri ->
                                val fileName = UUID.randomUUID().toString() + ".jpg"
                                val imageRef = storageRef.child("parking lots/$fileName")
                                imageRef.putFile(uri).await()
                                val downloadUrl = imageRef.downloadUrl.await()
                                uploadedPhotos.add(downloadUrl.toString())
                            }
                            val allPhotos = photoUrls.value + uploadedPhotos

                            withContext(Dispatchers.Main) {
                                saveParkingDetails(
                                    db, parkingId, name.value , pricePerHour.value, pricePerMin.value,
                                    fixedPrice.value, terms.value, electricCharges.value,
                                    hourStart.value, hourFinish.value, daysString,
                                    slot.value.toIntOrNull() ?: 0,
                                    allPhotos, location.value, address,
                                    message, isUploading, navController
                                )
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                message.value = "Error subiendo fotos a Firebase: ${e.message}"
                                isUploading.value = false
                            }
                        }
                    }
                }
            },
            shape = pillShape,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.6f)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue))
        ) {
            if (isUploading.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !isUploading.value,
            onClick = {
                deleteParking(db, parkingId, message, isUploading, navController)
            },
            shape = pillShape,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.6f)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            if (isUploading.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Eliminar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (message.value.isNotEmpty()) {
            Text(
                text = message.value,
                color = if (message.value.startsWith("P") || message.value.startsWith("C")) Color.Green else Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun LabelAndRight(label: String, right: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp)
        right()
    }
}

@Composable
fun CircleCheckClickable(checked: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(28.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.Black)
    ) {
        if (checked) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Check",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TimePill(time: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = colorResource(R.color.grisClaro),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = colorResource(R.color.azulruta),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DayLetter(letter: String) {
    Surface(
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = colorResource(R.color.grisClaro)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(letter, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TimeEditDialog(
    title: String, currentTime: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit
) {
    var time by remember { mutableStateOf(currentTime) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("HH:MM") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(time) }) { Text("Listo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

fun saveParkingDetails(
    db: FirebaseFirestore,
    parkingId: String,
    name: String,
    pricePerHour: String,
    pricePerMin: String,
    fixedPrice: String,
    terms: String,
    electricCharges: Boolean,
    hourStart: String,
    hourFinish: String,
    weekAvailability: String,
    slot: Int,
    photos: List<String>,
    location: LatLng?,
    address: MutableState<String>,
    message: MutableState<String>,
    isUploading: MutableState<Boolean>,
    navController: NavController?
) {
    val data = hashMapOf(
        "name" to name,
        "pricePerHour" to pricePerHour,
        "pricePerMin" to pricePerMin,
        "fixedPrice" to fixedPrice,
        "terms" to terms,
        "electricCharges" to electricCharges,
        "hourStart" to hourStart,
        "hourFinish" to hourFinish,
        "weekAvailability" to weekAvailability,
        "slot" to slot,
        "fotos" to photos,
        "photos" to photos,
        "latitud" to (location?.latitude ?: 0.0),
        "latitude" to (location?.latitude ?: 0.0),
        "longitud" to (location?.longitude ?: 0.0),
        "longitude" to (location?.longitude ?: 0.0),
        "direccion" to address.value,
        "address" to address.value
    )
    db.collection("parking lots").document(parkingId).set(data, SetOptions.merge())
        .addOnSuccessListener {
            message.value = "Cambios guardados"
            isUploading.value = false
            navController?.popBackStack()
        }.addOnFailureListener { e ->
            message.value = "Error: ${e.message}"
            isUploading.value = false
        }
}

fun deleteParking(
    db: FirebaseFirestore,
    parkingId: String,
    message: MutableState<String>,
    isUploading: MutableState<Boolean>,
    navController: NavController?
) {
    if (parkingId.isEmpty()) {
        message.value = "ID de parqueadero inválido"
        return
    }

    isUploading.value = true

    db.collection("parking lots").document(parkingId).delete()
        .addOnSuccessListener {
            message.value = "Parqueadero eliminado"
            isUploading.value = false
            navController?.popBackStack()
        }.addOnFailureListener { e ->
            message.value = "Error al eliminar: ${e.message}"
            isUploading.value = false
        }
}

@Composable
fun SelectedImage(model: Any, onDelete: () -> Unit) {
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        AsyncImage(
            model = model,
            contentDescription = "Foto seleccionada",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Eliminar foto",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}