package com.example.nosignalalertsystem.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.nosignalalertsystem.R
import com.example.nosignalalertsystem.service.SignalForegroundService
import com.example.nosignalalertsystem.utils.SignalStrengthCallback
import com.google.android.gms.location.*
import android.app.NotificationManager
import android.app.Notification

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var telephonyManager: TelephonyManager

    // Location components
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    // Telephony modern callback (API 31+)
    private var modernCallback: TelephonyCallback? = null
    private var lastAlertTime = 0L
    private val alertCooldown = 60_000 // 1 minute to prevent spam


    // Legacy signal listener (API 26–30)
    private val legacySignalListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            val dbm = SignalStrengthCallback.getDbm(signalStrength)
            updateSignalUI(dbm)
        }
    }

    // GPS callback
    private val gpsCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            updateLocationUI(loc.latitude, loc.longitude)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        telephonyManager =
            requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        setupLocationRequest()

        view.findViewById<Button>(R.id.btnStartService).setOnClickListener {
            startMonitoring()
        }

        view.findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopMonitoring()
        }

        requestPhonePermission()
        requestLocationPermission()
    }

    // -------------------- SIGNAL UI --------------------
    private fun updateSignalUI(dBm: Int) {
        val tv = view?.findViewById<TextView>(R.id.tvSignal) ?: return
        tv.text = "Signal Strength: $dBm dBm"

        if (dBm <= -115) {
            tv.setTextColor(Color.RED)
            showWeakSignalAlert(dBm)  // ← NEW
        } else {
            tv.setTextColor(Color.BLACK)
        }
    }

    // -------------------- LOCATION UI --------------------
    private fun updateLocationUI(lat: Double, lon: Double) {
        val tv = view?.findViewById<TextView>(R.id.tvLocation) ?: return
        tv.text = "Location: $lat, $lon"
    }



    // -------------------- PERMISSIONS --------------------
    private fun requestPhonePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                100
            )
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                200
            )
        }
    }

    // -------------------- SIGNAL MONITOR --------------------
    private fun startSignalMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback = object : TelephonyCallback(),
                TelephonyCallback.SignalStrengthsListener {

                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    val dbm = SignalStrengthCallback.getDbm(signalStrength)
                    updateSignalUI(dbm)
                }
            }

            telephonyManager.registerTelephonyCallback(
                ContextCompat.getMainExecutor(requireContext()),
                modernCallback!!
            )
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(
                legacySignalListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
            )
        }
    }

    // -------------------- LOCATION MONITOR --------------------
    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000 // every 3 seconds
        ).build()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            gpsCallback,
            Looper.getMainLooper()
        )
    }
    private fun showWeakSignalAlert(dBm: Int) {
        val now = System.currentTimeMillis()

        // Cooldown to avoid repeated alerts
        if (now - lastAlertTime < alertCooldown) return
        lastAlertTime = now

        val notification = NotificationCompat.Builder(requireContext(), SignalForegroundService.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Weak Signal Detected")
            .setContentText("Signal is very weak ($dBm dBm)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .build()

        val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2002, notification)
    }



    // -------------------- START / STOP MONITORING --------------------
    private fun startMonitoring() {
        val intent = Intent(requireContext(), SignalForegroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            requireContext().startForegroundService(intent)
        else
            requireContext().startService(intent)

        Toast.makeText(requireContext(), "Monitoring Started", Toast.LENGTH_SHORT).show()

        startSignalMonitoring()
        startLocationUpdates()
    }

    private fun stopMonitoring() {
        fusedLocationClient.removeLocationUpdates(gpsCallback)

        requireContext().stopService(
            Intent(requireContext(), SignalForegroundService::class.java)
        )

        Toast.makeText(requireContext(), "Monitoring Stopped", Toast.LENGTH_SHORT).show()
    }

    // -------------------- CLEAN UP --------------------
    override fun onDestroyView() {
        super.onDestroyView()

        fusedLocationClient.removeLocationUpdates(gpsCallback)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(null, PhoneStateListener.LISTEN_NONE)
        }
    }
}
