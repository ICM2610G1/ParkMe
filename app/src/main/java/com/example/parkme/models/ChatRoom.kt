package com.example.parkme.models

data class ChatRoom(
    val id: String = "",
    val parkingName: String = "",
    val userId: String = "",
    val operatorId: String = "",
    val lastMessage: String = "Chat iniciado",
    val timestamp: Long = System.currentTimeMillis()
)