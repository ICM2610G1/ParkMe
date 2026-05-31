package com.example.parkme.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parkme.MainActivity
import com.example.parkme.R
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.AppViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogIn(navController: NavController, viewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var sheetEmail by remember { mutableStateOf("") }
    var sheetPassword by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var sheetPasswordVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val activity = context as? MainActivity

    var linkedEmail by remember { mutableStateOf(viewModel.getSavedBiometricEmail(context)) }
    var linkedPassword by remember { mutableStateOf(viewModel.getSavedBiometricPass(context)) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .background(color = colorResource(R.color.back))
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier
                    .height(280.dp)
                    .width(400.dp),
                contentScale = ContentScale.Fit
            )

            Row {
                Text("Encuentra tu", color = colorResource(R.color.black), fontWeight = FontWeight.Bold)
                Text(" parqueadero", color = colorResource(R.color.blue), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (authState.errorMessage != null && !showSheet) {
                Text(
                    text = authState.errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            TextField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                placeholder = { Text("Dirección de correo electrónico", color = colorResource(R.color.grisB), fontSize = 14.sp) },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.LightGray,
                    unfocusedContainerColor = Color.LightGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contraseña", color = colorResource(R.color.grisB), fontSize = 14.sp) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "Ocultar" else "Mostrar",
                        color = colorResource(R.color.black),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { passwordVisible = !passwordVisible }
                    )
                },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.LightGray,
                    unfocusedContainerColor = Color.LightGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth(0.5f),
                enabled = !authState.isLoading && !showSheet,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.blue),
                    contentColor = colorResource(R.color.white)
                )
            ) {
                if (authState.isLoading && !showSheet) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                }
            }

            if (linkedEmail.isNotEmpty()) {
                val isMatch = email.isEmpty() || email.equals(linkedEmail, ignoreCase = true)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        activity?.authenticate(
                            onSuccess = { viewModel.login(linkedEmail, linkedPassword) },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    enabled = isMatch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMatch) Color.DarkGray else Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Ingresar con Biometría")
                }

                if (!isMatch) {
                    Text(
                        text = "El correo no coincide con la cuenta biométrica.",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("¿No tienes cuenta?")
            Button(
                onClick = { navController.navigate(AppScreens.SignUp.name) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.transparent),
                    contentColor = colorResource(R.color.blue)
                )
            ) {
                Text("Regístrate", fontWeight = FontWeight.Bold)
            }
        }
        TextButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            val buttonText = if (linkedEmail.isNotEmpty()) "Cambiar cuenta principal" else "Configurar Biometría"
            Text(buttonText, color = colorResource(R.color.blue), fontWeight = FontWeight.Bold)
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showSheet = false
                    viewModel.clearError()
                },
                sheetState = sheetState,
                containerColor = colorResource(R.color.back)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Configuración Biométrica",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.blue)
                    )
                    Text(
                        "Ingresa las credenciales del perfil principal que podrá usar biometría en este dispositivo.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    if (authState.errorMessage != null && showSheet) {
                        Text(
                            text = authState.errorMessage!!,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    TextField(
                        value = sheetEmail,
                        onValueChange = { sheetEmail = it; viewModel.clearError() },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = { Text("Email Principal", color = colorResource(R.color.grisB)) },
                        shape = RoundedCornerShape(50),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.LightGray.copy(alpha = 0.5f),
                            unfocusedContainerColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = sheetPassword,
                        onValueChange = { sheetPassword = it; viewModel.clearError() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contraseña", color = colorResource(R.color.grisB)) },
                        visualTransformation = if (sheetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            Text(
                                text = if (sheetPasswordVisible) "Ocultar" else "Mostrar",
                                color = colorResource(R.color.black),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable { sheetPasswordVisible = !sheetPasswordVisible }
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.LightGray.copy(alpha = 0.5f),
                            unfocusedContainerColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.verifyAndSaveBiometric(context, sheetEmail, sheetPassword) {
                                linkedEmail = sheetEmail
                                linkedPassword = sheetPassword
                                showSheet = false
                                sheetEmail = ""
                                sheetPassword = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !authState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.blue))
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Vincular y Activar")
                        }
                    }
                }
            }
        }
    }
}