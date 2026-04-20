package com.example.parkme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val userRole: String? = null,
    val isVerified: Boolean = false,
    val isCheckingSession: Boolean = true,
    val userEmail: String? = null,
    val userName: String? = null
)

class AppViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    init {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val doc = firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val role = doc.getString("role") ?: "Usuario"
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    val name = doc.getString("name") ?: ""
                    val lastName = doc.getString("lastName") ?: ""

                    _authState.value = AuthState(
                        isAuthenticated = true,
                        userRole = role,
                        isVerified = isVerified,
                        isCheckingSession = false,
                        userEmail = currentUser.email,
                        userName = "$name $lastName".trim()
                    )
                } catch (e: Exception) {
                    _authState.value = AuthState(
                        isCheckingSession = false
                    )
                }
            }
        } else {
            _authState.value = AuthState(
                isCheckingSession = false
            )
        }
    }


    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = _authState.value.copy(errorMessage = "Completa todos los campos")
            return
        }
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Usuario nulo")

                // Fuerza refresh del token antes de leer Firestore
                user.getIdToken(true).await()

                val doc = firestore.collection("users").document(user.uid).get().await()
                val role = doc.getString("role") ?: "Usuario"
                val isVerified = doc.getBoolean("isVerified") ?: false
                val name = doc.getString("name") ?: ""
                val lastName = doc.getString("lastName") ?: ""

                _authState.value = AuthState(
                    isAuthenticated = true,
                    userRole = role,
                    isVerified = isVerified,
                    isCheckingSession = false,
                    userEmail = user.email,
                    userName = "$name $lastName".trim()
                )
            } catch (e: Exception) {
                _authState.value = AuthState(
                    errorMessage = mapFirebaseError(e.message),
                    isCheckingSession = false
                )
            }
        }
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        name: String,
        lastName: String,
        phone: String,
        role: String
    ) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _authState.value = _authState.value.copy(errorMessage = "Completa todos los campos")
            return
        }
        if (password != confirmPassword) {
            _authState.value = _authState.value.copy(errorMessage = "Las contraseñas no coinciden")
            return
        }
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Error creando usuario")

                // Fuerza refresh del token antes de escribir en Firestore
                user.getIdToken(true).await()

                val userMap = hashMapOf(
                    "name" to name,
                    "lastName" to lastName,
                    "phone" to phone,
                    "email" to email,
                    "role" to role,
                    "isVerified" to false
                )

                firestore.collection("users").document(user.uid).set(userMap).await()

                _authState.value = AuthState(
                    isAuthenticated = true,
                    userRole = role,
                    isVerified = false,
                    isCheckingSession = false,
                    isLoading = false,
                    userEmail = email,
                    userName = "$name $lastName".trim()
                )
            } catch (e: Exception) {
                _authState.value = AuthState(
                    errorMessage = mapFirebaseError(e.message),
                    isCheckingSession = false,
                    isLoading = false
                )
            }
        }
    }
    fun verifyUser() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("isVerified", true)
                    .await()

                _authState.value = _authState.value.copy(
                    isVerified = true  // ← ¿Está esta línea?
                )
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    errorMessage = "Error verificando usuario"
                )
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState(
            isAuthenticated = false,
            isVerified = false,
            userRole = null,
            isLoading = false,
            errorMessage = null,
            isCheckingSession = false
        )
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }

    private fun mapFirebaseError(message: String?): String {
        return when {
            message == null -> "Error desconocido"
            message.contains("email address is already in use") -> "Este correo ya está registrado"
            message.contains("no user record") -> "No existe cuenta con este correo"
            message.contains("password is invalid") -> "Contraseña incorrecta"
            message.contains("badly formatted") -> "Formato de correo inválido"
            message.contains("network error") -> "Error de conexión"
            else -> "Error: $message"
        }
    }


}