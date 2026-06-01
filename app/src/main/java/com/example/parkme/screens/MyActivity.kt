package com.example.parkme.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.models.Reservation
import com.example.parkme.models.ReservationHolder
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.AppViewModel
import com.example.parkme.viewmodel.ChatViewModel
import com.google.firebase.firestore.FirebaseFirestore
@Composable
fun MyActivity(navController: NavController, viewModel: AppViewModel = viewModel()) {

    val chatViewModel: ChatViewModel = viewModel()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    var itemSeleccionado by remember { mutableIntStateOf(1) }
    val reservasRaw by viewModel.userReservations.collectAsState()
    val allParkingLots by viewModel.parkingLots.collectAsState()

    val reservasUsuario = reservasRaw.sortedByDescending { it.startTime }

    LaunchedEffect(Unit) {
        viewModel.fetchUserReservations()
        viewModel.fetchParkingLots()
    }

    Scaffold(
        modifier = Modifier.background(color = colorResource(R.color.back)),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp),
                color = colorResource(R.color.gris),
                contentColor = colorResource(R.color.black),
                shadowElevation = 8.dp
            ) {
                NavigationBar(containerColor = Color.Transparent) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio", fontWeight = FontWeight.Bold) },
                        selected = itemSeleccionado == 0,
                        onClick = {
                            itemSeleccionado = 0
                            navController.navigate(AppScreens.HomeUser.name)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Actividad") },
                        label = { Text("Actividad", fontWeight = FontWeight.Bold) },
                        selected = itemSeleccionado == 1,
                        onClick = { itemSeleccionado = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil", fontWeight = FontWeight.Bold) },
                        selected = itemSeleccionado == 2,
                        onClick = {
                            itemSeleccionado = 2
                            navController.navigate(AppScreens.UserProfile.name)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )
                }
            }
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .background(color = colorResource(R.color.back))
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp)
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
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Mi\nActividad",
                    color = colorResource(R.color.black),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start
                )
            }

            if (reservasUsuario.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no tienes actividad registrada", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        val reservaReciente = reservasUsuario.first()
                        val parqueaderoReciente = allParkingLots.find { it.id == reservaReciente.parkingId }
                        val primeraFoto = parqueaderoReciente?.photos?.firstOrNull()

                        Text(
                            text = "Actividad Reciente",
                            color = colorResource(R.color.black),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column {
                                if (primeraFoto != null) {
                                    AsyncImage(
                                        model = primeraFoto,
                                        contentDescription = "Foto del parqueo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.default1),
                                        contentDescription = "Sin foto",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                    )
                                }

                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = reservaReciente.parkingName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp,
                                            color = Color.Black,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = if (reservaReciente.status == "Activa" || reservaReciente.status == "Activo") Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = reservaReciente.status,
                                                color = if (reservaReciente.status == "Activa" || reservaReciente.status == "Activo") colorResource(R.color.blue) else Color.Gray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Ingreso: ${reservaReciente.startTime}", fontSize = 15.sp, color = Color.DarkGray)
                                    Text(text = "Tarifa: ${parqueaderoReciente?.pricePerHour ?: "$0"} / hora", fontSize = 15.sp, color = Color.DarkGray)

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!reservaReciente.isRated) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    navController.currentBackStackEntry?.savedStateHandle?.set("rateParkingId", reservaReciente.parkingId)
                                                    navController.currentBackStackEntry?.savedStateHandle?.set("rateReservationId", reservaReciente.id)
                                                    navController.navigate(AppScreens.RateParkingLot.name)
                                                }
                                            ) {
                                                Icon(Icons.Outlined.Star, contentDescription = "Calificar", tint = colorResource(R.color.blue), modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Calificar", fontSize = 16.sp, color = colorResource(R.color.blue), fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Star, contentDescription = "Calificado", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Calificado", color = Color.Gray, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (reservaReciente.status == "Activa" || reservaReciente.status == "Activo") {
                                            Button(
                                                onClick = {
                                                    navController.navigate("${AppScreens.TrackUserMap.name}/${reservaReciente.id}")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text("Ver Ruta", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (parqueaderoReciente != null) {
                                                        navController.currentBackStackEntry?.savedStateHandle?.set("ubicacionBuscada", parqueaderoReciente.location)
                                                        navController.currentBackStackEntry?.savedStateHandle?.set("preSelectedParkingId", parqueaderoReciente.id)
                                                        navController.navigate(AppScreens.SearchMap.name)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text("Reservar de nuevo", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    if (reservaReciente.status == "Activa" || reservaReciente.status == "Activo") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = Color(0xFFEEEEEE))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    chatViewModel.initChatRoom(reservaReciente)
                                                    ReservationHolder.selectedReservationId = reservaReciente.id
                                                    navController.navigate(AppScreens.ChatCli.name)
                                                },
                                                shape = RoundedCornerShape(50),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.blue)),
                                                border = BorderStroke(1.dp, colorResource(R.color.blue))
                                            ) {
                                                Text("Abrir Chat", fontWeight = FontWeight.Bold)
                                            }

                                            var localSharingState by remember(reservaReciente.sharingLocation) {
                                                mutableStateOf(reservaReciente.sharingLocation)
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (localSharingState) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (localSharingState) "Compartiendo" else "Ubicación",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (localSharingState) Color(0xFF2E7D32) else Color.DarkGray
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Switch(
                                                    checked = localSharingState,
                                                    onCheckedChange = { isSharing ->
                                                        if (isSharing && !hasLocationPermission) {
                                                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                                        } else {
                                                            localSharingState = isSharing
                                                            db.collection("reservas").document(reservaReciente.id).update("sharingLocation", isSharing)
                                                            if (isSharing) viewModel.startTrackingUserLocation(context, reservaReciente.userId)
                                                            else viewModel.stopTrackingUserLocation()
                                                        }
                                                    },
                                                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50), uncheckedTrackColor = Color.Gray),
                                                    modifier = Modifier.scale(0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (reservasUsuario.size > 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Historial",
                                color = colorResource(R.color.black),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    val restoReservas = reservasUsuario.drop(1)
                    items(restoReservas) { reserva ->
                        val parqueoData = allParkingLots.find { it.id == reserva.parkingId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = reserva.parkingName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                                        Text(text = "${reserva.startTime} - ${reserva.status}", fontSize = 14.sp, color = Color.DarkGray)

                                        if (!reserva.isRated) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .padding(top = 8.dp)
                                                    .clickable {
                                                        navController.currentBackStackEntry?.savedStateHandle?.set("rateParkingId", reserva.parkingId)
                                                        navController.currentBackStackEntry?.savedStateHandle?.set("rateReservationId", reserva.id)
                                                        navController.navigate(AppScreens.RateParkingLot.name)
                                                    }
                                            ) {
                                                Icon(Icons.Outlined.Star, contentDescription = "Calificar", tint = colorResource(R.color.blue), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Calificar", color = colorResource(R.color.blue), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 8.dp)
                                            ) {
                                                Icon(Icons.Filled.Star, contentDescription = "Calificado", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Calificado", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    if (reserva.status == "Activa" || reserva.status == "Activo") {
                                        Button(
                                            onClick = {
                                                navController.navigate("${AppScreens.TrackUserMap.name}/${reserva.id}")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue).copy(alpha = 0.1f)),
                                            shape = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Ver Ruta", color = colorResource(R.color.blue), fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                if (parqueoData != null) {
                                                    navController.currentBackStackEntry?.savedStateHandle?.set("ubicacionBuscada", parqueoData.location)
                                                    navController.currentBackStackEntry?.savedStateHandle?.set("preSelectedParkingId", parqueoData.id)
                                                    navController.navigate(AppScreens.SearchMap.name)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue).copy(alpha = 0.1f)),
                                            shape = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Volver", color = colorResource(R.color.blue), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (reserva.status == "Activa" || reserva.status == "Activo") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFFEEEEEE))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                chatViewModel.initChatRoom(reserva)
                                                ReservationHolder.selectedReservationId = reserva.id
                                                navController.navigate(AppScreens.ChatCli.name)
                                            },
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.blue)),
                                            border = BorderStroke(1.dp, colorResource(R.color.blue))
                                        ) {
                                            Text("Abrir Chat", fontWeight = FontWeight.Bold)
                                        }

                                        var localSharingState by remember(reserva.sharingLocation) {
                                            mutableStateOf(reserva.sharingLocation)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(if (localSharingState) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (localSharingState) "Compartiendo" else "Ubicación",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (localSharingState) Color(0xFF2E7D32) else Color.DarkGray
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Switch(
                                                checked = localSharingState,
                                                onCheckedChange = { isSharing ->
                                                    if (isSharing && !hasLocationPermission) {
                                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                                    } else {
                                                        localSharingState = isSharing
                                                        db.collection("reservas").document(reserva.id).update("sharingLocation", isSharing)
                                                        if (isSharing) viewModel.startTrackingUserLocation(context, reserva.userId)
                                                        else viewModel.stopTrackingUserLocation()
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50), uncheckedTrackColor = Color.Gray),
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyActivityOperator(navController: NavController, viewModel: AppViewModel = viewModel()) {
    val db = FirebaseFirestore.getInstance()
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var itemSeleccionado by remember { mutableIntStateOf(1) }
    val parkingLots by viewModel.operatorParkingLots.collectAsState()

    var liveReservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    val chatViewModel: ChatViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.fetchOperatorActivity()
    }

    DisposableEffect(uid) {
        if (uid.isEmpty()) return@DisposableEffect onDispose {}
        val listener = db.collection("reservas").whereEqualTo("operatorId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    liveReservations = snapshot.documents.map { doc ->
                        val res = doc.toObject(Reservation::class.java) ?: Reservation()
                        res.copy(id = doc.id)
                    }
                }
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        modifier = Modifier.background(color = colorResource(R.color.back)),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp),
                color = colorResource(R.color.gris),
                contentColor = colorResource(R.color.black),
                shadowElevation = 8.dp
            ) {
                NavigationBar(containerColor = Color.Transparent) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio", fontWeight = FontWeight.Bold) },
                        selected = itemSeleccionado == 0,
                        onClick = {
                            itemSeleccionado = 0
                            navController.navigate(AppScreens.HomeOperator.name)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Actividad") },
                        label = { Text("Actividad", fontWeight = FontWeight.Bold) },
                        selected = itemSeleccionado == 1,
                        onClick = { itemSeleccionado = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil", fontWeight = FontWeight.Bold) },
                        selected = itemSeleccionado == 2,
                        onClick = {
                            itemSeleccionado = 2
                            navController.navigate(AppScreens.OperatorProfile.name)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )
                }
            }
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .background(color = colorResource(R.color.back))
                .padding(paddingValues)
                .statusBarsPadding()
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp)
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
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Panel de\nActividad",
                    color = colorResource(R.color.black),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }

            if (parkingLots.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes parqueaderos ni actividad aún", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(parkingLots) { lot ->
                        val lotReservations = liveReservations.filter { it.parkingId == lot.id }
                        val activeReservations = lotReservations.filter { it.status == "Activa" || it.status == "Activo" }
                        val ocupados = activeReservations.size
                        val cuposDisponibles = maxOf(0, lot.slot - ocupados)
                        val completedReservations = lotReservations.filter { it.status == "Completada" || it.status == "Completado" || it.status == "Finalizada" }
                        val ganancias = completedReservations.sumOf { it.totalPrice }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(text = lot.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cupos disponibles", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "$cuposDisponibles/${lot.slot}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorResource(R.color.blue),
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                val prog = if (lot.slot > 0) ocupados.toFloat() / lot.slot.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { prog },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = colorResource(R.color.blue),
                                    trackColor = Color(0xFFEEEEEE)
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ganancias históricas", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "$ ${ganancias.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorResource(R.color.verdepasto),
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Servicios activos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.height(12.dp))

                                if (activeReservations.isEmpty()) {
                                    Text("No hay vehículos actualmente", color = Color.Gray, fontSize = 13.sp)
                                } else {
                                    val recentServices = activeReservations.sortedByDescending { it.startTime }
                                    recentServices.forEach { reserva ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(
                                                        text = "Placa: ${reserva.licensePlate.ifEmpty { "N/A" }}",
                                                        color = Color.Black,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Text(
                                                        text = "$ ${reserva.totalPrice.toInt()}",
                                                        color = colorResource(R.color.rojooscuro),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Ingreso: ${reserva.startTime}", color = Color.Gray, fontSize = 13.sp)

                                                Spacer(modifier = Modifier.height(12.dp))

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            chatViewModel.initChatRoom(reserva)
                                                            ReservationHolder.selectedReservationId = reserva.id
                                                            navController.navigate(AppScreens.ChatOp.name)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(42.dp),
                                                        contentPadding = PaddingValues(0.dp),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text("Abrir Chat", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            if (reserva.sharingLocation) {
                                                                navController.navigate("${AppScreens.TrackUserMap.name}/${reserva.id}")
                                                            }
                                                        },
                                                        enabled = reserva.sharingLocation,
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF4CAF50),
                                                            disabledContainerColor = Color(0xFFE0E0E0)
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(42.dp),
                                                        contentPadding = PaddingValues(0.dp),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text(
                                                            text = if (reserva.sharingLocation) "Ver Mapa" else "Sin GPS",
                                                            color = if (reserva.sharingLocation) Color.White else Color.Gray,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}