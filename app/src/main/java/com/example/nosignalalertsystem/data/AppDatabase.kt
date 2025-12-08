package com.example.nosignalalertsystem.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// FIX: 1. Add EmergencyContact::class to entities array
// FIX: 2. Increment version number from 1 to 2
@Database(
    entities = [WeakSignalEntity::class, EmergencyContact::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weakSignalDao(): WeakSignalDao

    // NEW: Abstract function for the new DAO
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "signal_db"
                )
                    // NOTE: When migrating an existing app, you must add a migration strategy here:
                    // .fallbackToDestructiveMigration() // Recommended for simple development
                    // .addMigrations(MIGRATION_1_2) // Recommended for production

                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}