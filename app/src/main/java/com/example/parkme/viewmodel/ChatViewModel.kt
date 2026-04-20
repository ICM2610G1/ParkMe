package com.example.parkme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.parkme.models.ChatMessage
import com.example.parkme.models.ChatRoom
import com.example.parkme.models.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Para los mensajes del chat activo
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Para la lista de chats en la bandeja de entrada
    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    // --- FUNCIONES PARA LA BANDEJA DE CHATS ---

    // Crea la sala si no existe cuando le dan "Iniciar Chat" en MyActivity
    fun iniciarChatRoom(reserva: Reservation) {
        val chatRoom = ChatRoom(
            id = reserva.id,
            parkingName = reserva.parkingName,
            userId = reserva.userId,
            operatorId = reserva.parkingId
        )

        db.collection("chats").document(reserva.id)
            .set(chatRoom, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { Log.i("ChatViewModel", "Sala de chat asegurada") }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al crear sala", e) }
    }

    // Carga la lista de chats para la bandeja de entrada
    fun fetchMyChats(userId: String, isOperador: Boolean) {
        val campoBusqueda = if (isOperador) "operatorId" else "userId"

        db.collection("chats")
            .whereEqualTo(campoBusqueda, userId)
            // .orderBy("timestamp", Query.Direction.DESCENDING) // Opcional si agregas un índice en Firebase
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error al escuchar chat rooms", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val rooms = snapshot.documents.mapNotNull { it.toObject(ChatRoom::class.java) }
                    // Ordenamos localmente por timestamp si no creamos el índice en Firebase
                    _chatRooms.value = rooms.sortedByDescending { it.timestamp }
                }
            }
    }

    // --- FUNCIONES PARA LOS MENSAJES DEL CHAT ---

    fun listenForMessages(chatId: String) {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error al escuchar mensajes", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val newMessages = snapshot.documents.mapNotNull { doc ->
                        val msg = doc.toObject(ChatMessage::class.java)
                        msg?.copy(id = doc.id)
                    }
                    _messages.value = newMessages
                }
            }
    }

    fun sendMessage(chatId: String, text: String, senderId: String) {
        if (text.isBlank()) return

        val newMessage = ChatMessage(
            text = text,
            senderId = senderId
        )

        db.collection("chats").document(chatId).collection("messages")
            .add(newMessage)
            .addOnSuccessListener {
                // Actualizar el "lastMessage" y "timestamp" en el documento principal del chat room
                db.collection("chats").document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to text,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
            }
    }
}