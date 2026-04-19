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
        ).fallbackToDestructiveMigration().build()
    }

    val repository: AppRepository by lazy {
        AppRepository(
            programDao = database.programDao(),
            workoutDao = database.workoutDao(),
            resultDao = database.resultDao(),
        )
    }
}
