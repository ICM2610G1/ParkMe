package com.example.parkme.models

import com.google.android.gms.maps.model.LatLng

data class ParkingLot(
    val id: String = "",
    val name: String = "",
    val location: LatLng = LatLng(0.0, 0.0),
    val pricePerMin: String = "",
    val pricePerHour: String = "",
    val fixedPrice: String = "",
    val terms: String = "",
    val electricCharges: Boolean = false,
    val hourStart: String = "",
    val hourFinish: String = "",
    val weekAvailability: String = "",
    val slot: Int = 0,
    val photos: List<String> = emptyList(),
    val rate: Float = 0f,
    val ratingCount: Int = 0,
    val direccion: String = ""
)