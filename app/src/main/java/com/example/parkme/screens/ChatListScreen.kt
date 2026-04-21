package com.example.parkme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parkme.R
import com.example.parkme.models.ChatRoom
import com.example.parkme.models.ReservationHolder
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    esOperador: Boolean,
    chatViewModel: ChatViewModel = viewModel()
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val chatRooms by chatViewModel.chatRooms.collectAsState()

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            chatViewModel.fetchMyChats(userId = currentUserId, isOperador = esOperador)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Chats") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.topbar)
                )
            )
        }
    ) { paddingValues ->
        if (chatRooms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No tienes chats activos.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colorResource(id = R.color.back))
            ) {
                items(chatRooms) { room ->
                    ChatRoomItem(
                        chatRoom = room,
                        onClick = {
                            ReservationHolder.selectedReservationId = room.id

                            val ruta = if (esOperador) AppScreens.ChatOp.name else AppScreens.ChatCli.name
                            navController.navigate(ruta)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatRoomItem(chatRoom: ChatRoom, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chatRoom.parkingName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = Date(chatRoom.timestamp)
            Text(
                text = sdf.format(date),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = chatRoom.lastMessage,
            fontSize = 14.sp,
            color = Color.DarkGray,
            maxLines = 1,
            modifier = Modifier.padding(end = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color.LightGray, thickness = 0.5.dp)
    }
}