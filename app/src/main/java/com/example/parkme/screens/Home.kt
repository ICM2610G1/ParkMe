package com.example.parkme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import android.location.Geocoder
import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.google.android.gms.maps.model.LatLng
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens
import com.example.parkme.models.SearchMapLocationHolder
import com.example.parkme.viewmodel.AppViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeUser(navController: NavController, viewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    var addressSuggestions by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val geocoder = remember { Geocoder(context) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableIntStateOf(0) }
    val userReservations by viewModel.userReservations.collectAsState()
    val allParkingLots by viewModel.parkingLots.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchUserReservations()
        viewModel.fetchParkingLots()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 3) {
            delay(500)
            withContext(Dispatchers.IO) {
                try {
                    val results = geocoder.getFromLocationName(
                        searchQuery,
                        5,
                        -4.22,
                        -79.27,
                        12.59,
                        -66.86
                    )

                    addressSuggestions = results ?: emptyList()
                    isDropdownExpanded = addressSuggestions.isNotEmpty()
                } catch (e: Exception) {
                    addressSuggestions = emptyList()
                }
            }
        } else {
            addressSuggestions = emptyList()
            isDropdownExpanded = false
        }
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
                        selected = selectedItem == 0,
                        onClick = { selectedItem = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Parqueaderos"
                            )
                        },
                        label = { Text("Actividad", fontWeight = FontWeight.Bold) },
                        selected = selectedItem == 1,
                        onClick = {
                            selectedItem = 1
                            navController.navigate(AppScreens.MyActivity.name)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil", fontWeight = FontWeight.Bold) },
                        selected = selectedItem == 2,
                        onClick = {
                            selectedItem = 2
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
        }
    ) { paddingValues ->
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
                    text = "Buscar\nparqueadero",
                    color = colorResource(R.color.black),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    placeholder = {
                        Text(
                            "¿Dónde te estacionarás hoy?",
                            color = colorResource(R.color.black),
                            fontSize = 18.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "types"
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(50),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.LightGray,
                        unfocusedContainerColor = Color.LightGray,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )


                AnimatedVisibility(visible = isDropdownExpanded && addressSuggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            addressSuggestions.forEachIndexed { index, address ->
                                val placeName = address.featureName ?: "Dirección"
                                val fullAddress = address.getAddressLine(0) ?: ""

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = fullAddress
                                            isDropdownExpanded = false
                                            val location =
                                                LatLng(address.latitude, address.longitude)
                                            SearchMapLocationHolder.searchedLocation = location
                                            navController.navigate(AppScreens.SearchMap.name)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Ubicación",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))


                                    Column {
                                        Text(
                                            text = placeName,
                                            color = Color.Black,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = fullAddress,
                                            color = Color.DarkGray,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (index < addressSuggestions.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(
                                            horizontal = 56.dp,
                                            vertical = 4.dp
                                        ),
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Tus últimos parqueos",
                color = colorResource(R.color.black),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .border(1.5.dp, Color.LightGray, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Transparent)
                    .padding(16.dp)
            ) {
                if (userReservations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes reservas recientes", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(userReservations) { reservation ->
                            val reservationParkingLot =
                                allParkingLots.find { it.id == reservation.parkingId }
                            val savedAddress = reservationParkingLot?.address
                            var displayAddress by remember(reservation.parkingId, savedAddress) {
                                mutableStateOf(
                                    if (!savedAddress.isNullOrBlank()) savedAddress
                                    else "Calculando dirección..."
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color.LightGray.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable {
                                        if (reservationParkingLot != null) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "ubicacionBuscada",
                                                reservationParkingLot.location
                                            )
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "preSelectedParkingId",
                                                reservationParkingLot.id
                                            )
                                            navController.navigate(AppScreens.SearchMap.name)
                                        }
                                    }
                                    .padding(vertical = 16.dp, horizontal = 16.dp)
                            ) {
                                Column {
                                    Text(
                                        reservation.parkingName,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = displayAddress,
                                        color = Color.DarkGray,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { navController.navigate(AppScreens.SearchMap.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.blue),
                    contentColor = colorResource(R.color.white)
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Reservar nuevo parqueadero",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun HomeOperator(navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val parkingLots =
        remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }

    DisposableEffect(uid) {
        val listener = db.collection("parking lots")
            .whereEqualTo("operatorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    parkingLots.value = snapshot.documents.map { doc ->
                        doc.id to (doc.data ?: emptyMap())
                    }
                }
            }

        onDispose {
            listener.remove()
        }
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
                        selected = selectedItem == 0,
                        onClick = { selectedItem = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Parqueaderos"
                            )
                        },
                        label = { Text("Actividad", fontWeight = FontWeight.Bold) },
                        selected = selectedItem == 1,
                        onClick = {
                            selectedItem = 1
                            navController.navigate(AppScreens.MyActivityOperator.name)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil", fontWeight = FontWeight.Bold) },
                        selected = selectedItem == 2,
                        onClick = {
                            selectedItem = 2
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(color = colorResource(R.color.back))
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

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
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .width(2.dp)
                        .background(Color.Black)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Mis\nparqueaderos",
                    color = colorResource(R.color.black),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(2.dp, Color.Gray, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                if (parkingLots.value.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes parqueaderos aún", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(parkingLots.value) { (id, data) ->
                            val rawRating = data["rate"] ?: data["calificacion"]
                            val ratingFloat = (rawRating as? Number)?.toFloat() ?: 0f
                            val ratingString =
                                if (ratingFloat > 0f) String.format("%.1f", ratingFloat) else "0.0"

                            val rawPhotos = data["photos"] ?: data["fotos"] ?: data["imageUrl"] ?: data["imageUrls"]
                            val photoList = when (rawPhotos) {
                                is List<*> -> rawPhotos.filterIsInstance<String>()
                                is String -> if (rawPhotos.isNotBlank()) listOf(rawPhotos) else emptyList()
                                else -> emptyList()
                            }

                            ParkingLotItem(
                                name = data["name"] as? String ?: "Sin nombre",
                                rating = ratingString,
                                parkingId = id,
                                photos = photoList,
                                navController = navController
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate(AppScreens.CreateParking.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Crear nuevo parqueadero",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ParkingLotItem(
    name: String,
    rating: String,
    parkingId: String,
    photos: List<String> = emptyList(),
    navController: NavController
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, fontSize = 20.sp, color = Color.Black)
            TextButton(
                onClick = { navController.navigate("${AppScreens.EditParking.name}/$parkingId") },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "Editar", color = Color(0xFF1877F2), fontSize = 14.sp)
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.LightGray,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "$rating", fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Estrella",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (photos.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(photos) { url ->
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(110.dp)
                            .border(2.dp, Color(0xFF1877F2), RoundedCornerShape(12.dp))
                            .background(Color.LightGray, RoundedCornerShape(12.dp))
                    ) {
                        if (url.isNotBlank()) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Foto parqueadero",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.parqueadero1),
                                contentDescription = "Sin foto",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .border(2.dp, Color(0xFF1877F2), RoundedCornerShape(12.dp))
                            .background(Color.LightGray, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.parqueadero1),
                            contentDescription = "Sin foto",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun HomeUserPreview() {
    HomeUser(navController = rememberNavController())
}

@Composable
@Preview
fun HomeOperatorPreview() {
    HomeOperator(navController = rememberNavController())
}