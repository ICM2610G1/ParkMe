package com.example.parkme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.models.ParkingLot

@Composable
fun ParkingLotDetail(navController: NavController, parking: ParkingLot) {
    Scaffold(
        modifier = Modifier.background(color = colorResource(R.color.back)),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue))
                ) {
                    Text(
                        "Reservar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                    modifier = Modifier.width(160.dp).height(95.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.height(70.dp).width(2.dp).background(Color.Black))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = parking.name,
                    color = colorResource(R.color.black),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(50),
                color = colorResource(R.color.grisprecios)
            ) {
                Text(
                    " ${parking.pricePerHour} / hora   •    ${parking.pricePerMin} / min   •   Tarifa plena:  ${parking.fixedPrice}",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(50),
                color = colorResource(R.color.grisprecios)
            ) {
                Text(
                    "Horario: ${parking.hourStart} - ${parking.hourFinish}   •   Días: ${parking.weekAvailability}",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    "Disponible",
                    fontSize = 17.sp,
                    color = colorResource(R.color.black),
                    fontWeight = FontWeight.Medium
                )
                Box(
                    Modifier
                        .size(24.dp)
                        .border(1.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (parking.slot > 0) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    }else{
                        Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))

                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    "Cargador EV",
                    fontSize = 17.sp,
                    color = colorResource(R.color.black),
                    fontWeight = FontWeight.Medium
                )
                Box(
                    Modifier
                        .size(24.dp)
                        .border(1.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (parking.electricCharges) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    "Reglas del parqueadero",
                    fontSize = 17.sp,
                    color = colorResource(R.color.black),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Ver más",
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                    color = colorResource(R.color.blue)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                shape = RoundedCornerShape(16.dp),
                color = colorResource(R.color.grisClaro)
            ) {
                Text(
                    text = parking.terms.ifEmpty { "Sin reglas definidas" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.grisClaro))
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text("Fotos del parqueadero", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        TextButton(onClick = {}, contentPadding = PaddingValues(0.dp)) {
                            Text(
                                text = "Ver todo",
                                color = colorResource(R.color.blue),
                                fontSize = 11.sp,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (parking.photos.isEmpty()) {
                            repeat(3) {
                                Image(
                                    painter = painterResource(id = R.drawable.parqueadero1),
                                    contentDescription = "Sin foto",
                                    modifier = Modifier.size(85.dp).clip(RoundedCornerShape(15.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            parking.photos.take(3).forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Foto parqueadero",
                                    modifier = Modifier.size(85.dp).clip(RoundedCornerShape(15.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}