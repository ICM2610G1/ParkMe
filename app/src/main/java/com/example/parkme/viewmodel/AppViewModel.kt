package com.example.parkme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkme.models.ParkingLot
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _parkingLots = MutableStateFlow<List<ParkingLot>>(emptyList())
    val parkingLots: StateFlow<List<ParkingLot>> = _parkingLots.asStateFlow()

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
    fun rateParkingLot(parkingLotId: String, newRating: Float) {
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("parqueaderos").document(parkingLotId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)

                    if (snapshot.exists()) {
                        val currentRate = snapshot.getDouble("rate")?.toFloat() ?: 0f
                        val currentCount = snapshot.getLong("ratingCount")?.toInt() ?: 0

                        val newCount = currentCount + 1
                        val newAverage = ((currentRate * currentCount) + newRating) / newCount

                        transaction.update(docRef, "rate", newAverage)
                        transaction.update(docRef, "ratingCount", newCount)
                    }
                }.await()

            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    errorMessage = "Error al enviar la calificación: ${e.message}"
                )
            }
        }
    }
    fun fetchParkingLots() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("parqueaderos").get().await()

                val lots = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id

                        val name = doc.getString("name") ?: doc.getString("nombre") ?: "Sin nombre"

                        val lat = (doc.get("latitud") as? Number)?.toDouble() ?: 0.0
                        val lng = (doc.get("longitud") as? Number)?.toDouble() ?: 0.0

                        var pricePerMin = doc.getString("pricePerMin") ?: doc.getString("precioMinuto") ?: "0"
                        var pricePerHour = doc.getString("pricePerHour") ?: doc.getString("precioHora") ?: "0"
                        var fixedPrice = doc.getString("fixedPrice") ?: doc.getString("tarifaFija") ?: "0"

                        if (!pricePerMin.startsWith("$")) pricePerMin = "$$pricePerMin"
                        if (!pricePerHour.startsWith("$")) pricePerHour = "$$pricePerHour"
                        if (!fixedPrice.startsWith("$")) fixedPrice = "$$fixedPrice"

                        val terms = doc.getString("terms") ?: doc.getString("terminos") ?: "Sin términos"
                        val hourStart = doc.getString("hourStart") ?: doc.getString("horaApertura") ?: ""
                        val hourFinish = doc.getString("hourFinish") ?: doc.getString("horaCierre") ?: ""
                        val weekAvailability = doc.getString("weekAvailability") ?: doc.getString("disponibilidad") ?: ""

                        val slot = (doc.get("slot") as? Number)?.toInt() ?: (doc.get("cupos") as? Number)?.toInt() ?: 0

                        val electricCharges = doc.getBoolean("electricCharges") ?: false

                        val rate = (doc.get("rate") as? Number)?.toFloat() ?: (doc.get("calificacion") as? Number)?.toFloat() ?: 0f
                        val ratingCount = (doc.get("ratingCount") as? Number)?.toInt() ?: 0

                        ParkingLot(
                            id = id,
                            name = name,
                            location = LatLng(lat, lng),
                            pricePerMin = pricePerMin,
                            pricePerHour = pricePerHour,
                            fixedPrice = fixedPrice,
                            terms = terms,
                            electricCharges = electricCharges,
                            hourStart = hourStart,
                            hourFinish = hourFinish,
                            weekAvailability = weekAvailability,
                            slot = slot,
                            rate = rate,
                            ratingCount = ratingCount
                        )
                    } catch (e: Exception) {
                        Log.e("MAPS_DEBUG", "Error parseando documento ${doc.id}: ${e.message}")
                        null
                    }
                }

                _parkingLots.value = lots
            } catch (e: Exception) {
                Log.e("MAPS_DEBUG", "Error de conexión a Firebase: ${e.message}")
            }
        }
    }


}