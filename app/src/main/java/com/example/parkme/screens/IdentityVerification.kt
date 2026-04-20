package com.example.parkme.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parkme.R
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.parkme.navigation.AppScreens
import com.example.parkme.viewmodel.AppViewModel

@Composable
fun IdentityVerification(navController: NavController, viewModel: AppViewModel) {
    val scrollState = rememberScrollState()
    var selectedDocument by remember { mutableStateOf<String?>(null) }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var photoTaken by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            photoTaken = true
        }
    }

    Column(
        modifier = Modifier
            .background(color = colorResource(R.color.back))
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Image(
            painter = painterResource(id = R.drawable.logoparkme),
            contentDescription = "Logo de la app",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(top = 10.dp, bottom = 5.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Verificación de\nIdentidad",
            color = colorResource(R.color.black),
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ten listo un documento de identidad para que podamos tomarle una foto y así verificar tu identidad.",
            color = colorResource(R.color.black),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tu información se mantendrá privada.",
            color = colorResource(R.color.black),
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Opción Cédula
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedDocument = "Cedula" }
                .padding(vertical = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                RadioButton(
                    selected = selectedDocument == "Cedula",
                    onClick = {
                        selectedDocument = "Cedula"
                        launcher.launch(null)
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = colorResource(R.color.blue),
                        unselectedColor = Color.Transparent
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray, RoundedCornerShape(50))
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Text(
                    text = "Cédula de Ciudadanía",
                    color = colorResource(R.color.black),
                    fontSize = 16.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedDocument = "Pasaporte" }
                .padding(vertical = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                RadioButton(
                    selected = selectedDocument == "Pasaporte",
                    onClick = {
                        selectedDocument = "Pasaporte"
                        launcher.launch(null)
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = colorResource(R.color.blue),
                        unselectedColor = Color.Transparent
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray, RoundedCornerShape(50))
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Text(
                    text = "Pasaporte",
                    color = colorResource(R.color.black),
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    if (activity != null) {
                        authenticateWithBiometrics(activity) {
                            viewModel.verifyUser()
                            if (authState.userRole == "Operador") {
                                navController.navigate(AppScreens.HomeOperator.name) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                navController.navigate(AppScreens.HomeUser.name) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    } else {
                        viewModel.verifyUser()
                        navController.navigate(AppScreens.HomeUser.name) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                enabled = photoTaken,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.blue),
                    contentColor = colorResource(R.color.white),
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.LightGray
                )
            ) {
                Text(
                    if (photoTaken) "Verificar identidad" else "Toma una foto primero",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


fun authenticateWithBiometrics(
    activity: FragmentActivity,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)

    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(activity, "Error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(activity, "Huella no reconocida", Toast.LENGTH_SHORT).show()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Verificación de Seguridad")
        .setSubtitle("Usa tu huella digital para confirmar tu identidad")
        .setNegativeButtonText("Cancelar")
        .build()

    biometricPrompt.authenticate(promptInfo)
}