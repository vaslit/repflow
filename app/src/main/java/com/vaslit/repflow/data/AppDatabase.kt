package com.vaslit.repflow.data

import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaslit.repflow.domain.DifficultyLevel
import com.vaslit.repflow.domain.ExerciseType
import com.vaslit.repflow.domain.ProgressionLevel
import com.vaslit.repflow.domain.ProgressionRecommendation
import java.time.Instant
import java.time.LocalDate

@Database(
    entities = [
        ProgramEntity::class,
        WorkoutEntity::class,
        SetPrescriptionEntity::class,
        CompletedWorkoutEntity::class,
        SetResultEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun resultDao(): ResultDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE programs ADD COLUMN preferredDays TEXT NOT NULL DEFAULT 'MONDAY,WEDNESDAY,FRIDAY'")
                db.execSQL("ALTER TABLE programs ADD COLUMN activePhaseIndex INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE workouts ADD COLUMN phaseIndex INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE programs ADD COLUMN bestGoalScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE programs ADD COLUMN maintenanceMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE programs ADD COLUMN definitionId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE programs ADD COLUMN archivedAt TEXT")
                db.execSQL(
                    """
                    UPDATE programs
                    SET definitionId = CASE exerciseType
                        WHEN 'PULL_UP' THEN 'pull_up_foundation_v1'
                        WHEN 'PUSH_UP' THEN 'push_up_foundation_v1'
                        ELSE ''
                    END
                    """.trimIndent(),
                )
                db.execSQL("ALTER TABLE set_prescriptions ADD COLUMN variantName TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}

class DbConverters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToString(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun exerciseToString(value: ExerciseType?): String? = value?.name

    @TypeConverter
    fun stringToExercise(value: String?): ExerciseType? = value?.let(ExerciseType::valueOf)

    @TypeConverter
    fun levelToString(value: ProgressionLevel?): String? = value?.name

    @TypeConverter
    fun stringToLevel(value: String?): ProgressionLevel? = value?.let(ProgressionLevel::valueOf)

    @TypeConverter
    fun difficultyToString(value: DifficultyLevel?): String? = value?.name

    @TypeConverter
    fun stringToDifficulty(value: String?): DifficultyLevel? = value?.let(DifficultyLevel::valueOf)

    @TypeConverter
    fun recommendationToString(value: ProgressionRecommendation?): String? = value?.name

    @TypeConverter
    fun stringToRecommendation(value: String?): ProgressionRecommendation? =
        value?.let(ProgressionRecommendation::valueOf)
}

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val definitionId: String,
    val exerciseType: ExerciseType,
    val currentLevel: ProgressionLevel,
    val currentDifficulty: DifficultyLevel,
    val preferredDays: String,
    val activePhaseIndex: Int,
    val bestGoalScore: Int,
    val maintenanceMode: Boolean,
    val successfulSessions: Int,
    val underperformSessions: Int,
    val passedTests: Int,
    val passedBandedTests: Int,
    val changeBandUnlocked: Boolean,
    val startedAt: LocalDate,
    val archivedAt: Instant? = null,
)

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programId")],
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val programId: Long,
    val exerciseType: ExerciseType,
    val title: String,
    val phaseIndex: Int,
    val weekIndex: Int,
    val sessionIndex: Int,
    val scheduledDate: LocalDate,
    val isTest: Boolean,
    val level: ProgressionLevel,
    val difficulty: DifficultyLevel,
    val techniqueTitle: String,
    val techniqueBody: String,
    val transitionHint: String,
    val completed: Boolean,
)

@Entity(
    tableName = "set_prescriptions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class SetPrescriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: String,
    val setIndex: Int,
    val variantName: String,
    val targetReps: Int?,
    val targetSeconds: Int?,
    val restSeconds: Int,
    val note: String,
)

@Entity(
    tableName = "completed_workouts",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programId"), Index("workoutId", unique = true)],
)
data class CompletedWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: String,
    val programId: Long,
    val completedAt: Instant,
    val successRate: Double,
    val recommendation: ProgressionRecommendation,
    val summary: String,
)

@Entity(
    tableName = "set_results",
    foreignKeys = [
        ForeignKey(
            entity = CompletedWorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["completedWorkoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("completedWorkoutId")],
)
data class SetResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedWorkoutId: Long,
    val setIndex: Int,
    val actualReps: Int?,
    val actualSeconds: Int?,
    val completed: Boolean,
)

data class WorkoutWithPrescriptions(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val prescriptions: List<SetPrescriptionEntity>,
)

data class CompletedWorkoutWithResults(
    @Embedded val completion: CompletedWorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "completedWorkoutId",
    )
    val setResults: List<SetResultEntity>,
    @Relation(
        parentColumn = "workoutId",
        entityColumn = "id",
    )
    val workout: WorkoutEntity?,
    @Relation(
        parentColumn = "workoutId",
        entityColumn = "workoutId",
    )
    val prescriptions: List<SetPrescriptionEntity>,
)
