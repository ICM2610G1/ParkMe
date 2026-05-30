package com.example.parkme.models


data class Reservation(
    val id: String = "",
    val parkingId: String = "",
    val parkingName: String = "",
    val userId: String = "",
    val operatorId: String = "",
    val placa: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "Activa",
    val totalPrice: Double = 0.0,
    val isRated: Boolean = false,
    val sharingLocation: Boolean = false
)