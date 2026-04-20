package com.example.parkme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parkme.navigation.AppScreens
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.unit.dp
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import androidx.compose.ui.res.colorResource
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import com.example.parkme.R




data class ChatMessage(
    val text: String,
    val time: String,
    val enviado: Boolean,
    //val leido: Boolean
)

@Composable
fun ChatCliente() {
    val messages = listOf(
        ChatMessage("Hola, buenos días", "09:00", false),
        ChatMessage("Buenos días", "09:00", true),
        ChatMessage("puedo pasar antes?\nPara poder revisar que todo, gracias", "09:02", true),
        ChatMessage(
            "Claro, el parqueadero tiene el servicio para vehículos electricos y el parqueadero es techado",
            "09:02",
            false
        ),
        ChatMessage("Listo, muchas gracias", "09:02", true)
    )

    Scaffold(
        containerColor = colorResource(R.color.back),
        topBar = { ChatTopBar(x=true) },
        bottomBar = { ChatBottomBar() }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                MessageContentCliente(message)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(x: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
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

        val p = if (x) "Operador" else "Usuario"

        Text(
            text = p,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = { Log.i("Tag:Llamar", "Se solicita acceso a el telefono") }) {
            Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = Color.DarkGray)
        }
        IconButton(onClick = { Log.i("Tag:info", "Se muestra info de el destinatario") }) {
            Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.DarkGray)
        }
    }
}

@Composable
fun ChatBottomBar() {

    var textState by remember { mutableStateOf("Mensaje") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { Log.i("Tag:camara", "Se solicita acceso a la camara") },
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
        )

        Spacer(modifier = Modifier.width(10.dp))
        IconButton(
            onClick = { Log.i("Tag:enviar", "Se envia el mensaje") },
            modifier = Modifier
                .background(Color(0xFF6B8DBE), CircleShape)
                .size(48.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.sendicon),
                contentDescription = "Mi foto de perfil"
            )

        }
    }
}

@Composable
fun MessageContentCliente(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.enviado) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.enviado) {
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
                    color = if (message.enviado) colorResource(R.color.mensajerecibido) else Color.LightGray,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 40.dp)

            )
            Text(
                text = message.time,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }


        if (message.enviado) {
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
@Composable
fun MessageContentOperator(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (!message.enviado) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (message.enviado) {
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
                    color = if (!message.enviado) colorResource(R.color.mensajerecibido) else Color.LightGray,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 40.dp)

            )
            Text(
                text = message.time,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }


        if (!message.enviado) {
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
@Composable
fun ChatOperador(){
    val messages = listOf(
        ChatMessage("Hola, buenos días", "09:00", false),
        ChatMessage("Buenos días", "09:00", true),
        ChatMessage("puedo pasar antes?\nPara poder revisar que todo, gracias", "09:02", true),
        ChatMessage(
            "Claro, el parqueadero tiene el servicio para vehículos electricos y el parqueadero es techado",
            "09:02",
            false
        ),
        ChatMessage("Listo, muchas gracias", "09:02", true)
    )

    Scaffold(
        containerColor = colorResource(R.color.back),
        topBar = { ChatTopBar(x=false) },
        bottomBar = { ChatBottomBar() }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                MessageContentOperator(message)
            }
        }
    }
}

@Preview
@Composable
fun viewchatTopBAR() {
    ChatCliente()
    //ChatOperador()
}
