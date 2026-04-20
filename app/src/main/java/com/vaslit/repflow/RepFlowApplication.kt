package com.vaslit.repflow

import android.app.Application
import androidx.room.Room
import com.vaslit.repflow.data.AppDatabase
import com.vaslit.repflow.data.AppRepository

class RepFlowApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "repflow.db",
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
        ).build()
    }

    val repository: AppRepository by lazy {
        AppRepository(
            database = database,
            programDao = database.programDao(),
            workoutDao = database.workoutDao(),
            resultDao = database.resultDao(),
        )
    }
}
