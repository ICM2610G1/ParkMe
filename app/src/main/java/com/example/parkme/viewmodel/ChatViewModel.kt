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
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()


    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    fun iniciarChatRoom(reserva: Reservation) {
        val chatRoom = ChatRoom(
            id = reserva.id,
            parkingName = reserva.parkingName,
            userId = reserva.userId,
            operatorId = reserva.operatorId
        )

        db.collection("chats").document(reserva.id)
            .set(chatRoom, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { Log.i("ChatViewModel", "Sala de chat asegurada") }
            .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al crear sala", e) }
    }


    fun fetchMyChats(userId: String, isOperador: Boolean) {
        val campoBusqueda = if (isOperador) "operatorId" else "userId"

        db.collection("chats")
            .whereEqualTo(campoBusqueda, userId)

            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error al escuchar chat rooms", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val rooms = snapshot.documents.mapNotNull { it.toObject(ChatRoom::class.java) }
                    _chatRooms.value = rooms.sortedByDescending { it.timestamp }
                }
            }
    }


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
                db.collection("chats").document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to text,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
            }
    }

    fun sendImageMessage(chatId: String, imageUri: Uri, senderId: String) {
        val storageRef = FirebaseStorage.getInstance().reference

        val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val newMessage = ChatMessage(
                        text = "",
                        imageUrl = downloadUrl.toString(),
                        senderId = senderId
                    )

                    db.collection("chats").document(chatId).collection("messages")
                        .add(newMessage)
                        .addOnSuccessListener {
                            db.collection("chats").document(chatId)
                                .update(
                                    mapOf(
                                        "lastMessage" to "📷 Imagen enviada",
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                )
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al subir imagen", e)
            }
    }
}