package com.example.nosignalalertsystem.data.model

data class SignalData(
    val dbm: Int = 0,
    val quality: String = "",
    val networkType: String = "",
    val isAirplaneMode: Boolean = false,
    val timestamp: Long = 0
)
