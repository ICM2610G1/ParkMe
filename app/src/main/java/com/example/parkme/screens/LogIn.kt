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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.navigation.NavController
import com.example.parkme.MainActivity
import com.example.parkme.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.AppViewModel



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

    val pillShape = RoundedCornerShape(50)
    val modernTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colorResource(R.color.blue),
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedContainerColor = Color(0xFFF9F9F9),
        unfocusedContainerColor = Color(0xFFF9F9F9),
        disabledContainerColor = Color(0xFFEFEFEF),
        disabledBorderColor = Color.Transparent,
        disabledTextColor = Color.Gray,
        disabledPlaceholderColor = Color.Gray
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .background(color = colorResource(R.color.back))
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier
                    .height(180.dp)
                    .width(280.dp),
                contentScale = ContentScale.Fit
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Encuentra tu", color = colorResource(R.color.black), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(" parqueadero", color = colorResource(R.color.blue), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (authState.errorMessage != null && !showSheet) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = authState.errorMessage!!,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !authState.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                placeholder = { Text("Dirección de correo electrónico", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.Gray) },
                shape = pillShape,
                colors = modernTextFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !authState.isLoading,
                placeholder = { Text("Contraseña", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Contraseña", tint = Color.Gray) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "Ocultar" else "Mostrar",
                        color = if (authState.isLoading) Color.Gray else colorResource(R.color.black),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable(enabled = !authState.isLoading) { passwordVisible = !passwordVisible }
                    )
                },
                shape = pillShape,
                colors = modernTextFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !authState.isLoading && !showSheet,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.blue),
                    contentColor = colorResource(R.color.white),
                    disabledContainerColor = colorResource(R.color.blue).copy(alpha = 0.5f)
                ),
                shape = pillShape
            ) {
                if (authState.isLoading && !showSheet) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                } else {
                    Text("Iniciar Sesión", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isMatch && !authState.isLoading,
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMatch) Color.DarkGray else Color.LightGray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Ingresar con Biometría", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                if (!isMatch) {
                    Text(
                        text = "El correo no coincide con la cuenta biométrica.",
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tienes cuenta?", color = Color.Gray, fontSize = 15.sp)
                TextButton(
                    onClick = { navController.navigate(AppScreens.SignUp.name) },
                    enabled = !authState.isLoading
                ) {
                    Text("Regístrate", color = colorResource(R.color.blue), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        TextButton(
            onClick = { showSheet = true },
            enabled = !authState.isLoading,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            val buttonText = if (linkedEmail.isNotEmpty()) "Cambiar cuenta principal" else "Configurar Biometría"
            Text(buttonText, color = colorResource(R.color.blue), fontWeight = FontWeight.Bold)
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (!authState.isLoading) {
                        showSheet = false
                        viewModel.clearError()
                    }
                },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
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
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = authState.errorMessage!!,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = sheetEmail,
                        onValueChange = { sheetEmail = it; viewModel.clearError() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !authState.isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        placeholder = { Text("Email Principal", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.Gray) },
                        shape = pillShape,
                        colors = modernTextFieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sheetPassword,
                        onValueChange = { sheetPassword = it; viewModel.clearError() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !authState.isLoading,
                        placeholder = { Text("Contraseña", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Contraseña", tint = Color.Gray) },
                        visualTransformation = if (sheetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            Text(
                                text = if (sheetPasswordVisible) "Ocultar" else "Mostrar",
                                color = if (authState.isLoading) Color.Gray else colorResource(R.color.black),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable(enabled = !authState.isLoading) { sheetPasswordVisible = !sheetPasswordVisible }
                            )
                        },
                        shape = pillShape,
                        colors = modernTextFieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !authState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.blue),
                            disabledContainerColor = colorResource(R.color.blue).copy(alpha = 0.5f)
                        ),
                        shape = pillShape
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                        } else {
                            Text("Vincular y Activar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}