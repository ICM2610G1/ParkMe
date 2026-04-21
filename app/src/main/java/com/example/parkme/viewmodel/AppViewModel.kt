package com.example.parkme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkme.models.ParkingLot
import com.example.parkme.models.Reservation
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
    private val _userReservations = MutableStateFlow<List<Reservation>>(emptyList())
    val userReservations: StateFlow<List<Reservation>> = _userReservations.asStateFlow()

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
                    isVerified = true
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
    fun rateParkingLot(parkingLotId: String, reservationId: String, newRating: Float) {
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("parqueaderos").document(parkingLotId)
                val reservationRef = firestore.collection("reservas").document(reservationId)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)

                    if (snapshot.exists()) {
                        val currentRate = snapshot.getDouble("rate")?.toFloat() ?: 0f
                        val currentCount = snapshot.getLong("ratingCount")?.toInt() ?: 0

                        val newCount = currentCount + 1
                        val newAverage = ((currentRate * currentCount) + newRating) / newCount

                        transaction.update(docRef, "rate", newAverage)
                        transaction.update(docRef, "ratingCount", newCount)
                        transaction.update(reservationRef, "isRated", true)
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

        firestore.collection("parqueaderos").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("MAPS_DEBUG", "Error de conexión a Firebase en tiempo real: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val lots = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id

                        val name = doc.getString("name") ?: doc.getString("nombre") ?: "Sin nombre"
                        val operatorId = doc.getString("operatorId") ?: doc.getString("idOperador") ?: ""

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
                        val direccion = doc.getString("direccion") ?: ""
                        val rawPhotos = doc.get("photos") ?: doc.get("fotos") ?: doc.get("imageUrl") ?: doc.get("imageUrls")
                        val photos = when (rawPhotos) {
                            is List<*> -> rawPhotos.filterIsInstance<String>()
                            is String -> if (rawPhotos.isNotBlank()) listOf(rawPhotos) else emptyList()
                            else -> emptyList()
                        }

                        ParkingLot(
                            id = id,
                            operatorId = operatorId,
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
                            ratingCount = ratingCount,
                            direccion = direccion,
                            photos = photos
                        )
                    } catch (e: Exception) {
                        Log.e("MAPS_DEBUG", "Error parseando documento ${doc.id}: ${e.message}")
                        null
                    }
                }


                _parkingLots.value = lots
            }
        }
    }
    fun fetchUserReservations() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("RESERVAS_DEBUG", "Error: No hay usuario iniciado.")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("RESERVAS_DEBUG", "Buscando reservas para el usuario: $uid")

                val result = firestore.collection("reservas").whereEqualTo("userId", uid).get().await()
                Log.d("RESERVAS_DEBUG", "Documentos encontrados en Firebase: ${result.documents.size}")

                val reservas = result.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id

                        val parkingId = doc.getString("parkingId") ?: doc.getString("idParqueadero") ?: ""
                        val parkingName = doc.getString("parkingName") ?: doc.getString("nombreParqueadero") ?: "Parqueadero"
                        val userId = doc.getString("userId") ?: ""
                        val placa = doc.getString("placa") ?: ""
                        val operatorId = doc.getString("operatorId") ?: doc.getString("idOperador") ?: ""
                        val startTime = doc.getString("startTime") ?: doc.getString("horaInicio") ?: ""
                        val endTime = doc.getString("endTime") ?: doc.getString("horaFin") ?: ""
                        val status = doc.getString("status") ?: doc.getString("estado") ?: "Activa"

                        val totalPrice = (doc.get("totalPrice") as? Number)?.toDouble()
                            ?: (doc.get("precioTotal") as? Number)?.toDouble() ?: 0.0
                        val isRated = doc.getBoolean("isRated") ?: false

                        Log.d("RESERVAS_DEBUG", "Reserva leída correctamente: $parkingName - $placa")

                        Reservation(
                            id = id,
                            parkingId = parkingId,
                            operatorId = operatorId,
                            parkingName = parkingName,
                            userId = userId,
                            placa = placa,
                            startTime = startTime,
                            endTime = endTime,
                            status = status,
                            totalPrice = totalPrice,
                            isRated = isRated
                        )
                    } catch (e: Exception) {
                        Log.e("RESERVAS_DEBUG", "Error armando la reserva ${doc.id}: ${e.message}")
                        null
                    }
                }

                Log.d("RESERVAS_DEBUG", "Total de reservas válidas a mostrar en la lista: ${reservas.size}")
                _userReservations.value = reservas

            } catch (e: Exception) {
                Log.e("RESERVAS_DEBUG", "Error conectando con Firebase para las reservas: ${e.message}")
            }
        }
    }

    private val _operatorParkingLots = MutableStateFlow<List<ParkingLot>>(emptyList())
    val operatorParkingLots: StateFlow<List<ParkingLot>> = _operatorParkingLots.asStateFlow()

    private val _operatorReservations = MutableStateFlow<List<Reservation>>(emptyList())
    val operatorReservations: StateFlow<List<Reservation>> = _operatorReservations.asStateFlow()

    fun fetchOperatorActivity() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("parqueaderos")
            .whereEqualTo("operatorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    val lots = snapshot.documents.mapNotNull { doc ->
                        try {
                            ParkingLot(
                                id = doc.id,
                                operatorId = doc.getString("operatorId") ?: "",
                                name = doc.getString("name") ?: doc.getString("nombre") ?: "Sin nombre",
                                slot = (doc.get("slot") as? Number)?.toInt() ?: 0
                            )
                        } catch (e: Exception) { null }
                    }
                    _operatorParkingLots.value = lots
                }
            }

        firestore.collection("reservas")
            .whereEqualTo("operatorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    val res = snapshot.documents.mapNotNull { doc ->
                        try {
                            Reservation(
                                id = doc.id,
                                parkingId = doc.getString("parkingId") ?: "",
                                operatorId = doc.getString("operatorId") ?: "",
                                parkingName = doc.getString("parkingName") ?: "",
                                userId = doc.getString("userId") ?: "",
                                placa = doc.getString("placa") ?: "",
                                startTime = doc.getString("startTime") ?: "",
                                endTime = doc.getString("endTime") ?: "",
                                status = doc.getString("status") ?: "Activa",
                                totalPrice = (doc.get("totalPrice") as? Number)?.toDouble() ?: 0.0,
                                isRated = doc.getBoolean("isRated") ?: false
                            )
                        } catch (e: Exception) { null }
                    }
                    _operatorReservations.value = res.sortedByDescending { it.startTime }
                }
            }
    }

}