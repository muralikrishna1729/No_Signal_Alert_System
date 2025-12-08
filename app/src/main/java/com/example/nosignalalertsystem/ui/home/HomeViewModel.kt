package com.example.nosignalalertsystem.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nosignalalertsystem.data.model.LocationData
import com.example.nosignalalertsystem.data.model.SignalData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _signalFlow = MutableStateFlow(SignalData())
    val signalFlow: StateFlow<SignalData> = _signalFlow

    private val _locationFlow = MutableStateFlow(LocationData())
    val locationFlow: StateFlow<LocationData> = _locationFlow

    private var registered = false

    fun registerReceiver(context: Context) {
        if (registered) return

        val filter = IntentFilter().apply {
            addAction("SERVICE_SIGNAL_UPDATE")
            addAction("SERVICE_LOCATION_UPDATE")
        }

        // Required for Android 13+
        context.registerReceiver(
            receiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )

        registered = true
    }

    fun unregisterReceiver(context: Context) {
        if (registered) {
            context.unregisterReceiver(receiver)
            registered = false
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {

                "SERVICE_SIGNAL_UPDATE" -> {
                    val signal = SignalData(
                        dbm = intent.getIntExtra("dbm", 0),
                        quality = intent.getStringExtra("quality") ?: "",
                        networkType = intent.getStringExtra("networkType") ?: "",
                        isAirplaneMode = intent.getBooleanExtra("airplane", false)
                    )
                    viewModelScope.launch { _signalFlow.emit(signal) }
                }

                "SERVICE_LOCATION_UPDATE" -> {
                    val loc = LocationData(
                        latitude = intent.getDoubleExtra("lat", 0.0),
                        longitude = intent.getDoubleExtra("lon", 0.0),
                        accuracy = intent.getFloatExtra("accuracy", 0f)
                    )
                    viewModelScope.launch { _locationFlow.emit(loc) }
                }
            }
        }
    }
}
