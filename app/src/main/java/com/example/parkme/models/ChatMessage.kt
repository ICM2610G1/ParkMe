package com.example.parkme.models


import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val timestamp: Timestamp = Timestamp.now()
) {
    fun isEnviadoPorMi(miUserId: String): Boolean {
        return senderId == miUserId
    }

    fun getFormattedTime(): String {
        val date = timestamp.toDate()
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}