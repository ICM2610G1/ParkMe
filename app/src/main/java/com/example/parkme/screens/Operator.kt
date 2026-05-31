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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier.width(110.dp).height(65.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.height(45.dp).width(2.dp).background(Color.Black))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Crear\nparqueadero",
                color = colorResource(R.color.black),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        FormSection("Información General", Icons.Default.Home) {
            ModernTextField(
                value = name.value,
                onValueChange = { name.value = it },
                label = "Nombre del parqueadero"
            )
            Spacer(Modifier.height(12.dp))
            ModernTextField(
                value = slot.value,
                onValueChange = { slot.value = it },
                label = "Cupos disponibles",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Estación de carga EV", fontSize = 15.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = electricCharges.value,
                    onCheckedChange = { electricCharges.value = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = colorResource(R.color.blue))
                )
            }
        }

        FormSection("Tarifas (Obligatorio)", Icons.Default.Star) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernTextField(
                    value = pricePerHour.value,
                    onValueChange = { pricePerHour.value = it },
                    label = "$ Precio por hora",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                ModernTextField(
                    value = pricePerMin.value,
                    onValueChange = { pricePerMin.value = it },
                    label = "$ Precio por min",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
            }
            Spacer(Modifier.height(12.dp))
            ModernTextField(
                value = fixedPrice.value,
                onValueChange = { fixedPrice.value = it },
                label = "$ Tarifa plena (Día completo)",
                keyboardType = KeyboardType.Number
            )
        }


        FormSection("Horarios de Atención", Icons.Default.DateRange) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showOpeningDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text("Apertura:\n${hourStart.value}", textAlign = TextAlign.Center)
                }
                OutlinedButton(
                    onClick = { showClosingDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text("Cierre:\n${hourFinish.value}", textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Días de servicio", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

            val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dayLabels.forEachIndexed { index, letter ->
                    DayToggle(
                        letter = letter,
                        isSelected = selectedDays.value[index],
                        onClick = {
                            val newList = selectedDays.value.toMutableList()
                            newList[index] = !newList[index]
                            selectedDays.value = newList
                            backStackEntry?.savedStateHandle?.set("days", ArrayList(newList))
                        }
                    )
                }
            }
        }

        FormSection("Detalles y Multimedia", Icons.Default.List) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showTermsDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = colorResource(R.color.blue))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Reglas del parqueadero", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(if (terms.value.isEmpty()) "Toca para añadir reglas" else "Reglas añadidas", fontSize = 12.sp, color = Color.Gray)
                    }
                    Text("Editar", color = colorResource(R.color.blue), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate(AppScreens.MapPicker.name) { launchSingleTop = true } },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (location.value != null) Color(0xFFE8F5E9) else Color(0xFFF5F7FA)),
                border = BorderStroke(1.dp, if (location.value != null) Color(0xFFA5D6A7) else Color(0xFFE0E0E0))
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (location.value != null) Color(0xFF2E7D32) else Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (location.value != null) "Ubicación fijada" else "Ubicación en el mapa", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(if (location.value != null) address.value else "Toca para abrir el mapa", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Fotos", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${photoUris.value.size}/15", color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
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
                                .border(1.5.dp, colorResource(R.color.blue), RoundedCornerShape(12.dp))
                                .background(colorResource(R.color.blue).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = colorResource(R.color.blue))
                                Text("Añadir", color = colorResource(R.color.blue), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = !isUploading.value,
            onClick = {
                if (uid.isEmpty()) { message.value = "Usuario no autenticado"; return@Button }
                if (location.value == null) { message.value = "Debes agregar una ubicación en el mapa"; return@Button }

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
                    "photos" to emptyList<String>(),
                    "latitude" to (location.value?.latitude ?: 0.0),
                    "longitude" to (location.value?.longitude ?: 0.0),
                    "address" to address.value
                )

                db.collection("parking lots").add(baseParkingLot)
                    .addOnSuccessListener { docRef ->
                        val totalPhotos = photoUris.value.size
                        if (totalPhotos == 0) {
                            message.value = "Parqueadero creado con éxito"
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
                                    docRef.update(mapOf("fotos" to uploadedPhotos, "photos" to uploadedPhotos))
                                        .addOnSuccessListener {
                                            message.value = "Parqueadero creado con fotos"
                                            isUploading.value = false
                                            navController.popBackStack()
                                        }.addOnFailureListener { e ->
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
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.8f).height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue))
        ) {
            if (isUploading.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Crear Parqueadero", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        if (message.value.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = message.value,
                color = if (message.value.startsWith("P") || message.value.startsWith("C")) Color(0xFF2E7D32) else Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(30.dp))
    }

    if (showTermsDialog) {
        RulesDialog(
            initialRules = terms.value,
            onDismiss = { showTermsDialog = false },
            onSave = { updatedRules ->
                terms.value = updatedRules
                showTermsDialog = false
            }
        )
    }
    if (showOpeningDialog) {
        TimeEditDialog("Hora de apertura", hourStart.value, { hourStart.value = it; showOpeningDialog = false }, { showOpeningDialog = false })
    }
    if (showClosingDialog) {
        TimeEditDialog("Hora de cierre", hourFinish.value, { hourFinish.value = it; showClosingDialog = false }, { showClosingDialog = false })
    }
}


