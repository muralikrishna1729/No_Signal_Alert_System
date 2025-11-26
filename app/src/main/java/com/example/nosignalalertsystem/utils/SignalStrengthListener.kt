package com.example.nosignalalertsystem.utils

import android.telephony.SignalStrength

/**
 * Safe dBm extractor for ALL Android versions (API 26 → 34)
 * We avoid "dbm" property entirely (requires API 31)
 * We avoid cellSignalStrengths (requires API 29)
 */
object SignalStrengthCallback {

    fun getDbm(signalStrength: SignalStrength?): Int {
        if (signalStrength == null) return -150
        return try {
            // Reflection works on ALL Android versions
            val method = SignalStrength::class.java.getMethod("getDbm")
            (method.invoke(signalStrength) as? Int) ?: -150
        } catch (e: Exception) {
            -150
        }
    }
}
