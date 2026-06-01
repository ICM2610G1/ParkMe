package com.example.parkme.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens

@Composable
fun MyBottomNavBar(navController: NavController, initialIndex: Int) {
    var selectedItem by remember { mutableIntStateOf(initialIndex) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color(0xFFF8F9FA),
        contentColor = Color.Black,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            modifier = Modifier.height(76.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            val navigationColors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent,
                selectedIconColor = colorResource(R.color.azulruta),
                selectedTextColor = colorResource(R.color.azulruta),
                unselectedIconColor = colorResource(R.color.grisicon),
                unselectedTextColor = colorResource(R.color.grisicon)
            )

            NavigationBarItem(
                selected = selectedItem == 0,
                onClick = {
                    selectedItem = 0
                    navController.navigate(AppScreens.HomeOperator.name)
                },
                colors = navigationColors,
                label = null,
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Inicio",
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Inicio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )

            NavigationBarItem(
                selected = selectedItem == 1,
                onClick = {
                    selectedItem = 1
                    navController.navigate(AppScreens.MyActivityOperator.name)
                },
                colors = navigationColors,
                label = null,
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timeline,
                            contentDescription = "Parqueaderos",
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Actividad",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )

            NavigationBarItem(
                selected = selectedItem == 2,
                onClick = {
                    selectedItem = 2
                    navController.navigate(AppScreens.OperatorProfile.name)
                },
                colors = navigationColors,
                label = null,
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Perfil",
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Perfil",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    }
}