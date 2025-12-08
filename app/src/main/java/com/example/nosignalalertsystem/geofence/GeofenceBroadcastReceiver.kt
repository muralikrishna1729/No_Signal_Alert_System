package com.example.nosignalalertsystem.geofence

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE = "com.example.nosignalalertsystem.action.GEOFENCE_EVENT"

        fun getGeofencePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
                action = ACTION_GEOFENCE
            }

            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != ACTION_GEOFENCE) {
            Log.w(TAG, "Unknown intent action: ${intent.action}")
            return
        }

        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (event.hasError()) {
            Log.e(TAG, "Geofence error: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val geofences = event.triggeringGeofences ?: emptyList()

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER ->
                geofences.forEach { handleEnter(context, it.requestId) }

            Geofence.GEOFENCE_TRANSITION_EXIT ->
                geofences.forEach { handleExit(context, it.requestId) }

            else ->
                Log.w(TAG, "Unknown transition: $transition")
        }
    }

    private fun handleEnter(context: Context, id: String) {
        Toast.makeText(context, "Entered Dead Zone: $id", Toast.LENGTH_LONG).show()
        Log.i(TAG, "ENTER geofence $id")

        // TODO: start dead-zone timer service
    }

    private fun handleExit(context: Context, id: String) {
        Toast.makeText(context, "Exited Dead Zone: $id", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "EXIT geofence $id")

        // TODO: stop dead-zone timer service
    }
}
