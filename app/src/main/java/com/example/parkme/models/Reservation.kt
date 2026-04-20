package com.example.parkme.models


data class Reservation(
    val id: String = "",
    val parkingId: String = "",
    val parkingName: String = "",
    val userId: String = "",
    val placa: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "Activa",
    val totalPrice: Double = 0.0
)