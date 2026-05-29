package com.example.parkme.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.models.ChatMessage
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.ChatViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun ChatScreen(
    chatId: String,
    miUserId: String,
    esOperador: Boolean,
    navController: NavController,
    chatViewModel: ChatViewModel = viewModel()
) {
    val messages by chatViewModel.messages.collectAsState()
    val partnerName by chatViewModel.chatPartnerName.collectAsState()
    val currentChat by chatViewModel.currentChatRoom.collectAsState()

    val context = LocalContext.current
    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var localIsSharing by remember { mutableStateOf(false) }

    LaunchedEffect(currentChat?.sharingLocation) {
        localIsSharing = currentChat?.sharingLocation ?: false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            localIsSharing = isGranted
            chatViewModel.toggleLocationSharing(chatId, isGranted)
        }
    )

    LaunchedEffect(chatId) {
        chatViewModel.listenForMessages(chatId)
        chatViewModel.loadChatPartnerName(chatId, esOperador)
        chatViewModel.listenCurrentChatRoom(chatId)
    }

    DisposableEffect(currentChat?.sharingLocation) {
        var locationCallback: LocationCallback? = null

        if (currentChat?.sharingLocation == true && !esOperador) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(5f).build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        chatViewModel.updateUserLocation(miUserId, loc.latitude, loc.longitude)
                    }
                }
            }

            if (hasLocationPermission) {
                locationProvider.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }

        onDispose {
            locationCallback?.let { locationProvider.removeLocationUpdates(it) }
        }
    }

    val estadoCompartirUI = if (esOperador) {
        currentChat?.sharingLocation == true
    } else {
        localIsSharing
    }

    Scaffold(
        containerColor = colorResource(R.color.back),
        topBar = {
            ChatTopBar(
                nombreDestinatario = partnerName,
                esOperador = esOperador,
                sharingLocation = estadoCompartirUI,
                onToggleShare = { isSharing ->
                    localIsSharing = isSharing

                    if (isSharing && !hasLocationPermission) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        chatViewModel.toggleLocationSharing(chatId, isSharing)
                    }
                },
                onViewLocation = {
                    navController.navigate("${AppScreens.TrackUserMap.name}/$chatId")
                }
            )
        },
        bottomBar = {
            ChatBottomBar(
                onSendMessage = { textoDelMensaje ->
                    chatViewModel.sendMessage(chatId, textoDelMensaje, miUserId)
                },
                onSendImage = { uriImagen ->
                    chatViewModel.sendImageMessage(chatId, uriImagen, miUserId)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageContent(message = message, miUserId = miUserId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    nombreDestinatario: String,
    esOperador: Boolean,
    sharingLocation: Boolean,
    onToggleShare: (Boolean) -> Unit,
    onViewLocation: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorResource(R.color.white)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = colorResource(R.color.blue)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = nombreDestinatario,
                        color = colorResource(R.color.white),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "En línea",
                        color = colorResource(R.color.white).copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        },
        actions = {
            if (!esOperador) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        "Compartir Ubi.",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = sharingLocation,
                        onCheckedChange = { onToggleShare(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.Green
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            } else {
                if (sharingLocation) {
                    Button(
                        onClick = onViewLocation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "Ver ubicación",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorResource(R.color.blue)
        ),
        modifier = Modifier.shadow(4.dp)
    )
}

@Composable
fun ChatBottomBar(onSendMessage: (String) -> Unit, onSendImage: (Uri) -> Unit) {
    var textState by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSendImage(uri)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colorResource(R.color.white),
            modifier = Modifier.weight(1f),
            shadowElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                IconButton(onClick = {
                    galleryLauncher.launch("image/*")
                }) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Galería",
                        tint = colorResource(R.color.gris)
                    )
                }

                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Mensaje...", color = colorResource(R.color.gris))
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colorResource(R.color.blue)
                    ),
                    maxLines = 4
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FloatingActionButton(
            onClick = {
                if (textState.isNotBlank()) {
                    onSendMessage(textState)
                    textState = ""
                }
            },
            containerColor = colorResource(R.color.blue),
            contentColor = colorResource(R.color.white),
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun MessageContent(message: ChatMessage, miUserId: String) {
    val enviadoPorMi = message.isEnviadoPorMi(miUserId)

    val bubbleShape = if (enviadoPorMi) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
    }

    val bubbleColor = if (enviadoPorMi) colorResource(R.color.mensajerecibido) else colorResource(R.color.white)
    val textColor = if (enviadoPorMi) colorResource(R.color.white) else colorResource(R.color.black)
    val timeColor = if (enviadoPorMi) colorResource(R.color.white).copy(alpha = 0.8f) else colorResource(R.color.gris)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (enviadoPorMi) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {

                if (!message.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Imagen enviada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(bottom = if (message.text.isNotBlank()) 6.dp else 2.dp)
                    )
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Text(
                    text = message.getFormattedTime(),
                    fontSize = 10.sp,
                    color = timeColor,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp, end = 4.dp)
                )
            }
        }
    }
}