package com.example.parkme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.AppViewModel
import java.util.Locale

@Composable
fun ProfileScreen(navController: NavController, viewModel: AppViewModel) {
    var selectedItem by remember { mutableIntStateOf(2) }

    var showSupportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val authState by viewModel.authState.collectAsState()
    val profileImageUrl = authState.profileImageUrl

    val isOperator = authState.userRole == "Operador"

    val homeRoute = if (isOperator) AppScreens.HomeOperator.name else AppScreens.HomeUser.name
    val activityRoute =
        if (isOperator) AppScreens.MyActivityOperator.name else AppScreens.MyActivity.name
    val chatRoute = if (isOperator) AppScreens.ChatListOp.name else AppScreens.ChatListCli.name
    val roleText = if (isOperator) "Operador" else "Usuario"

    val operatorParkingLots by viewModel.operatorParkingLots.collectAsState()
    LaunchedEffect(isOperator) {
        if (isOperator) {
            viewModel.fetchOperatorActivity()
        }
    }
    val averageRating = remember(operatorParkingLots) {
        var totalScore = 0.0
        var totalVotes = 0
        operatorParkingLots.forEach { lot ->
            totalScore += (lot.rate * lot.ratingCount)
            totalVotes += lot.ratingCount
        }
        if (totalVotes > 0) totalScore / totalVotes else 0.0
    }
    val formattedRating = if (averageRating > 0.0) String.format(Locale.US, "%.1f", averageRating) else "0.0"
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
                NavigationBar(
                    containerColor = Color.Transparent
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio") },
                        selected = selectedItem == 0,
                        onClick = {
                            selectedItem = 0
                            navController.navigate(homeRoute)
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
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Actividad"
                            )
                        },
                        label = { Text("Actividad") },
                        selected = selectedItem == 1,
                        onClick = {
                            selectedItem = 1
                            navController.navigate(activityRoute)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Black,
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Black
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil") },
                        selected = selectedItem == 2,
                        onClick = { selectedItem = 2 },
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
                .fillMaxSize()
                .background(colorResource(R.color.back))
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isOperator && !profileImageUrl.isNullOrEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .padding(top = 30.dp, bottom = 8.dp)
                        .size(160.dp),
                    shape = CircleShape, // Imagen redonda
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Foto de perfil del Operador",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Recorta para llenar el círculo
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "perfil",
                    modifier = Modifier
                        .padding(top = 30.dp, bottom = 8.dp)
                        .size(160.dp)
                        .clip(CircleShape), // Mantiene la consistencia redonda
                    contentScale = ContentScale.Crop
                )
            }
            if (isOperator) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray,
                        contentColor = colorResource(R.color.black)
                    )
                ) {
                    Text("$formattedRating ★ ", fontSize = 17.sp, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }


            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray,
                    contentColor = colorResource(R.color.white)
                )
            ) {
                Text(
                    authState.userName ?: "Cargando...",
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(roleText)

            Spacer(modifier = Modifier.height(4.dp))
            Text(authState.userEmail ?: "Cargando...")

            Spacer(modifier = Modifier.height(24.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mis Servicios",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.SupportAgent,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { showSupportDialog = true }
                            )
                            Text("Soporte", fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Message,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        navController.navigate(chatRoute)
                                    }
                            )
                            Text("Mensajes", fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate(AppScreens.LogIn.name)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.blue),
                    contentColor = colorResource(R.color.white)
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text("Cerrar Sesión", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showSupportDialog) {
            Dialog(onDismissRequest = { showSupportDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { showSupportDialog = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.Gray
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = "Soporte",
                            tint = colorResource(id = R.color.blue),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Para soporte o ayuda, puedes comunicarte a:",
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray,

                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "parkme.company@gmail.com",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.blue),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    // --- NUEVA LÓGICA: Enviar a Gmail ---
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:parkme.company@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Soporte ParkMe - Necesito ayuda")
                                    }
                                    context.startActivity(intent)
                                }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "(Respuesta en 3 días hábiles)",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}