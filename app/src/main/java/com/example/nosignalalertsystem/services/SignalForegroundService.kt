package com.example.nosignalalertsystem.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.nosignalalertsystem.R

class SignalForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "signal_tracking_channel"
        const val ALERT_CHANNEL_ID = "signal_alert_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()

        startForeground(
            1,
            buildNotification("Monitoring Active…")
        )
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_signal)
            .setContentTitle("No Signal Alert System")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val main = NotificationChannel(
            CHANNEL_ID,
            "Signal Tracking",
            NotificationManager.IMPORTANCE_LOW
        )

        val alert = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Weak Signal Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )

        manager.createNotificationChannel(main)
        manager.createNotificationChannel(alert)
    }

    fun broadcastSignal(dbm: Int, quality: String, type: String, airplane: Boolean) {
        val intent = Intent("SERVICE_SIGNAL_UPDATE").apply {
            putExtra("dbm", dbm)
            putExtra("quality", quality)
            putExtra("networkType", type)
            putExtra("airplane", airplane)
        }
        sendBroadcast(intent)
    }

    fun broadcastLocation(lat: Double, lon: Double, accuracy: Float) {
        val intent = Intent("SERVICE_LOCATION_UPDATE").apply {
            putExtra("lat", lat)
            putExtra("lon", lon)
            putExtra("accuracy", accuracy)
        }
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
