package com.example.nosignalalertsystem.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.nosignalalertsystem.R
import com.example.nosignalalertsystem.data.AppDatabase
import com.example.nosignalalertsystem.data.WeakSignalEntity
import com.example.nosignalalertsystem.service.SignalForegroundService
import com.example.nosignalalertsystem.utils.SignalStrengthCallback
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var db: AppDatabase

    private lateinit var tvDbm: TextView
    private lateinit var tvQuality: TextView
    private lateinit var tvNetwork: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvSignal: TextView

    private var lastLat = 0.0
    private var lastLon = 0.0
    private var isAirplaneMode = false
    private var isLocationUpdatesRequested = false

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    companion object {
        private const val PERMISSION_REQUEST = 900
    }

    private val gpsCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            lastLat = loc.latitude
            lastLon = loc.longitude

            tvLocation.text = "Lat: $lastLat\nLon: $lastLon"
        }
    }

    // -------------------------------------------------------------
    // SIGNAL LISTENER
    // -------------------------------------------------------------
    private val signalListener = object : PhoneStateListener() {

        override fun onSignalStrengthsChanged(strength: SignalStrength?) {
            super.onSignalStrengthsChanged(strength)
            if (!isAdded || strength == null) return

            val dbm = SignalStrengthCallback.getDbm(strength)
            val quality = getQuality(dbm)
            val type = getNetworkType()

            tvDbm.text = "Signal: $dbm dBm"
            tvQuality.text = "Quality: $quality"
            tvNetwork.text = "Network: $type"

            updateSignalStatus(dbm)
            logWeakSignal(dbm)
        }

        override fun onServiceStateChanged(serviceState: ServiceState?) {
            super.onServiceStateChanged(serviceState)
            if (!isAdded) return

            isAirplaneMode = serviceState?.state == ServiceState.STATE_POWER_OFF
            val isOutOfService = serviceState?.state == ServiceState.STATE_OUT_OF_SERVICE

            when {
                isAirplaneMode -> {
                    tvSignal.text = "Airplane Mode"
                    tvSignal.setTextColor(Color.BLUE)
                }
                isOutOfService -> {
                    tvSignal.text = "No Service"
                    tvSignal.setTextColor(Color.RED)
                }
            }
        }
    }

    // -------------------------------------------------------------
    // ON VIEW CREATED
    // -------------------------------------------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDbm = view.findViewById(R.id.signalDbm)
        tvQuality = view.findViewById(R.id.signalQuality)
        tvNetwork = view.findViewById(R.id.networkType)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvSignal = view.findViewById(R.id.tvSignal)

        telephonyManager = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        fusedLocation = LocationServices.getFusedLocationProviderClient(requireContext())
        db = AppDatabase.getDatabase(requireContext())

        createLocationRequest()

        view.findViewById<Button>(R.id.btnStartService).setOnClickListener { startMonitoring() }
        view.findViewById<Button>(R.id.btnStopService).setOnClickListener { stopMonitoring() }

        // Register broadcast receiver
        viewModel.registerReceiver(requireContext())

        lifecycleScope.launchWhenStarted {
            viewModel.signalFlow.collect {
                tvDbm.text = "Signal: ${it.dbm} dBm"
                tvQuality.text = "Quality: ${it.quality}"
                tvNetwork.text = "Network: ${it.networkType}"
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.locationFlow.collect {
                tvLocation.text = "Lat: ${it.latitude}\nLon: ${it.longitude}"
            }
        }
    }

    // -------------------------------------------------------------
    // SIGNAL QUALITY
    // -------------------------------------------------------------
    private fun getQuality(dbm: Int): String = when {
        dbm >= -85 -> "Excellent"
        dbm >= -95 -> "Good"
        dbm >= -105 -> "Fair"
        dbm >= -115 -> "Weak"
        else -> "Dead Zone"
    }

    private fun getNetworkType(): String {
        return when (telephonyManager.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            else -> "Unknown"
        }
    }

    private fun updateSignalStatus(dbm: Int) {
        tvSignal.text = "Signal: $dbm dBm"
        tvSignal.setTextColor(if (dbm <= -115) Color.RED else Color.BLACK)
    }

    // -------------------------------------------------------------
    // START / STOP MONITORING
    // -------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private fun startMonitoring() {
        if (!hasPermissions()) {
            requestPermissions(permissions, PERMISSION_REQUEST)
            return
        }

        requireContext().startForegroundService(Intent(requireContext(), SignalForegroundService::class.java))

        telephonyManager.listen(
            signalListener,
            PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE
        )

        fusedLocation.requestLocationUpdates(locationRequest, gpsCallback, Looper.getMainLooper())
        isLocationUpdatesRequested = true

        Toast.makeText(requireContext(), "Monitoring Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopMonitoring() {
        telephonyManager.listen(null, PhoneStateListener.LISTEN_NONE)

        if (isLocationUpdatesRequested) {
            fusedLocation.removeLocationUpdates(gpsCallback)
            isLocationUpdatesRequested = false
        }

        requireContext().stopService(Intent(requireContext(), SignalForegroundService::class.java))

        Toast.makeText(requireContext(), "Monitoring Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun hasPermissions() = permissions.all {
        ActivityCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    // -------------------------------------------------------------
    // LOCATION REQUEST
    // -------------------------------------------------------------
    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1500)
            .build()
    }

    // -------------------------------------------------------------
    // LOG WEAK SIGNAL
    // -------------------------------------------------------------
    private var lastLog = 0L
    private val logGap = 60000L

    private fun logWeakSignal(dbm: Int) {
        if (dbm > -115) return

        val now = System.currentTimeMillis()
        if (now - lastLog < logGap) return
        lastLog = now

        lifecycleScope.launch {
            db.weakSignalDao().insertLog(
                WeakSignalEntity(
                    timestamp = now,
                    dbm = dbm,
                    latitude = lastLat,
                    longitude = lastLon
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.unregisterReceiver(requireContext())
    }
}
