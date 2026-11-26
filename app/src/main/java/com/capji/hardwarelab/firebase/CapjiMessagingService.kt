package com.capji.hardwarelab.firebase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.capji.hardwarelab.MainActivity
import com.capji.hardwarelab.R
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

object Push {
    const val CHANNEL_ID = "capji_updates"
    const val TOPIC = "all_users"

    fun subscribe(ctx: Context) {
        try {
            if (FirebaseApp.getApps(ctx).isEmpty()) return
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
        } catch (_: Throwable) {
        }
    }

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "CAPJI Updates", NotificationManager.IMPORTANCE_DEFAULT),
                )
            }
        }
    }
}

class CapjiMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Push.subscribe(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification
        val title = n?.title ?: message.data["title"] ?: return
        val body = n?.body ?: message.data["body"] ?: ""
        Push.ensureChannel(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(this, Push.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_capji)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notif)
        } catch (_: SecurityException) {
        }
    }
}
