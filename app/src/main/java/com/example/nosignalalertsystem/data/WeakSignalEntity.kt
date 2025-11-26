package com.example.nosignalalertsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weak_signal_logs")
data class WeakSignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val dbm: Int,
    val latitude: Double,
    val longitude: Double
)
