package com.example.nosignalalertsystem.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nosignalalertsystem.R
import com.example.nosignalalertsystem.data.AppDatabase
import com.example.nosignalalertsystem.data.WeakSignalEntity
import com.example.nosignalalertsystem.service.SignalForegroundService
import com.example.nosignalalertsystem.utils.SignalStrengthCallback
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var db: AppDatabase

    private var lastLocationLat = 0.0
    private var lastLocationLon = 0.0
    private var lastLoggedTime = 0L
    private val logCooldown = 60_000L // log once per minute
    private var lastAlertTime = 0L
    private val alertCooldown = 60_000L // alert once per minute

    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    // --------------------- PHONE SIGNAL LISTENER ---------------------
    private val signalListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            val dbm = SignalStrengthCallback.getDbm(signalStrength)
            updateSignalUI(dbm)
            handleWeakSignal(dbm)
        }
    }

    // --------------------- LOCATION CALLBACK -------------------------
    private val gpsCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            lastLocationLat = loc.latitude
            lastLocationLon = loc.longitude
            updateLocationUI(loc.latitude, loc.longitude)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())
        telephonyManager = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        setupLocationRequest()

        view.findViewById<Button>(R.id.btnStartService).setOnClickListener {
            startMonitoring()
        }

        view.findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopMonitoring()
        }
    }

    // --------------------- SIGNAL UI ---------------------
    private fun updateSignalUI(dbm: Int) {
        val tv = view?.findViewById<TextView>(R.id.tvSignal) ?: return
        tv.text = "Signal Strength: $dbm dBm"
        tv.setTextColor(if (dbm <= -115) Color.RED else Color.BLACK)
    }

    // --------------------- LOCATION UI ---------------------
    private fun updateLocationUI(lat: Double, lon: Double) {
        val tv = view?.findViewById<TextView>(R.id.tvLocation) ?: return
        tv.text = "Location: $lat, $lon"
    }

    // ----------------------------------------------------------------
    // PERMISSIONS
    // ----------------------------------------------------------------
    private fun hasPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), PERMISSIONS, 101)
    }

    // ----------------------------------------------------------------
    // MONITORING CONTROL
    // ----------------------------------------------------------------
    private fun startMonitoring() {
        if (!hasPermissions()) {
            requestPermissions()
            return
        }

        // Start Foreground Service
        val intent = Intent(requireContext(), SignalForegroundService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)

        // Signal listener
        telephonyManager.listen(
            signalListener,
            PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
        )

        // Location listener
        fusedClient.requestLocationUpdates(locationRequest, gpsCallback, Looper.getMainLooper())

        Toast.makeText(requireContext(), "Monitoring Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopMonitoring() {
        telephonyManager.listen(null, PhoneStateListener.LISTEN_NONE)
        fusedClient.removeLocationUpdates(gpsCallback)

        requireContext().stopService(
            Intent(requireContext(), SignalForegroundService::class.java)
        )

        Toast.makeText(requireContext(), "Monitoring Stopped", Toast.LENGTH_SHORT).show()
    }

    // --------------------- LOCATION REQUEST ---------------------
    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).setMinUpdateDistanceMeters(5f).build()
    }

    // ----------------------------------------------------------------
    // WEAK SIGNAL HANDLING (ALERT + DB LOGGING)
    // ----------------------------------------------------------------
    private fun handleWeakSignal(dbm: Int) {
        if (dbm > -115) return

        // Notification alert once per minute
        val now = System.currentTimeMillis()
        if (now - lastAlertTime > alertCooldown) {
            lastAlertTime = now
            showWeakSignalNotification(dbm)
        }

        // DB logging once per minute
        if (now - lastLoggedTime > logCooldown) {
            lastLoggedTime = now
            saveWeakSignalLocation(dbm)
        }
    }

    private fun showWeakSignalNotification(dbm: Int) {
        val notif = androidx.core.app.NotificationCompat.Builder(
            requireContext(),
            SignalForegroundService.ALERT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Weak Signal Detected")
            .setContentText("Signal is $dbm dBm")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(2024, notif)
    }

    // --------------------- DB LOGGING ---------------------
    private fun saveWeakSignalLocation(dbm: Int) {
        lifecycleScope.launch {
            db.weakSignalDao().insertLog(
                WeakSignalEntity(
                    timestamp = System.currentTimeMillis(),
                    dbm = dbm,
                    latitude = lastLocationLat,
                    longitude = lastLocationLon
                )
            )
        }
    }

    // --------------------- CLEANUP ---------------------
    override fun onDestroyView() {
        super.onDestroyView()
        telephonyManager.listen(null, PhoneStateListener.LISTEN_NONE)
        fusedClient.removeLocationUpdates(gpsCallback)
    }
}
