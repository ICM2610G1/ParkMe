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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.AppViewModel

@Composable
fun ProfileScreen(navController: NavController, viewModel: AppViewModel) {
    var itemSeleccionado by remember { mutableIntStateOf(2) }
    val context = LocalContext.current

    val authState by viewModel.authState.collectAsState()

    val isOperator = authState.userRole == "Operador"

    val homeRoute = if (isOperator) AppScreens.HomeOperator.name else AppScreens.HomeUser.name
    val activityRoute = if (isOperator) AppScreens.MyActivityOperator.name else AppScreens.MyActivity.name
    val chatRoute = if (isOperator) AppScreens.ChatOp.name else AppScreens.ChatCli.name
    val roleText = if (isOperator) "Operador" else "Usuario"

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
                        selected = itemSeleccionado == 0,
                        onClick = {
                            itemSeleccionado = 0
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
                        selected = itemSeleccionado == 1,
                        onClick = {
                            itemSeleccionado = 1
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
                        selected = itemSeleccionado == 2,
                        onClick = { itemSeleccionado = 2 },
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
            Image(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "perfil",
                modifier = Modifier
                    .padding(top = 30.dp, bottom = 8.dp)
                    .size(160.dp),
                contentScale = ContentScale.Fit
            )

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray,
                    contentColor = colorResource(R.color.black)
                )
            ) {
                Text("4.9 ★ ", fontSize = 17.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray,
                    contentColor = colorResource(R.color.white)
                )
            ) {
                Text(authState.userName ?: "Cargando...", fontSize = 17.sp, textAlign = TextAlign.Center)
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
                                Icons.Default.Payment,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "Formas de pago\nregistradas",
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.SupportAgent,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
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
                                modifier = Modifier.size(32.dp).clickable {
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
    }
}