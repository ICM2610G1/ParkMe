package com.example.parkme.screens

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
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.AppViewModel

@Composable
fun MyActivity(navController: NavController, viewModel: AppViewModel = viewModel()) {

    val chatViewModel: com.example.parkme.viewmodel.ChatViewModel = viewModel()

    var itemSeleccionado by remember { mutableIntStateOf(1) }
    val reservasRaw by viewModel.userReservations.collectAsState()
    val allParkingLots by viewModel.parkingLots.collectAsState()

    val reservasUsuario = reservasRaw.sortedByDescending { it.startTime }

    LaunchedEffect(Unit) {
        viewModel.fetchUserReservations()
        viewModel.fetchParkingLots()
    }
    Scaffold(
        modifier = Modifier.background(color = colorResource(R.color.back)), bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp),
                color = colorResource(R.color.gris),
                contentColor = colorResource(R.color.black),
                shadowElevation = 8.dp
            ) {
                NavigationBar(
                    containerColor = Color.Transparent
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Home, contentDescription = "Inicio"
                            )
                        },
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
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List, contentDescription = "Actividad"
                            )
                        },
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
                        icon = {
                            Icon(
                                Icons.Default.Person, contentDescription = "Perfil"
                            )
                        },
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
                        .width(160.dp)
                        .height(95.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .height(70.dp)
                        .width(2.dp)
                        .background(Color.Black)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Mi\nActividad",
                    color = colorResource(R.color.black),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Start
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "Todos tus parqueos",
                color = colorResource(R.color.black),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (reservasUsuario.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text("Aún no tienes actividad registrada", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        val reservaReciente = reservasUsuario.first()
                        val parqueaderoReciente =
                            allParkingLots.find { it.id == reservaReciente.parkingId }
                        val primeraFoto = parqueaderoReciente?.photos?.firstOrNull()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(2.dp, Color.Gray, RoundedCornerShape(24.dp))
                                .background(Color.Transparent)
                                .padding(bottom = 16.dp)
                        ) {
                            Column {
                                if (primeraFoto != null) {
                                    AsyncImage(
                                        model = primeraFoto,
                                        contentDescription = "Foto del parqueo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 24.dp, topEnd = 24.dp
                                                )
                                            )
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.parqueadero1),
                                        contentDescription = "Sin foto",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 24.dp, topEnd = 24.dp
                                                )
                                            )
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            reservaReciente.parkingName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = "${reservaReciente.startTime} - ${reservaReciente.status}",
                                            fontSize = 16.sp,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = "${parqueaderoReciente?.pricePerHour ?: "$0"} por hora",
                                            fontSize = 16.sp,
                                            color = Color.DarkGray
                                        )
                                        if (!reservaReciente.isRated) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .padding(top = 8.dp)
                                                    .clickable {
                                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                                            "rateParkingId",
                                                            reservaReciente.parkingId
                                                        )
                                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                                            "rateReservationId", reservaReciente.id
                                                        )
                                                        navController.navigate(AppScreens.RateParkingLot.name)
                                                    }) {
                                                Icon(
                                                    Icons.Outlined.Star,
                                                    contentDescription = "Calificar",
                                                    tint = colorResource(R.color.blue),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Calificar",
                                                    fontSize = 18.sp,
                                                    color = colorResource(R.color.blue),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                "★ Calificado",
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Button(
                                            onClick = {
                                                if (parqueaderoReciente != null) {
                                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                                        "ubicacionBuscada",
                                                        parqueaderoReciente.location
                                                    )
                                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                                        "preSelectedParkingId",
                                                        parqueaderoReciente.id
                                                    )
                                                    navController.navigate(AppScreens.SearchMap.name)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colorResource(
                                                    R.color.blue
                                                )
                                            ),
                                            shape = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(
                                                horizontal = 16.dp, vertical = 8.dp
                                            )
                                        ) {
                                            Text(
                                                "Reservar\nde nuevo",
                                                textAlign = TextAlign.Center,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (reservaReciente.status == "Activa") {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    chatViewModel.iniciarChatRoom(reservaReciente)
                                                    navController.navigate(AppScreens.ChatListCli.name)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colorResource(
                                                        R.color.blue
                                                    )
                                                ),
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(
                                                    horizontal = 16.dp, vertical = 8.dp
                                                )
                                            ) {
                                                Text(
                                                    "Iniciar Chat",
                                                    textAlign = TextAlign.Center,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    val restoReservas = reservasUsuario.drop(1)
                    items(restoReservas) { reserva ->
                        val parqueoData = allParkingLots.find { it.id == reserva.parkingId }

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reserva.parkingName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "${reserva.startTime} - ${reserva.status}",
                                        fontSize = 16.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "${parqueoData?.pricePerHour ?: "$0"} por hora",
                                        fontSize = 16.sp,
                                        color = Color.DarkGray
                                    )
                                    if (!reserva.isRated) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Star,
                                                contentDescription = "Calificar",
                                                tint = colorResource(R.color.blue),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Calificar experiencia",
                                                color = colorResource(R.color.blue),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .padding(top = 8.dp)
                                                    .clickable {
                                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                                            "rateParkingId", reserva.parkingId
                                                        )
                                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                                            "rateReservationId", reserva.id
                                                        )
                                                        navController.navigate(AppScreens.RateParkingLot.name)
                                                    })

                                        }
                                    } else {
                                        Text(
                                            "★ Calificado",
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }

                                }

                                Button(
                                    onClick = {
                                        if (parqueoData != null) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "ubicacionBuscada", parqueoData.location
                                            )
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "preSelectedParkingId", parqueoData.id
                                            )
                                            navController.navigate(AppScreens.SearchMap.name)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(
                                            R.color.blue
                                        )
                                    ),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp, vertical = 8.dp
                                    ),
                                    modifier = Modifier.padding(start = 12.dp)
                                ) {

                                    Text(
                                        "Reservar\nde nuevo",
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun MyActivityOperator(navController: NavController, viewModel: AppViewModel = viewModel()) {
    var itemSeleccionado by remember { mutableIntStateOf(1) }
    val parkingLots by viewModel.operatorParkingLots.collectAsState()
    val reservations by viewModel.operatorReservations.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchOperatorActivity()
    }

    Scaffold(
        modifier = Modifier.background(color = colorResource(R.color.back)), bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp),
                color = colorResource(R.color.gris),
                contentColor = colorResource(R.color.black),
                shadowElevation = 8.dp
            ) {
                NavigationBar(
                    containerColor = Color.Transparent
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Home, contentDescription = "Inicio"
                            )
                        },
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
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List, contentDescription = "Actividad"
                            )
                        },
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
                        icon = {
                            Icon(
                                Icons.Default.Person, contentDescription = "Perfil"
                            )
                        },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
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
                        .height(60.dp)
                        .width(2.dp)
                        .background(Color.Black)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Mi\nActividad",
                    color = colorResource(R.color.black),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (parkingLots.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes parqueaderos ni actividad aún", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(parkingLots) { lot ->
                        val lotReservations = reservations.filter { it.parkingId == lot.id }
                        val activeReservations =
                            lotReservations.filter { it.status == "Activa" || it.status == "Activo" }
                        val ocupados = activeReservations.size
                        val cuposDisponibles = maxOf(0, lot.slot - ocupados)

                        val completedReservations =
                            lotReservations.filter { it.status == "Completada" || it.status == "Completado" || it.status == "Finalizada" }
                        val ganancias = completedReservations.sumOf { it.totalPrice }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, Color.Gray, RoundedCornerShape(24.dp))
                                .background(Color.Transparent, RoundedCornerShape(24.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = lot.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Cupos disponibles:", fontSize = 15.sp, color = Color.Black
                                    )
                                    Text(
                                        text = "$cuposDisponibles/${lot.slot}",
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(R.color.blue),
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val prog =
                                    if (lot.slot > 0) ocupados.toFloat() / lot.slot.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { prog },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = colorResource(R.color.blue),
                                    trackColor = Color.LightGray
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Ganancias históricas:",
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "$ ${ganancias.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(R.color.verdepasto),
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "Servicios recientes", fontSize = 14.sp, color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (lotReservations.isEmpty()) {
                                    Text(
                                        "Aún no tienes servicios registrados",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    val recentServices =
                                        lotReservations.sortedByDescending { it.startTime }.take(3)
                                    recentServices.forEach { reserva ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.5.dp, Color.Gray, RoundedCornerShape(16.dp)
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Vehículo Placa: ${reserva.placa.ifEmpty { "N/A" }}",
                                                    color = Color.Black,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Ingreso: ${reserva.startTime}",
                                                    color = if (reserva.status == "Activa") colorResource(R.color.amarillodorado) else colorResource(R.color.blue),
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "Estado: ${reserva.status}",
                                                        color = Color.Black,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "$ ${reserva.totalPrice.toInt()}",
                                                        color = if (reserva.status == "Activa") colorResource(R.color.rojooscuro) else colorResource(R.color.verdepasto),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
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
@Preview
fun Activitypreview() {
    val navController = rememberNavController()
    MyActivity(navController = navController)
}