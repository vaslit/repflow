package com.vaslit

import android.app.Application
import androidx.room.Room
import com.vaslit.data.AppDatabase
import com.vaslit.data.AppRepository

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
