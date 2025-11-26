package com.example.nosignalalertsystem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WeakSignalDao {

    @Insert
    suspend fun insertLog(log: WeakSignalEntity)

    @Query("SELECT * FROM weak_signal_logs ORDER BY id DESC")
    suspend fun getAllLogs(): List<WeakSignalEntity>
}
