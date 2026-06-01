package com.example.parkme.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.parkme.MainActivity
import com.example.parkme.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                .update("fcmToken", token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Standard notification handling (e.g., Chats)
        remoteMessage.notification?.let {
            showNotification(
                title = it.title,
                body = it.body,
                notifId = System.currentTimeMillis().toInt(),
                maxProgress = 0,
                currentProgress = 0,
                channelId = "chat_notifications",
                channelName = "Mensajes de Chat" // User-facing, keep in Spanish
            )
        }

        // Data Payload handling (Uber-style reservations)
        if (remoteMessage.data.isNotEmpty() && remoteMessage.data["type"] == "RESERVATION_UPDATE") {
            val userName = remoteMessage.data["userName"] ?: "Usuario"
            val etaSeconds = remoteMessage.data["eta"]?.toIntOrNull() ?: 0

            // Safe hashcode to prevent app crash if reservationId is missing
            val reservationId = remoteMessage.data["reservationId"]?.hashCode() ?: System.currentTimeMillis().toInt()

            val remainingMinutes = etaSeconds / 60

            // User-facing text remains in Spanish
            val title = "Reserva en camino: $userName"
            val body = "Llega en aprox $remainingMinutes minutos"

            // Assuming a max estimated trip (e.g., 30 min = 1800 seconds) for the progress bar max value
            val maxProgress = 1800
            val currentProgress = maxProgress - etaSeconds // The lower the ETA, the fuller the bar

            showNotification(
                title = title,
                body = body,
                notifId = reservationId,
                maxProgress = maxProgress,
                currentProgress = currentProgress,
                channelId = "reservation_notifications",
                channelName = "Estado de Reservas" // User-facing, keep in Spanish
            )
        }
    }

    private fun showNotification(
        title: String?,
        body: String?,
        notifId: Int,
        maxProgress: Int,
        currentProgress: Int,
        channelId: String,
        channelName: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        // If maxProgress is greater than 0, display the progress bar
        if (maxProgress > 0) {
            builder.setProgress(maxProgress, currentProgress, false)
            // Prevents the notification from making a sound every single time it updates
            builder.setOnlyAlertOnce(true)
        } else {
            builder.setAutoCancel(true)
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel creation logic is CRITICAL for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(notifId, builder.build())
    }
}