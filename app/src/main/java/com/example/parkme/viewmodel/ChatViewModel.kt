package com.example.parkme.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.parkme.models.ChatMessage
import com.example.parkme.models.ChatRoom
import com.example.parkme.models.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _chatPartnerName = MutableStateFlow<String>("User")
    val chatPartnerName: StateFlow<String> = _chatPartnerName.asStateFlow()

    private val _currentChatRoom = MutableStateFlow<ChatRoom?>(null)
    val currentChatRoom: StateFlow<ChatRoom?> = _currentChatRoom.asStateFlow()
    private val _chatPartnerImage = MutableStateFlow<String?>(null)
    val chatPartnerImage: StateFlow<String?> = _chatPartnerImage.asStateFlow()
    fun listenCurrentChatRoom(chatId: String) {
        db.collection("chats").document(chatId).addSnapshotListener { snapshot, error ->
            if (snapshot != null && snapshot.exists()) {
                _currentChatRoom.value = snapshot.toObject(ChatRoom::class.java)
            }
        }
    }

    fun toggleLocationSharing(chatId: String, isSharing: Boolean) {
        db.collection("chats").document(chatId)
            .set(mapOf("sharingLocation" to isSharing), SetOptions.merge())
    }

    fun updateUserLocation(userId: String, lat: Double, lng: Double) {
        db.collection("users").document(userId).update(
            mapOf(
                "latitude" to lat,
                "longitude" to lng
            )
        )
    }

    fun loadChatPartnerName(chatId: String, isOperator: Boolean) {
        _chatPartnerImage.value = null

        db.collection("chats").document(chatId).get().addOnSuccessListener { doc ->
            val room = doc.toObject(ChatRoom::class.java)
            if (room != null) {
                if (isOperator) {
                    db.collection("users").document(room.userId).get().addOnSuccessListener { userDoc ->
                        val name = userDoc.getString("name") ?: "Usuario"
                        val lastName = userDoc.getString("lastName") ?: ""
                        _chatPartnerName.value = "$name $lastName".trim()

                        _chatPartnerImage.value = userDoc.getString("profileImage")
                    }
                } else {
                    _chatPartnerName.value = room.parkingName

                    db.collection("users").document(room.operatorId).get().addOnSuccessListener { opDoc ->
                        _chatPartnerImage.value = opDoc.getString("profileImage")
                    }
                }
            }
        }.addOnFailureListener {
            _chatPartnerName.value = "Chat"
        }
    }

    fun initChatRoom(reservation: Reservation) {
        db.collection("users").document(reservation.userId).get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc.getString("name") ?: "Usuario"
                val lastName = userDoc.getString("lastName") ?: ""
                val fullUserName = "$name $lastName".trim()

                val chatRoom = ChatRoom(
                    id = reservation.id,
                    parkingName = reservation.parkingName,
                    userName = fullUserName,
                    userId = reservation.userId,
                    operatorId = reservation.operatorId,
                    sharingLocation = true
                )

                db.collection("chats").document(reservation.id)
                    .set(chatRoom, SetOptions.merge())
                    .addOnSuccessListener { Log.i("ChatViewModel", "Sala de chat asegurada con nombre") }
                    .addOnFailureListener { e -> Log.e("ChatViewModel", "Error al crear sala", e) }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error al buscar usuario, usando genérico", e)
                val chatRoom = ChatRoom(
                    id = reservation.id,
                    parkingName = reservation.parkingName,
                    userName = "Usuario",
                    userId = reservation.userId,
                    operatorId = reservation.operatorId,
                    sharingLocation = true
                )
                db.collection("chats").document(reservation.id).set(chatRoom, SetOptions.merge())
            }
    }

    fun fetchMyChats(userId: String, isOperator: Boolean) {
        val searchField = if (isOperator) "operatorId" else "userId"

        db.collection("chats")
            .whereEqualTo(searchField, userId)
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
                                        "lastMessage" to "Imagen enviada",
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