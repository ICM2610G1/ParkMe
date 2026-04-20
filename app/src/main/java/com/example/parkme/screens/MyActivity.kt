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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens

@Composable
fun MyActivity(navController: NavController) {
    var itemSeleccionado by remember { mutableIntStateOf(1) }

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
                        onClick = { itemSeleccionado = 2
                            navController.navigate(AppScreens.UserProfile.name)},
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(2.dp, Color.Gray, RoundedCornerShape(24.dp))
                            .background(Color.Transparent).padding(bottom = 16.dp)
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.mapa),
                                contentDescription = "Mapa del parqueo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Parqueadero La 43", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                                    Text("$$$ por hora", fontSize = 16.sp, color = Color.DarkGray)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .clickable { navController.navigate(AppScreens.RateParkingLot.name) }
                                    ) {
                                        Icon(Icons.Outlined.Star, contentDescription = "Calificar", tint = Color.Black, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Califica", fontSize = 16.sp, color = Color.Black)
                                    }
                                }

                                Button(
                                    onClick = {  },
                                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("Reservar de\nnuevo", textAlign = TextAlign.Center, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item { HistoryItemRow("Parqueadero Sophieparking", "Feb 17 2026 - 2:05 p.m.", "$$$") }
                item { HistoryItemRow("Parqueadero Sophieparking", "Feb 11 2026 - 1:30 p.m.", "$$$") }
                item { HistoryItemRow("Parqueadero Be Parking", "Feb 2 2026 - 9:07 a.m.", "$$$") }
            }
        }
    }
}

@Composable
fun HistoryItemRow(name: String, date: String, price: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(text = date, fontSize = 14.sp, color = Color.DarkGray)
                Text(text = "$price por hora", fontSize = 14.sp, color = Color.DarkGray)
            }

            Button(
                onClick = {  },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Reservar\nde nuevo", textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 14.sp)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Color.Gray)
    }
}

@Composable
fun MyActivityOperator(navController: NavController) {
    var itemSeleccionado by remember { mutableIntStateOf(1) }

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
                Spacer(modifier = Modifier.width(12.dp))
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color.Gray, RoundedCornerShape(24.dp))
                            .background(Color.Transparent, RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Parqueadero A",
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
                                Text("Cupos disponibles:", fontSize = 15.sp, color = Color.Black)
                                Text(
                                    text = "12/20",
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(R.color.blue),
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { 12f / 20f },
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
                                Text("Ganancias del día:", fontSize = 15.sp, color = Color.Black)
                                Text(
                                    text = "$ 130.000",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Green,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Servicios recientes", fontSize = 14.sp, color = Color.DarkGray)

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, Color.Gray, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "Toyota Corolla AAA-123", color = Color.Black, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Ingreso desde las 9:00 a.m", color = Color.Yellow, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row {
                                        Text(text = "Costo acumulado: ", color = Color.Black, fontSize = 14.sp)
                                        Text(text = "$ 22.000", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, Color.Gray, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "Chevrolet Captiva BBB-123", color = Color.Black, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "7:30 a.m. - 9:00 a.m.", color = colorResource(R.color.blue), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row {
                                        Text(text = "Pagó: ", color = Color.Black, fontSize = 14.sp)
                                        Text(text = "$ 14.000", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
fun Activitypreview(){
    val navController = rememberNavController()
    MyActivity(navController = navController)
}