package com.example.nosignalalertsystem.utils

import android.os.Build
import android.telephony.CellSignalStrength
import android.telephony.SignalStrength

object SignalStrengthCallback {

    fun getDbm(signal: SignalStrength?): Int {
        if (signal == null) return -150

        return try {
            // API 29+ (Android 10+) — safe call
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val strength = signal.cellSignalStrengths.firstOrNull()
                strength?.dbm ?: -150
            } else {
                // API 26–28 fallback using getGsmSignalStrength()
                getDbmLegacy(signal)
            }
        } catch (e: Exception) {
            -150
        }
    }

    @Suppress("DEPRECATION")
    private fun getDbmLegacy(signal: SignalStrength): Int {
        return try {
            // GSM phones
            val gsm = signal.gsmSignalStrength
            if (gsm != 99) {
                (2 * gsm) - 113    // Convert ASU → dBm
            } else {
                // CDMA fallback
                val cdmaDbm = signal.cdmaDbm
                val evdoDbm = signal.evdoDbm
                when {
                    cdmaDbm > -120 -> cdmaDbm
                    evdoDbm > -120 -> evdoDbm
                    else -> -150
                }
            }
        } catch (e: Exception) {
            -150
        }
    }
}
