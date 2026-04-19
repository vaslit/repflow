package com.vaslit.repflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vaslit.repflow.domain.ExerciseType
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY startedAt ASC")
    fun observePrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE exerciseType = :exerciseType LIMIT 1")
    fun observeProgram(exerciseType: ExerciseType): Flow<ProgramEntity?>

    @Query("SELECT * FROM programs WHERE exerciseType = :exerciseType LIMIT 1")
    suspend fun getProgram(exerciseType: ExerciseType): ProgramEntity?

    @Insert
    suspend fun insertProgram(program: ProgramEntity): Long

    @Update
    suspend fun updateProgram(program: ProgramEntity)
}

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts WHERE programId = :programId ORDER BY scheduledDate ASC, sessionIndex ASC")
    fun observeWorkouts(programId: Long): Flow<List<WorkoutWithPrescriptions>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkout(workoutId: String): WorkoutWithPrescriptions?

    @Insert
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Insert
    suspend fun insertPrescriptions(items: List<SetPrescriptionEntity>)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Query("SELECT id FROM workouts WHERE programId = :programId AND completed = 0")
    suspend fun getPendingWorkoutIds(programId: Long): List<String>

    @Query("DELETE FROM workouts WHERE id IN (:workoutIds)")
    suspend fun deleteWorkoutsByIds(workoutIds: List<String>)
}

@Dao
interface ResultDao {
    @Transaction
    @Query("SELECT * FROM completed_workouts WHERE programId = :programId ORDER BY completedAt DESC")
    fun observeCompletedWorkouts(programId: Long): Flow<List<CompletedWorkoutWithResults>>

    @Transaction
    @Query("SELECT * FROM completed_workouts WHERE programId = :programId ORDER BY completedAt DESC LIMIT :limit")
    suspend fun recentCompletedWorkouts(programId: Long, limit: Int): List<CompletedWorkoutWithResults>

    @Insert
    suspend fun insertCompletedWorkout(item: CompletedWorkoutEntity): Long

    @Insert
    suspend fun insertSetResults(items: List<SetResultEntity>)
}
