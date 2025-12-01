package com.example.nosignalalertsystem.data

import androidx.room.*
@Dao
interface WeakSignalDao {

    @Insert
    suspend fun insertLog(log: WeakSignalEntity)

    @Query("SELECT * FROM weak_signal_logs ORDER BY id DESC")
    suspend fun getAllLogs(): List<WeakSignalEntity>

    @Query("DELETE FROM weak_signal_logs")
    suspend fun deleteAllLogs()

    @Delete
    suspend fun deleteLog(log: WeakSignalEntity)
}
