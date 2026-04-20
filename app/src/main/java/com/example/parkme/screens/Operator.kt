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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens
import com.example.parkme.utils.CloudinaryUploader
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch


@Composable
fun CreateParkingVisual(navController: NavController, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(50)
    val card = RoundedCornerShape(24)
    val backStackEntry = navController.currentBackStackEntry

    val scroll = rememberScrollState()
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
    val mensaje = rememberSaveable { mutableStateOf("") }
    val subiendo = rememberSaveable { mutableStateOf(false) }
    val ubicacion = remember { mutableStateOf<LatLng?>(null) }

    val fotosUris = remember {
        mutableStateOf<List<Uri>>(
            backStackEntry?.savedStateHandle?.get<ArrayList<Uri>>("fotosUris") ?: emptyList()
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val nuevaLista = fotosUris.value + uris
        fotosUris.value = nuevaLista
        backStackEntry?.savedStateHandle?.set("fotosUris", ArrayList(nuevaLista))
    }

    var mostrarDialogoTerms by rememberSaveable { mutableStateOf(false) }
    var mostrarDialogoApertura by rememberSaveable { mutableStateOf(false) }
    var mostrarDialogoCierre by rememberSaveable { mutableStateOf(false) }

    val latLng by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<LatLng?>("latLng", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    val diasSeleccionados = remember {
        mutableStateOf<List<Boolean>>(
            backStackEntry?.savedStateHandle?.get<ArrayList<Boolean>>("dias")
                ?: listOf(true, false, true, false, true, false, true)
        )
    }
    LaunchedEffect(diasSeleccionados.value) {
        backStackEntry?.savedStateHandle?.set("dias_seleccionados", diasSeleccionados.value)
    }

    LaunchedEffect(latLng) {
        if (latLng != null) ubicacion.value = latLng
    }

    Column(
        modifier = Modifier
            .background(color = colorResource(R.color.back))
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier.width(160.dp).height(95.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.height(70.dp).width(2.dp).background(Color.Black))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Crear\nparqueadero",
                color = colorResource(R.color.black),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
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
                    modifier = Modifier.width(140.dp).height(56.dp),
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
                    onClick = { mostrarDialogoTerms = true },
                    shape = pill,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Ingresar", color = Color.Blue, fontWeight = FontWeight.Bold)
                }
            }
        )

        if (mostrarDialogoTerms) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoTerms = false },
                title = { Text("Reglas del parqueadero") },
                text = {
                    OutlinedTextField(
                        value = terms.value,
                        onValueChange = { terms.value = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        label = { Text("Escribe las reglas aquí") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { mostrarDialogoTerms = false }) { Text("Listo") }
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colorResource(R.color.grisClaro), card)
                .padding(14.dp)
        ) {
            Column {
                Text("Fotos del parqueadero", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fotosUris.value.take(3).forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
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

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                navController.navigate(AppScreens.MapPicker.name) {
                    launchSingleTop = true
                }
            },
            shape = pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (ubicacion.value != null) colorResource(R.color.blue) else Color(0xFFE0E0E0)
            ),
            modifier = Modifier.fillMaxWidth(0.72f).align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (ubicacion.value != null) "Ubicación agregada" else "Agregar Ubicación",
                color = if (ubicacion.value != null) Color.White else Color.DarkGray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        LabelAndRight(
            label = "Hora de apertura",
            right = {
                Box(modifier = Modifier.clickable { mostrarDialogoApertura = true }) {
                    TimePill(hourStart.value)
                }
            }
        )
        LabelAndRight(
            label = "Hora de cierre",
            right = {
                Box(modifier = Modifier.clickable { mostrarDialogoCierre = true }) {
                    TimePill(hourFinish.value)
                }
            }
        )

        if (mostrarDialogoApertura) {
            HoraDialog(
                titulo = "Hora de apertura",
                horaActual = hourStart.value,
                onConfirm = { hourStart.value = it; mostrarDialogoApertura = false },
                onDismiss = { mostrarDialogoApertura = false }
            )
        }
        if (mostrarDialogoCierre) {
            HoraDialog(
                titulo = "Hora de cierre",
                horaActual = hourFinish.value,
                onConfirm = { hourFinish.value = it; mostrarDialogoCierre = false },
                onDismiss = { mostrarDialogoCierre = false }
            )
        }

        Spacer(Modifier.height(12.dp))

        val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEach { d -> DayLetter(d) }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            diasSeleccionados.value.forEachIndexed { index, seleccionado ->
                CircleCheckClickable(
                    checked = seleccionado,
                    onClick = {
                        val nuevaLista = diasSeleccionados.value.toMutableList()
                        nuevaLista[index] = !seleccionado
                        diasSeleccionados.value = nuevaLista
                        backStackEntry?.savedStateHandle?.set("dias", ArrayList(nuevaLista))
                    }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            enabled = !subiendo.value,
            onClick = {
                if (uid.isEmpty()) {
                    mensaje.value = "Usuario no autenticado"
                    return@Button
                }
                if (ubicacion.value == null) {
                    mensaje.value = "Debes agregar una ubicación en el mapa"
                    return@Button
                }

                subiendo.value = true

                val diasString = diasSeleccionados.value
                    .mapIndexed { i, b -> if (b) listOf("L", "M", "M", "J", "V", "S", "D")[i] else "" }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")

                val parqueaderoBase = hashMapOf(
                    "operadorId" to uid,
                    "name" to name.value,
                    "pricePerHour" to pricePerHour.value,
                    "pricePerMin" to pricePerMin.value,
                    "fixedPrice" to fixedPrice.value,
                    "terms" to terms.value,
                    "electricCharges" to electricCharges.value,
                    "hourStart" to hourStart.value,
                    "hourFinish" to hourFinish.value,
                    "weekAvailability" to diasString,
                    "slot" to (slot.value.toIntOrNull() ?: 0),
                    "fotos" to emptyList<String>(),
                    "latitud" to (ubicacion.value?.latitude ?: 0.0),
                    "longitud" to (ubicacion.value?.longitude ?: 0.0)
                )

                db.collection("parqueaderos").add(parqueaderoBase)
                    .addOnSuccessListener { docRef ->
                        val totalFotos = fotosUris.value.size
                        if (totalFotos == 0) {
                            mensaje.value = " Parqueadero creado"
                            subiendo.value = false
                            return@addOnSuccessListener
                        }
                        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                        scope.launch {
                            val fotosSubidas = mutableListOf<String>()
                            fotosUris.value.forEach { uri ->
                                val url = CloudinaryUploader.uploadImage(context, uri)
                                if (url != null) fotosSubidas.add(url)
                            }
                            if (fotosSubidas.size == totalFotos) {
                                docRef.update("fotos", fotosSubidas)
                                    .addOnSuccessListener {
                                        mensaje.value = "Parqueadero creado con fotos"
                                        subiendo.value = false
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener { e ->
                                        mensaje.value = "Error guardando fotos: ${e.message}"
                                        subiendo.value = false
                                    }
                            } else {
                                mensaje.value = "Algunas fotos no se subieron"
                                subiendo.value = false
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        mensaje.value = "Error creando parqueadero: ${e.message}"
                        subiendo.value = false
                    }
            },
            shape = pill,
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.6f).height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue))
        ) {
            if (subiendo.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Crear", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        if (mensaje.value.isNotEmpty()) {
            Text(
                text = mensaje.value,
                color = if (mensaje.value.startsWith("P")) Color.Green else Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}


@Composable
fun EditParkingVisual(parkingId: String = "", navController: NavController? = null, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(50)
    val card = RoundedCornerShape(24)
    val scroll = rememberScrollState()
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
    val diasSeleccionados = remember { mutableStateOf(listOf(true, false, true, false, true, false, true)) }
    val mensaje = remember { mutableStateOf("") }
    val cargando = remember { mutableStateOf(true) }
    val subiendo = remember { mutableStateOf(false) }
    val fotosUris = remember { mutableStateOf<List<Uri>>(emptyList()) }
    val fotosUrls = remember { mutableStateOf<List<String>>(emptyList()) }
    val ubicacion = remember { mutableStateOf<LatLng?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> fotosUris.value = fotosUris.value + uris }

    val latLng by navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<LatLng?>("latLng", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(latLng) {
        if (latLng != null) ubicacion.value = latLng
    }

    LaunchedEffect(parkingId) {
        if (parkingId.isNotEmpty()) {
            db.collection("parqueaderos").document(parkingId).get()
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

                    val lat = doc.getDouble("latitud")
                    val lng = doc.getDouble("longitud")
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        ubicacion.value = LatLng(lat, lng)
                    }

                    val weekAvail = doc.getString("weekAvailability") ?: ""
                    val dias = listOf("L", "M", "M", "J", "V", "S", "D")
                    diasSeleccionados.value = dias.map { weekAvail.contains(it) }

                    val fotosFirestore = doc.get("fotos")
                    if (fotosFirestore is List<*>) {
                        fotosUrls.value = fotosFirestore.filterIsInstance<String>()
                    }

                    cargando.value = false
                }
                .addOnFailureListener {
                    mensaje.value = "Error al cargar datos"
                    cargando.value = false
                }
        } else {
            cargando.value = false
        }
    }

    if (cargando.value) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .background(color = colorResource(R.color.back))
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier.width(160.dp).height(95.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.height(70.dp).width(2.dp).background(Color.Black))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Editar\nparqueadero",
                color = colorResource(R.color.black),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
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
                    modifier = Modifier.width(140.dp).height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.grisClaro),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        )
        var mostrarDialogoEliminar by remember { mutableStateOf(false) }
        var mostrarDialogoTerms by remember { mutableStateOf(false) }
        LabelAndRight(
            label = "Reglas del parqueadero",
            right = {
                Button(
                    onClick = { mostrarDialogoTerms = true },
                    shape = pill,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Ingresar", color = Color.Blue, fontWeight = FontWeight.Bold)
                }
            }
        )
        if (mostrarDialogoTerms) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoTerms = false },
                title = { Text("Reglas del parqueadero") },
                text = {
                    OutlinedTextField(
                        value = terms.value,
                        onValueChange = { terms.value = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        label = { Text("Escribe las reglas aquí") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { mostrarDialogoTerms = false }) { Text("Listo") }
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colorResource(R.color.grisClaro), card)
                .padding(14.dp)
        ) {
            Column {
                Text("Fotos del parqueadero", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fotosUrls.value.take(3).forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    fotosUris.value.take(3 - fotosUrls.value.size).forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
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

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { navController?.navigate(AppScreens.MapPicker.name) },
            shape = pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (ubicacion.value != null) colorResource(R.color.blue) else Color(0xFFE0E0E0)
            ),
            modifier = Modifier.fillMaxWidth(0.72f).align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (ubicacion.value != null) "Ubicación agregada" else "Agregar Ubicación",
                color = if (ubicacion.value != null) Color.White else Color.DarkGray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        var mostrarDialogoApertura by remember { mutableStateOf(false) }
        var mostrarDialogoCierre by remember { mutableStateOf(false) }

        LabelAndRight(
            label = "Hora de apertura",
            right = {
                Box(modifier = Modifier.clickable { mostrarDialogoApertura = true }) {
                    TimePill(hourStart.value)
                }
            }
        )
        LabelAndRight(
            label = "Hora de cierre",
            right = {
                Box(modifier = Modifier.clickable { mostrarDialogoCierre = true }) {
                    TimePill(hourFinish.value)
                }
            }
        )

        if (mostrarDialogoApertura) {
            HoraDialog(
                titulo = "Hora de apertura",
                horaActual = hourStart.value,
                onConfirm = { hourStart.value = it; mostrarDialogoApertura = false },
                onDismiss = { mostrarDialogoApertura = false }
            )
        }
        if (mostrarDialogoCierre) {
            HoraDialog(
                titulo = "Hora de cierre",
                horaActual = hourFinish.value,
                onConfirm = { hourFinish.value = it; mostrarDialogoCierre = false },
                onDismiss = { mostrarDialogoCierre = false }
            )
        }

        Spacer(Modifier.height(12.dp))

        val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEach { d -> DayLetter(d) }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            diasSeleccionados.value.forEachIndexed { index, seleccionado ->
                CircleCheckClickable(
                    checked = seleccionado,
                    onClick = {
                        val nuevaLista = diasSeleccionados.value.toMutableList()
                        nuevaLista[index] = !seleccionado
                        diasSeleccionados.value = nuevaLista
                    }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            enabled = !subiendo.value,
            onClick = {
                if (parkingId.isEmpty()) {
                    mensaje.value = "ID de parqueadero inválido"
                    return@Button
                }
                subiendo.value = true

                val diasString = diasSeleccionados.value
                    .mapIndexed { i, b -> if (b) listOf("L", "M", "M", "J", "V", "S", "D")[i] else "" }
                    .filter { it.isNotEmpty() }
                    .joinToString(",")

                val totalFotos = fotosUris.value.size
                if (totalFotos == 0) {
                    guardarDatos(
                        db, parkingId, name.value, pricePerHour.value, pricePerMin.value,
                        fixedPrice.value, terms.value, electricCharges.value,
                        hourStart.value, hourFinish.value, diasString,
                        slot.value.toIntOrNull() ?: 0,
                        fotosUrls.value, ubicacion.value,
                        mensaje, subiendo, navController
                    )
                } else {
                    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                    scope.launch {
                        val fotosSubidas = mutableListOf<String>()
                        fotosUris.value.forEach { uri ->
                            val url = CloudinaryUploader.uploadImage(context, uri)
                            if (url != null) fotosSubidas.add(url)
                        }
                        val todasLasFotos = fotosUrls.value + fotosSubidas
                        guardarDatos(
                            db, parkingId, name.value , pricePerHour.value, pricePerMin.value,
                            fixedPrice.value, terms.value, electricCharges.value,
                            hourStart.value, hourFinish.value, diasString,
                            slot.value.toIntOrNull() ?: 0,
                            todasLasFotos, ubicacion.value,
                            mensaje, subiendo, navController
                        )
                    }
                }
            },
            shape = pill,
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.6f).height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue))
        ) {
            if (subiendo.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !subiendo.value,
            onClick = {
                // Llamamos a la función externa directamente
                eliminarParqueadero(db, parkingId, mensaje, subiendo, navController)
            },
            shape = pill,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.6f)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            if (subiendo.value) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Eliminar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (mensaje.value.isNotEmpty()) {
            Text(
                text = mensaje.value,
                color = if (mensaje.value.startsWith("P")) Color.Green else Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}


@Composable
fun LabelAndRight(label: String, right: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
        modifier = Modifier.size(28.dp).clickable { onClick() },
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.Black)
    ) {
        if (checked) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Check", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun TimePill(time: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFE0E0E0),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = Color(0xFF1565C0),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DayLetter(letter: String) {
    Surface(
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = Color(0xFFE0E0E0)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(letter, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HoraDialog(titulo: String, horaActual: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var hora by remember { mutableStateOf(horaActual) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = hora,
                onValueChange = { hora = it },
                label = { Text("HH:MM") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hora) }) { Text("Listo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

fun guardarDatos(
    db: FirebaseFirestore,
    parkingId: String,
    name : String,
    pricePerHour: String,
    pricePerMin: String,
    fixedPrice: String,
    terms: String,
    electricCharges: Boolean,
    hourStart: String,
    hourFinish: String,
    weekAvailability: String,
    slot: Int,
    fotos: List<String>,
    ubicacion: LatLng?,
    mensaje: MutableState<String>,
    subiendo: MutableState<Boolean>,
    navController: NavController?
) {
    val datos = hashMapOf(
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
        "fotos" to fotos,
        "latitud" to (ubicacion?.latitude ?: 0.0),
        "longitud" to (ubicacion?.longitude ?: 0.0)
    )
    db.collection("parqueaderos").document(parkingId)
        .set(datos, SetOptions.merge())
        .addOnSuccessListener {
            mensaje.value = "Cambios guardados"
            subiendo.value = false
            navController?.popBackStack()
        }
        .addOnFailureListener { e ->
            mensaje.value = "Error: ${e.message}"
            subiendo.value = false
        }
}

fun eliminarParqueadero(
    db: FirebaseFirestore,
    parkingId: String,
    mensaje: MutableState<String>,
    subiendo: MutableState<Boolean>,
    navController: NavController?
) {
    if (parkingId.isEmpty()) {
        mensaje.value = "ID de parqueadero inválido"
        return
    }

    subiendo.value = true

    db.collection("parqueaderos").document(parkingId)
        .delete()
        .addOnSuccessListener {
            mensaje.value = "Parqueadero eliminado"
            subiendo.value = false
            navController?.popBackStack()
        }
        .addOnFailureListener { e ->
            mensaje.value = "Error al eliminar: ${e.message}"
            subiendo.value = false
        }
}

