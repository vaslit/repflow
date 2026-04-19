package com.vaslit.repflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vaslit.repflow.domain.ExerciseType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE archivedAt IS NULL ORDER BY startedAt ASC")
    fun observePrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE exerciseType = :exerciseType AND archivedAt IS NULL LIMIT 1")
    fun observeProgram(exerciseType: ExerciseType): Flow<ProgramEntity?>

    @Query("SELECT * FROM programs WHERE exerciseType = :exerciseType AND archivedAt IS NULL LIMIT 1")
    suspend fun getProgram(exerciseType: ExerciseType): ProgramEntity?

    @Query("SELECT * FROM programs WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun observeArchivedPrograms(): Flow<List<ProgramEntity>>

    @Insert
    suspend fun insertProgram(program: ProgramEntity): Long

    @Query("UPDATE programs SET archivedAt = :archivedAt WHERE id = :programId")
    suspend fun archiveProgramById(programId: Long, archivedAt: Instant)

    @Query("DELETE FROM programs WHERE id = :programId")
    suspend fun deleteProgramById(programId: Long)

    @Update
    suspend fun updateProgram(program: ProgramEntity)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY scheduledDate ASC, sessionIndex ASC")
    fun observeWorkoutEntities(): Flow<List<WorkoutEntity>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE programId = :programId ORDER BY scheduledDate ASC, sessionIndex ASC")
    fun observeWorkouts(programId: Long): Flow<List<WorkoutWithPrescriptions>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkout(workoutId: String): WorkoutWithPrescriptions?

    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkoutEntity(workoutId: String): WorkoutEntity?

    @Insert
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Insert
    suspend fun insertPrescriptions(items: List<SetPrescriptionEntity>)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Query("SELECT id FROM workouts WHERE programId = :programId AND completed = 0")
    suspend fun getPendingWorkoutIds(programId: Long): List<String>

    @Query("SELECT * FROM workouts WHERE programId = :programId AND completed = 0 ORDER BY scheduledDate ASC, sessionIndex ASC")
    suspend fun getPendingWorkoutEntities(programId: Long): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE programId = :programId AND phaseIndex = :phaseIndex ORDER BY scheduledDate ASC, sessionIndex ASC")
    suspend fun getWorkoutsForPhase(programId: Long, phaseIndex: Int): List<WorkoutEntity>

    @Query("UPDATE workouts SET scheduledDate = :scheduledDate WHERE id = :workoutId")
    suspend fun updateWorkoutScheduledDate(workoutId: String, scheduledDate: LocalDate)

    @Query("DELETE FROM workouts WHERE id IN (:workoutIds)")
    suspend fun deleteWorkoutsByIds(workoutIds: List<String>)
}

@Dao
interface ResultDao {
    @Transaction
    @Query("SELECT * FROM completed_workouts ORDER BY completedAt DESC")
    fun observeAllCompletedWorkouts(): Flow<List<CompletedWorkoutWithResults>>

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