@Composable
fun EditParkingVisual(
    parkingId: String = "", navController: NavController? = null, modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(50)
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
        Box(Modifier.fillMaxSize().background(colorResource(R.color.back)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colorResource(R.color.blue))
        }
        return
    }

    var showTermsDialog by remember { mutableStateOf(false) }
    var showOpeningDialog by remember { mutableStateOf(false) }
    var showClosingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(color = colorResource(R.color.back))
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier.width(110.dp).height(65.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.height(45.dp).width(2.dp).background(Color.Black))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Editar\nparqueadero",
                color = colorResource(R.color.black),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        FormSection("Información General", Icons.Default.Home) {
            ModernTextField(
                value = name.value,
                onValueChange = { name.value = it },
                label = "Nombre del parqueadero"
            )
            Spacer(Modifier.height(12.dp))
            ModernTextField(
                value = slot.value,
                onValueChange = { slot.value = it },
                label = "Cupos disponibles",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Estación de carga EV", fontSize = 15.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = electricCharges.value,
                    onCheckedChange = { electricCharges.value = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = colorResource(R.color.blue))
                )
            }
        }

        FormSection("Tarifas", Icons.Default.Star) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernTextField(
                    value = pricePerHour.value,
                    onValueChange = { pricePerHour.value = it },
                    label = "$ Precio por hora",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                ModernTextField(
                    value = pricePerMin.value,
                    onValueChange = { pricePerMin.value = it },
                    label = "$ Precio por min",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
            }
            Spacer(Modifier.height(12.dp))
            ModernTextField(
                value = fixedPrice.value,
                onValueChange = { fixedPrice.value = it },
                label = "$ Tarifa plena (Día completo)",
                keyboardType = KeyboardType.Number
            )
        }

        FormSection("Horarios de Atención", Icons.Default.DateRange) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showOpeningDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text("Apertura:\n${hourStart.value}", textAlign = TextAlign.Center)
                }
                OutlinedButton(
                    onClick = { showClosingDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text("Cierre:\n${hourFinish.value}", textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Días de servicio", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

            val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dayLabels.forEachIndexed { index, letter ->
                    DayToggle(
                        letter = letter,
                        isSelected = selectedDays.value[index],
                        onClick = {
                            val newList = selectedDays.value.toMutableList()
                            newList[index] = !newList[index]
                            selectedDays.value = newList
                        }
                    )
                }
            }
        }

        FormSection("Detalles y Multimedia", Icons.Default.List) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showTermsDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = colorResource(R.color.blue))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Reglas del parqueadero", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(if (terms.value.isEmpty()) "Toca para añadir reglas" else "Reglas añadidas", fontSize = 12.sp, color = Color.Gray)
                    }
                    Text("Editar", color = colorResource(R.color.blue), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController?.navigate(AppScreens.MapPicker.name) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (location.value != null) Color(0xFFE8F5E9) else Color(0xFFF5F7FA)),
                border = BorderStroke(1.dp, if (location.value != null) Color(0xFFA5D6A7) else Color(0xFFE0E0E0))
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (location.value != null) Color(0xFF2E7D32) else Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (location.value != null) "Ubicación fijada" else "Ubicación en el mapa", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(if (location.value != null) address.value else "Toca para abrir el mapa", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val totalPhotos = photoUrls.value.size + photoUris.value.size
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Fotos", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("$totalPhotos/15", color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
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
                                .border(1.5.dp, colorResource(R.color.blue), RoundedCornerShape(12.dp))
                                .background(colorResource(R.color.blue).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = colorResource(R.color.blue))
                                Text("Añadir", color = colorResource(R.color.blue), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = !isUploading.value,
            onClick = {
                if (parkingId.isEmpty()) { message.value = "ID de parqueadero inválido"; return@Button }
                isUploading.value = true

                val daysString = selectedDays.value
                    .mapIndexed { i, b -> if (b) listOf("L", "M", "M", "J", "V", "S", "D")[i] else "" }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")

                val totalPh = photoUris.value.size
                if (totalPh == 0) {
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
                                    db, parkingId, name.value, pricePerHour.value, pricePerMin.value,
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
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.8f).height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue))
        ) {
            if (isUploading.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = !isUploading.value,
            onClick = { deleteParking(db, parkingId, message, isUploading, navController) },
            shape = pillShape,
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.8f).height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            if (isUploading.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Eliminar Parqueadero", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
        }

        if (message.value.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = message.value,
                color = if (message.value.startsWith("C") || message.value.startsWith("P")) Color(0xFF2E7D32) else Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(30.dp))
    }

    if (showTermsDialog) {
        RulesDialog(
            initialRules = terms.value,
            onDismiss = { showTermsDialog = false },
            onSave = { updatedRules ->
                terms.value = updatedRules
                showTermsDialog = false
            }
        )
    }
    if (showOpeningDialog) {
        TimeEditDialog("Hora de apertura", hourStart.value, { hourStart.value = it; showOpeningDialog = false }, { showOpeningDialog = false })
    }
    if (showClosingDialog) {
        TimeEditDialog("Hora de cierre", hourFinish.value, { hourFinish.value = it; showClosingDialog = false }, { showClosingDialog = false })
    }
}

