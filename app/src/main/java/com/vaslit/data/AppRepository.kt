package com.vaslit.data

import com.vaslit.domain.AssessmentResult
import com.vaslit.domain.DefaultPlannerEngine
import com.vaslit.domain.DifficultyLevel
import com.vaslit.domain.EvaluationSnapshot
import com.vaslit.domain.ExerciseType
import com.vaslit.domain.PlanWeek
import com.vaslit.domain.PlannerEngine
import com.vaslit.domain.ProgramState
import com.vaslit.domain.ProgressionLevel
import com.vaslit.domain.SessionResult
import com.vaslit.domain.SetPrescription
import com.vaslit.domain.SetResult
import com.vaslit.domain.TechniqueTip
import com.vaslit.domain.TrainingCatalog
import com.vaslit.domain.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

data class ProgramSummary(
    val exerciseType: ExerciseType,
    val currentLevel: ProgressionLevel,
    val currentDifficulty: DifficultyLevel,
)

data class HistoryEntry(
    val completedAt: Instant,
    val title: String,
    val level: ProgressionLevel,
    val difficulty: DifficultyLevel,
    val successRate: Double,
    val summary: String,
)

data class ProgramDetail(
    val summary: ProgramSummary,
    val workouts: List<WorkoutSession>,
    val history: List<HistoryEntry>,
    val techniqueTip: TechniqueTip,
)

class AppRepository(
    private val programDao: ProgramDao,
    private val workoutDao: WorkoutDao,
    private val resultDao: ResultDao,
    private val plannerEngine: PlannerEngine = DefaultPlannerEngine(),
) {

    fun observeProgramSummaries(): Flow<List<ProgramSummary>> =
        programDao.observePrograms().map { programs ->
            programs.map {
                ProgramSummary(
                    exerciseType = it.exerciseType,
                    currentLevel = it.currentLevel,
                    currentDifficulty = it.currentDifficulty,
                )
            }
        }

    fun observeProgramDetail(exerciseType: ExerciseType): Flow<ProgramDetail?> =
        programDao.observeProgram(exerciseType).flatMapLatest { program ->
            if (program == null) {
                flowOf(null)
            } else {
                combine(
                    workoutDao.observeWorkouts(program.id),
                    resultDao.observeCompletedWorkouts(program.id),
                ) { workouts, history ->
                    ProgramDetail(
                        summary = ProgramSummary(
                            exerciseType = program.exerciseType,
                            currentLevel = program.currentLevel,
                            currentDifficulty = program.currentDifficulty,
                        ),
                        workouts = workouts.map { it.toDomain() },
                        history = history.mapNotNull { item ->
                            val workout = item.workout ?: return@mapNotNull null
                            HistoryEntry(
                                completedAt = item.completion.completedAt,
                                title = workout.title,
                                level = workout.level,
                                difficulty = workout.difficulty,
                                successRate = item.completion.successRate,
                                summary = item.completion.summary,
                            )
                        },
                        techniqueTip = TrainingCatalog.techniqueTip(program.exerciseType, program.currentLevel),
                    )
                }
            }
        }

    suspend fun createProgram(assessmentResult: AssessmentResult) {
        val existing = programDao.getProgram(assessmentResult.exerciseType)
        if (existing != null) return

        val programId = programDao.insertProgram(
            ProgramEntity(
                exerciseType = assessmentResult.exerciseType,
                currentLevel = assessmentResult.level,
                currentDifficulty = assessmentResult.difficulty,
                successfulSessions = 0,
                underperformSessions = 0,
                passedTests = 0,
                passedBandedTests = 0,
                changeBandUnlocked = false,
                startedAt = LocalDate.now(),
            ),
        )
        insertPlan(programId, plannerEngine.buildInitialPlan(assessmentResult))
    }

    suspend fun getWorkout(workoutId: String): WorkoutSession? =
        workoutDao.getWorkout(workoutId)?.toDomain()

    suspend fun completeWorkout(exerciseType: ExerciseType, workoutId: String, setResults: List<SetResult>): EvaluationSnapshot {
        val program = requireNotNull(programDao.getProgram(exerciseType))
        val workoutWithPrescriptions = requireNotNull(workoutDao.getWorkout(workoutId))
        val sessionResult = SessionResult(
            workout = workoutWithPrescriptions.toDomain(),
            setResults = setResults,
        )
        val recentHistory = resultDao.recentCompletedWorkouts(program.id, 2).map { it.toSessionResult() }
        val evaluation = plannerEngine.evaluateWorkout(
            sessionResult = sessionResult,
            recentHistory = recentHistory,
            currentState = program.toState(),
        )

        workoutDao.updateWorkout(workoutWithPrescriptions.workout.copy(completed = true))
        val completionId = resultDao.insertCompletedWorkout(
            CompletedWorkoutEntity(
                workoutId = workoutId,
                programId = program.id,
                completedAt = Instant.now(),
                successRate = evaluation.successRate,
                recommendation = evaluation.recommendation,
                summary = evaluation.summary,
            ),
        )
        resultDao.insertSetResults(
            setResults.map {
                SetResultEntity(
                    completedWorkoutId = completionId,
                    setIndex = it.setIndex,
                    actualReps = it.actualReps,
                    actualSeconds = it.actualSeconds,
                    completed = it.completed,
                )
            },
        )

        programDao.updateProgram(program.applyEvaluation(evaluation))
        val pendingIds = workoutDao.getPendingWorkoutIds(program.id)
        if (pendingIds.isNotEmpty()) {
            workoutDao.deleteWorkoutsByIds(pendingIds)
        }
        insertPlan(program.id, plannerEngine.scheduleNextPhase(evaluation.suggestedState))

        return evaluation
    }

    fun techniqueGuide(exerciseType: ExerciseType): List<Pair<ProgressionLevel, TechniqueTip>> =
        when (exerciseType) {
            ExerciseType.PULL_UP -> listOf(
                ProgressionLevel.SCAPULAR_HANG,
                ProgressionLevel.NEGATIVE,
                ProgressionLevel.BANDED,
                ProgressionLevel.STRICT,
            )

            ExerciseType.PUSH_UP -> listOf(
                ProgressionLevel.WALL,
                ProgressionLevel.INCLINE,
                ProgressionLevel.KNEE,
                ProgressionLevel.CLASSIC,
            )
        }.map { level -> level to TrainingCatalog.techniqueTip(exerciseType, level) }

    private suspend fun insertPlan(programId: Long, plan: com.vaslit.domain.ProgramPlan) {
        val workouts = plan.weeks.flatMap(PlanWeek::sessions)
        workoutDao.insertWorkouts(workouts.map { it.toEntity(programId) })
        workoutDao.insertPrescriptions(
            workouts.flatMap { workout ->
                workout.prescriptions.map { it.toEntity(workout.id) }
            },
        )
    }
}

