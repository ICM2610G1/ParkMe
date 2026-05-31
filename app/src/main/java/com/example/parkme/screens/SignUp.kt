package com.example.parkme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parkme.R
import com.example.parkme.viewmodel.AppViewModel

@Composable
fun SignUp(navController: NavController, viewModel: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("Usuario") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        if (uri != null) localError = null
    }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    var isRegistering by remember { mutableStateOf(false) }

    val isLoading = authState.isLoading

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && isRegistering) {
            viewModel.saveBiometricCredentials(context, email, password)
            isRegistering = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .background(color = colorResource(R.color.back))
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.logoparkme),
                contentDescription = "Logo de la app",
                modifier = Modifier
                    .height(200.dp)
                    .padding(top = 10.dp, bottom = 5.dp)
                    .width(300.dp),
                contentScale = ContentScale.Fit
            )

            val errorMessage = localError ?: authState.errorMessage
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        item {
            Row(modifier = Modifier.padding(bottom = 24.dp)) {
                Text("Crea tu", color = colorResource(R.color.black), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text(" Cuenta", color = colorResource(R.color.blue), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }

        item {

            SignUpCardSection("Información Personal") {
                SignUpTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombres",
                    placeholder = "Ingrese su nombre/s",
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                SignUpTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Apellidos",
                    placeholder = "Ingrese sus apellidos",
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                SignUpTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Teléfono",
                    placeholder = "+57 3XX-XXX XXXX",
                    keyboardType = KeyboardType.Phone,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                SignUpTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    placeholder = "Dirección de correo electrónico",
                    keyboardType = KeyboardType.Email,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                SignUpPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    placeholder = "Ingrese su contraseña",
                    isVisible = passwordVisible,
                    onVisibilityChange = { passwordVisible = !passwordVisible },
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                SignUpPasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Verificar contraseña",
                    placeholder = "Confirme su contraseña",
                    isVisible = confirmPasswordVisible,
                    onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible },
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SignUpCardSection("Configuración de Cuenta") {
                Text(
                    text = "Selecciona tu Rol:",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedRole == "Usuario",
                            onClick = {
                                if (!isLoading) {
                                    selectedRole = "Usuario"
                                    localError = null
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = colorResource(R.color.blue)),
                            enabled = !isLoading
                        )
                        Text("Usuario", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedRole == "Operador",
                            onClick = { if (!isLoading) selectedRole = "Operador" },
                            colors = RadioButtonDefaults.colors(selectedColor = colorResource(R.color.blue)),
                            enabled = !isLoading
                        )
                        Text("Operador", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = if (selectedRole == "Operador")
                            "La foto de perfil es obligatoria."
                        else
                            "Puedes añadir una foto para personalizar tu perfil.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val buttonText = if (imageUri != null) {
                        "Cambiar Foto"
                    } else if (selectedRole == "Operador") {
                        "Seleccionar Foto de Perfil *"
                    } else {
                        "Seleccionar Foto de Perfil (Opcional)"
                    }

                    if (imageUri != null) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(2.dp, colorResource(R.color.blue), CircleShape)
                                .clickable(enabled = !isLoading) { imagePickerLauncher.launch("image/*") }
                        ) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(colorResource(R.color.blue).copy(alpha = 0.1f))
                                .border(2.dp, colorResource(R.color.blue).copy(alpha = 0.5f), CircleShape)
                                .clickable(enabled = !isLoading) { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Add, contentDescription = "Agregar Foto", tint = colorResource(R.color.blue), modifier = Modifier.size(32.dp))
                                Text("Subir", color = colorResource(R.color.blue), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buttonText,
                        color = if (isLoading) Color.Gray else colorResource(R.color.blue),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(enabled = !isLoading) { imagePickerLauncher.launch("image/*") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.blue),
                    contentColor = colorResource(R.color.white),
                    disabledContainerColor = colorResource(R.color.blue).copy(alpha = 0.6f)
                ),
                onClick = {
                    if (selectedRole == "Operador" && imageUri == null) {
                        localError = "Debes seleccionar una foto de perfil obligatoria para ser Operador."
                    } else {
                        localError = null
                        isRegistering = true
                        viewModel.register(
                            email,
                            password,
                            confirmPassword,
                            name,
                            lastName,
                            phone,
                            selectedRole,
                            imageUri
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(56.dp),
                shape = RoundedCornerShape(30.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Procesando...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "Continuar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SignUpCardSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = Color.LightGray, fontSize = 14.sp) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorResource(R.color.blue),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color(0xFFF9F9F9),
            unfocusedContainerColor = Color(0xFFF9F9F9),
            disabledContainerColor = Color(0xFFF0F0F0),
            disabledBorderColor = Color(0xFFE0E0E0),
            disabledTextColor = Color.DarkGray
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        enabled = enabled
    )
}

@Composable
private fun SignUpPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = Color.LightGray, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorResource(R.color.blue),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color(0xFFF9F9F9),
            unfocusedContainerColor = Color(0xFFF9F9F9),
            disabledContainerColor = Color(0xFFF0F0F0),
            disabledBorderColor = Color(0xFFE0E0E0),
            disabledTextColor = Color.DarkGray
        ),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            Text(
                text = if (isVisible) "Ocultar" else "Mostrar",
                color = if (enabled) colorResource(R.color.blue) else Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable(enabled = enabled) { onVisibilityChange() }
            )
        },
        singleLine = true,
        enabled = enabled
    )
}