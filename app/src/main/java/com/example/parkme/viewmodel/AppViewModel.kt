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
import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
                    val doc = firestore.collection("users").document(currentUser.uid).get().await()
                    val role = doc.getString("role") ?: "Usuario"
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    val name = doc.getString("name") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    saveDeviceToken(currentUser.uid)
                    _authState.value = AuthState(
                        isAuthenticated = true,
                        userRole = role,
                        isVerified = isVerified,
                        isCheckingSession = false,
                        userEmail = currentUser.email,
                        userName = "$name $lastName".trim()
                    )
                } catch (e: Exception) {
                    _authState.value = AuthState(isCheckingSession = false)
                }
            }
        } else {
            _authState.value = AuthState(isCheckingSession = false)
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
                saveDeviceToken(user.uid)
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
                _authState.value = AuthState(errorMessage = mapFirebaseError(e.message), isCheckingSession = false)
            }
        }
    }

    fun verifyAndSaveBiometric(context: Context, email: String, pass: String, onSuccess: () -> Unit) {
        _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                saveBiometricCredentials(context, email, pass)
                auth.signOut()
                _authState.value = _authState.value.copy(isLoading = false, errorMessage = null)
                onSuccess()
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(isLoading = false, errorMessage = "Credenciales incorrectas o error de conexión")
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String, name: String, lastName: String, phone: String, role: String) {
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
                val userMap = hashMapOf("name" to name, "lastName" to lastName, "phone" to phone, "email" to email, "role" to role, "isVerified" to false)
                firestore.collection("users").document(user.uid).set(userMap).await()
                saveDeviceToken(user.uid)
                _authState.value = AuthState(isAuthenticated = true, userRole = role, isVerified = false, isCheckingSession = false, isLoading = false, userEmail = email, userName = "$name $lastName".trim())
            } catch (e: Exception) {
                _authState.value = AuthState(errorMessage = mapFirebaseError(e.message), isCheckingSession = false, isLoading = false)
            }
        }
    }

    fun verifyUser() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid).update("isVerified", true).await()
                _authState.value = _authState.value.copy(isVerified = true)
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(errorMessage = "Error verificando usuario")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState(isAuthenticated = false, isVerified = false, userRole = null, isLoading = false, errorMessage = null, isCheckingSession = false)
    }

    fun clearError() { _authState.value = _authState.value.copy(errorMessage = null) }

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
                _authState.value = _authState.value.copy(errorMessage = "Error al enviar la calificación: ${e.message}")
            }
        }
    }

    fun fetchParkingLots() {
        firestore.collection("parqueaderos").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val lots = snapshot.documents.mapNotNull { doc ->
                    try {
                        val name = doc.getString("name") ?: doc.getString("nombre") ?: "Sin nombre"
                        val operatorId = doc.getString("operatorId") ?: doc.getString("idOperador") ?: ""

                        // Lectura con fallback a español
                        val lat = (doc.get("latitude") as? Number)?.toDouble() ?: (doc.get("latitud") as? Number)?.toDouble() ?: 0.0
                        val lng = (doc.get("longitude") as? Number)?.toDouble() ?: (doc.get("longitud") as? Number)?.toDouble() ?: 0.0

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
                        val address = doc.getString("address") ?: doc.getString("direccion") ?: ""

                        val rawPhotos = doc.get("photos") ?: doc.get("fotos") ?: doc.get("imageUrl")
                        val photos = when (rawPhotos) {
                            is List<*> -> rawPhotos.filterIsInstance<String>()
                            is String -> if (rawPhotos.isNotBlank()) listOf(rawPhotos) else emptyList()
                            else -> emptyList()
                        }

                        ParkingLot(id = doc.id, operatorId = operatorId, name = name, location = LatLng(lat, lng), pricePerMin = pricePerMin, pricePerHour = pricePerHour, fixedPrice = fixedPrice, terms = terms, electricCharges = electricCharges, hourStart = hourStart, hourFinish = hourFinish, weekAvailability = weekAvailability, slot = slot, rate = rate, ratingCount = ratingCount, address = address, photos = photos)
                    } catch (e: Exception) { null }
                }
                _parkingLots.value = lots
            }
        }
    }

    fun fetchUserReservations() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("reservas").whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val reservas = snapshot.documents.mapNotNull { doc ->
                        try {
                            Reservation(
                                id = doc.id,
                                parkingId = doc.getString("parkingId") ?: doc.getString("idParqueadero") ?: "",
                                operatorId = doc.getString("operatorId") ?: doc.getString("idOperador") ?: "",
                                parkingName = doc.getString("parkingName") ?: doc.getString("nombreParqueadero") ?: "Parqueadero",
                                userId = doc.getString("userId") ?: "",
                                licensePlate = doc.getString("licensePlate") ?: "",
                                startTime = doc.getString("startTime") ?: doc.getString("horaInicio") ?: "",
                                endTime = doc.getString("endTime") ?: doc.getString("horaFin") ?: "",
                                status = doc.getString("status") ?: doc.getString("estado") ?: "Activa",
                                totalPrice = (doc.get("totalPrice") as? Number)?.toDouble() ?: (doc.get("precioTotal") as? Number)?.toDouble() ?: 0.0,
                                isRated = doc.getBoolean("isRated") ?: false,
                                sharingLocation = doc.getBoolean("sharingLocation") ?: false
                            )
                        } catch (e: Exception) { null }
                    }
                    checkAndUpdateExpiredReservations(reservas)
                    _userReservations.value = reservas
                }
            }
    }

    private val _operatorParkingLots = MutableStateFlow<List<ParkingLot>>(emptyList())
    val operatorParkingLots: StateFlow<List<ParkingLot>> = _operatorParkingLots.asStateFlow()
    private val _operatorReservations = MutableStateFlow<List<Reservation>>(emptyList())
    val operatorReservations: StateFlow<List<Reservation>> = _operatorReservations.asStateFlow()

    fun fetchOperatorActivity() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("parqueaderos").whereEqualTo("operatorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    _operatorParkingLots.value = snapshot.documents.mapNotNull { doc ->
                        try {
                            ParkingLot(id = doc.id, operatorId = doc.getString("operatorId") ?: "", name = doc.getString("name") ?: doc.getString("nombre") ?: "Sin nombre", slot = (doc.get("slot") as? Number)?.toInt() ?: 0)
                        } catch (e: Exception) { null }
                    }
                }
            }
        firestore.collection("reservas").whereEqualTo("operatorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val res = snapshot.documents.mapNotNull { doc ->
                        try {
                            Reservation(
                                id = doc.id,
                                parkingId = doc.getString("parkingId") ?: "",
                                operatorId = doc.getString("operatorId") ?: "",
                                parkingName = doc.getString("parkingName") ?: "",
                                userId = doc.getString("userId") ?: "",
                                licensePlate = doc.getString("licensePlate") ?: "",
                                startTime = doc.getString("startTime") ?: "",
                                endTime = doc.getString("endTime") ?: "",
                                status = doc.getString("status") ?: "Activa",
                                totalPrice = (doc.get("totalPrice") as? Number)?.toDouble() ?: 0.0,
                                isRated = doc.getBoolean("isRated") ?: false,
                                sharingLocation = doc.getBoolean("sharingLocation") ?: false
                            )
                        } catch (e: Exception) { null }
                    }
                    checkAndUpdateExpiredReservations(res)
                    _operatorReservations.value = res.sortedByDescending { it.startTime }
                }
            }
    }

    fun getSavedBiometricEmail(context: Context): String = context.getSharedPreferences("ParkMePrefs", Context.MODE_PRIVATE).getString("savedEmail", "") ?: ""
    fun getSavedBiometricPass(context: Context): String = context.getSharedPreferences("ParkMePrefs", Context.MODE_PRIVATE).getString("savedPass", "") ?: ""
    fun saveBiometricCredentials(context: Context, email: String, pass: String) = context.getSharedPreferences("ParkMePrefs", Context.MODE_PRIVATE).edit().putString("savedEmail", email).putString("savedPass", pass).apply()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startTrackingUserLocation(context: Context, userId: String) {
        if (fusedLocationClient == null) fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        if (locationCallback != null) return
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    firestore.collection("users").document(userId).update(mapOf("latitude" to loc.latitude, "longitude" to loc.longitude))
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build(), locationCallback!!, Looper.getMainLooper())
    }

    fun stopTrackingUserLocation() {
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun saveDeviceToken(uid: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                firestore.collection("users").document(uid).update("fcmToken", token).await()
            } catch (e: Exception) { Log.e("FCM", "Error FCM", e) }
        }
    }

    private fun checkAndUpdateExpiredReservations(reservations: List<Reservation>) {
        val dateFormatFull = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentTime = Date()
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentTime)
        reservations.forEach { reserva ->
            if (reserva.status == "Activa" || reserva.status == "Activo") {
                try {
                    val end = if (reserva.endTime.length > 5) dateFormatFull.parse(reserva.endTime) else dateFormatFull.parse("$hoy ${reserva.endTime}")
                    if (end != null && currentTime.after(end)) {
                        firestore.collection("reservas").document(reserva.id).update(mapOf("status" to "Finalizada", "sharingLocation" to false)).addOnSuccessListener {
                            if (auth.currentUser?.uid == reserva.userId) stopTrackingUserLocation()
                        }
                    }
                } catch (e: Exception) { Log.e("RESERVAS", "Error fecha ${reserva.id}: ${e.message}") }
            }
        }
    }
}