private fun ProgramEntity.toState(): ProgramState = ProgramState(
    exerciseType = exerciseType,
    currentLevel = currentLevel,
    currentDifficulty = currentDifficulty,
    successfulSessions = successfulSessions,
    underperformSessions = underperformSessions,
    passedTests = passedTests,
    passedBandedTests = passedBandedTests,
    changeBandUnlocked = changeBandUnlocked,
)

private fun ProgramEntity.applyEvaluation(evaluation: EvaluationSnapshot): ProgramEntity = copy(
    currentLevel = evaluation.suggestedState.currentLevel,
    currentDifficulty = evaluation.suggestedState.currentDifficulty,
    successfulSessions = evaluation.suggestedState.successfulSessions,
    underperformSessions = evaluation.suggestedState.underperformSessions,
    passedTests = evaluation.suggestedState.passedTests,
    passedBandedTests = evaluation.suggestedState.passedBandedTests,
    changeBandUnlocked = evaluation.suggestedState.changeBandUnlocked,
)

private fun WorkoutWithPrescriptions.toDomain(): WorkoutSession = WorkoutSession(
    id = workout.id,
    exerciseType = workout.exerciseType,
    title = workout.title,
    weekIndex = workout.weekIndex,
    sessionIndex = workout.sessionIndex,
    scheduledDate = workout.scheduledDate,
    isTest = workout.isTest,
    level = workout.level,
    difficulty = workout.difficulty,
    prescriptions = prescriptions.sortedBy(SetPrescriptionEntity::setIndex).map {
        SetPrescription(
            setIndex = it.setIndex,
            targetReps = it.targetReps,
            targetSeconds = it.targetSeconds,
            restSeconds = it.restSeconds,
            note = it.note,
        )
    },
    techniqueTip = TechniqueTip(
        title = workout.techniqueTitle,
        body = workout.techniqueBody,
    ),
    transitionHint = workout.transitionHint,
    completed = workout.completed,
)

private fun WorkoutSession.toEntity(programId: Long): WorkoutEntity = WorkoutEntity(
    id = id,
    programId = programId,
    exerciseType = exerciseType,
    title = title,
    weekIndex = weekIndex,
    sessionIndex = sessionIndex,
    scheduledDate = scheduledDate,
    isTest = isTest,
    level = level,
    difficulty = difficulty,
    techniqueTitle = techniqueTip.title,
    techniqueBody = techniqueTip.body,
    transitionHint = transitionHint,
    completed = completed,
)

private fun SetPrescription.toEntity(workoutId: String): SetPrescriptionEntity = SetPrescriptionEntity(
    workoutId = workoutId,
    setIndex = setIndex,
    targetReps = targetReps,
    targetSeconds = targetSeconds,
    restSeconds = restSeconds,
    note = note,
)

private fun CompletedWorkoutWithResults.toSessionResult(): SessionResult {
    val workout = requireNotNull(workout)
    return SessionResult(
        workout = WorkoutSession(
            id = workout.id,
            exerciseType = workout.exerciseType,
            title = workout.title,
            weekIndex = workout.weekIndex,
            sessionIndex = workout.sessionIndex,
            scheduledDate = workout.scheduledDate,
            isTest = workout.isTest,
            level = workout.level,
            difficulty = workout.difficulty,
            prescriptions = prescriptions.sortedBy(SetPrescriptionEntity::setIndex).map {
                SetPrescription(
                    setIndex = it.setIndex,
                    targetReps = it.targetReps,
                    targetSeconds = it.targetSeconds,
                    restSeconds = it.restSeconds,
                    note = it.note,
                )
            },
            techniqueTip = TechniqueTip(workout.techniqueTitle, workout.techniqueBody),
            transitionHint = workout.transitionHint,
            completed = true,
        ),
        setResults = setResults.map {
            SetResult(
                setIndex = it.setIndex,
                actualReps = it.actualReps,
                actualSeconds = it.actualSeconds,
                completed = it.completed,
            )
        },
    )
}