@Composable
fun FormSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = colorResource(R.color.blue))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorResource(R.color.blue),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color(0xFFF9F9F9),
            unfocusedContainerColor = Color(0xFFF9F9F9)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

@Composable
fun DayToggle(letter: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isSelected) colorResource(R.color.blue) else Color(0xFFF0F0F0))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun TimeEditDialog(title: String, currentTime: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val parts = currentTime.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    LaunchedEffect(Unit) {
        android.app.TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                onConfirm(formattedTime)
            },
            hour,
            minute,
            true
        ).apply {
            setOnCancelListener { onDismiss() }
            show()
        }
    }
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
    db: FirebaseFirestore, parkingId: String, message: MutableState<String>,
    isUploading: MutableState<Boolean>, navController: NavController?
) {
    if (parkingId.isEmpty()) { message.value = "ID inválido"; return }
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
    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.TopEnd) {
        AsyncImage(
            model = model,
            contentDescription = "Foto",
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun RulesDialog(
    initialRules: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var rulesList by remember { mutableStateOf(initialRules.split("\n").filter { it.isNotBlank() }) }
    var newRuleText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Reglas del Parqueadero", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Agrega las normas una por una. Los conductores las verán como una lista.", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = newRuleText,
                    onValueChange = { newRuleText = it },
                    placeholder = { Text("Ej. No dejar objetos de valor...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.blue), unfocusedBorderColor = Color.Gray
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (newRuleText.isNotBlank()) {
                                rulesList = rulesList + newRuleText.trim()
                                newRuleText = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar", tint = colorResource(R.color.blue))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (rulesList.isEmpty()) {
                        item { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay reglas añadidas.", color = Color.LightGray, modifier = Modifier.padding(top = 32.dp)) } }
                    }
                    items(rulesList) { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(colorResource(R.color.grisClaro), RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colorResource(R.color.blue), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = rule, modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color.DarkGray)
                            IconButton(onClick = { rulesList = rulesList.filter { it != rule } }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            var finalRules = rulesList
                            if (newRuleText.isNotBlank()) { finalRules = finalRules + newRuleText.trim() }
                            onSave(finalRules.joinToString("\n"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)), shape = RoundedCornerShape(50)
                    ) { Text("Guardar Reglas", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}