package com.example.parkme.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkme.R
import com.example.parkme.models.ChatMessage
import com.example.parkme.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    chatId: String,
    miUserId: String,
    esOperador: Boolean,
    chatViewModel: ChatViewModel = viewModel()
) {
    val messages by chatViewModel.messages.collectAsState()

    LaunchedEffect(chatId) {
        chatViewModel.listenForMessages(chatId)
    }

    Scaffold(
        containerColor = colorResource(R.color.back),
        topBar = { ChatTopBar(esOperador = esOperador) },
        bottomBar = {
            ChatBottomBar(
                onSendMessage = { textoDelMensaje ->
                    chatViewModel.sendMessage(chatId, textoDelMensaje, miUserId)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                MessageContent(message = message, miUserId = miUserId)
            }
        }
    }
}

@Composable
fun ChatTopBar(esOperador: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.topbar))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = "Usuario",
            modifier = Modifier
                .size(36.dp)
                .border(2.dp, Color.Gray, CircleShape)
        )
        Spacer(modifier = Modifier.size(14.dp))

        val titulo = if (esOperador) "Operador" else "Usuario"

        Text(
            text = titulo,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = { Log.i("Tag:Llamar", "Llamando") }) {
            Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = Color.DarkGray)
        }
        IconButton(onClick = { Log.i("Tag:info", "Info") }) {
            Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.DarkGray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBottomBar(onSendMessage: (String) -> Unit) {
    var textState by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { /* Acción de cámara */ },
            modifier = Modifier
                .background(Color.Gray, CircleShape)
                .size(50.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Cámara", tint = Color.White)
        }

        Spacer(modifier = Modifier.width(10.dp))

        TextField(
            value = textState,
            onValueChange = { textState = it },
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            placeholder = { Text("Mensaje") },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.width(10.dp))

        IconButton(
            onClick = {
                if (textState.isNotBlank()) {
                    onSendMessage(textState)
                    textState = ""
                }
            },
            modifier = Modifier
                .background(Color(0xFF6B8DBE), CircleShape)
                .size(48.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.sendicon),
                contentDescription = "Enviar"
            )
        }
    }
}

@Composable
fun MessageContent(message: ChatMessage, miUserId: String) {
    val enviadoPorMi = message.isEnviadoPorMi(miUserId)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (enviadoPorMi) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!enviadoPorMi) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "User",
                modifier = Modifier
                    .size(28.dp)
                    .border(1.dp, Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .background(
                    color = if (enviadoPorMi) colorResource(R.color.mensajerecibido) else Color.LightGray,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                )
                Text(
                    text = message.getFormattedTime(),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End),
                    color = Color.DarkGray
                )
            }
        }

        if (enviadoPorMi) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "User",
                modifier = Modifier
                    .size(28.dp)
                    .border(1.dp, Color.Gray, CircleShape)
            )
        }
    }
}