package com.example.nosignalalertsystem.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)

    /**
     * Retrieves all contacts, emitted as a Flow to allow UI updates whenever the data changes.
     */
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    /**
     * Used by the background service to quickly get phone numbers for sending SMS alerts.
     */
    @Query("SELECT phoneNumber FROM emergency_contacts")
    suspend fun getAllPhoneNumbers(): List<String>
}