package com.vaslit.repflow.data

import androidx.room.withTransaction
import com.vaslit.repflow.domain.AssessmentResult
import com.vaslit.repflow.domain.ControlMode
import com.vaslit.repflow.domain.DefaultPlannerEngine
import com.vaslit.repflow.domain.DifficultyLevel
import com.vaslit.repflow.domain.EvaluationSnapshot
import com.vaslit.repflow.domain.ExerciseType
import com.vaslit.repflow.domain.ExerciseVariant
import com.vaslit.repflow.domain.FinalGoal
import com.vaslit.repflow.domain.PlanWeek
import com.vaslit.repflow.domain.PlannerEngine
import com.vaslit.repflow.domain.ProgramCatalog
import com.vaslit.repflow.domain.ProgramPlan
import com.vaslit.repflow.domain.ProgramState
import com.vaslit.repflow.domain.ProgressionLevel
import com.vaslit.repflow.domain.SessionResult
import com.vaslit.repflow.domain.SetPrescription
import com.vaslit.repflow.domain.SetResult
import com.vaslit.repflow.domain.TechniqueTip
import com.vaslit.repflow.domain.TrainingCatalog
import com.vaslit.repflow.domain.WorkoutSession
import com.vaslit.repflow.domain.finalGoal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ProgramSummary(
    val definitionId: String,
    val title: String,
    val exerciseType: ExerciseType,
    val currentLevel: ProgressionLevel,
    val currentDifficulty: DifficultyLevel,
    val preferredDays: List<DayOfWeek>,
    val nextWorkoutDate: LocalDate?,
    val bestGoalScore: Int,
    val goalTarget: Int,
    val goalLabel: String,
    val maintenanceMode: Boolean,
)

data class HistoryEntry(
    val completedAt: Instant,
    val scheduledDate: LocalDate,
    val title: String,
    val level: ProgressionLevel,
    val difficulty: DifficultyLevel,
    val successRate: Double,
    val summary: String,
    val totalVolume: Int,
    val isTest: Boolean,
)

data class ProgramDetail(
    val summary: ProgramSummary,
    val workouts: List<WorkoutSession>,
    val history: List<HistoryEntry>,
    val techniqueTip: TechniqueTip,
)

enum class WorkoutCalendarStatus {
    PLANNED,
    COMPLETED_STRONG,
    COMPLETED_OK,
    MISSED,
}

data class WorkoutCalendarItem(
    val id: String,
    val programId: Long,
    val exerciseType: ExerciseType,
    val title: String,
    val scheduledDate: LocalDate,
    val phaseIndex: Int,
    val isTest: Boolean,
    val level: ProgressionLevel,
    val difficulty: DifficultyLevel,
    val status: WorkoutCalendarStatus,
    val successRate: Double? = null,
    val totalVolume: Int? = null,
)

data class DashboardProgram(
    val definitionId: String,
    val title: String,
    val exerciseType: ExerciseType,
    val currentLevel: ProgressionLevel,
    val currentDifficulty: DifficultyLevel,
    val preferredDays: List<DayOfWeek>,
    val nextWorkout: WorkoutCalendarItem?,
    val pendingCount: Int,
    val bestGoalScore: Int,
    val goalTarget: Int,
    val maintenanceMode: Boolean,
)

data class DashboardState(
    val today: LocalDate,
    val programs: List<DashboardProgram>,
    val todayItems: List<WorkoutCalendarItem>,
    val tomorrowItems: List<WorkoutCalendarItem>,
    val laterItems: List<WorkoutCalendarItem>,
)

data class AnalyticsEntry(
    val date: LocalDate,
    val totalVolume: Int,
    val successRate: Double,
    val isTest: Boolean,
    val testScore: Int?,
)

data class ProgramAnalytics(
    val exerciseType: ExerciseType,
    val entries: List<AnalyticsEntry>,
)

class AppRepository(
    private val database: AppDatabase,
    private val programDao: ProgramDao,
    private val workoutDao: WorkoutDao,
    private val resultDao: ResultDao,
    private val plannerEngine: PlannerEngine = DefaultPlannerEngine(),
) {

    fun observeProgramSummaries(): Flow<List<ProgramSummary>> =
        combine(
            programDao.observePrograms(),
            workoutDao.observeWorkoutEntities(),
        ) { programs, workouts ->
            programs.map { program ->
                val definition = ProgramCatalog.byId(program.definitionId)
                val nextWorkoutDate = workouts
                    .filter { it.programId == program.id && !it.completed }
                    .minByOrNull(WorkoutEntity::scheduledDate)
                    ?.scheduledDate
                ProgramSummary(
                    definitionId = program.definitionId,
                    title = definition.title,
                    exerciseType = program.exerciseType,
                    currentLevel = program.currentLevel,
                    currentDifficulty = program.currentDifficulty,
                    preferredDays = program.preferredDays(),
                    nextWorkoutDate = nextWorkoutDate,
                    bestGoalScore = program.bestGoalScore,
                    goalTarget = definition.goal.targetReps,
                    goalLabel = definition.goal.label,
                    maintenanceMode = program.maintenanceMode,
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeProgramDetail(exerciseType: ExerciseType): Flow<ProgramDetail?> =
        programDao.observeProgram(exerciseType).flatMapLatest { program ->
            if (program == null) {
                flowOf(null)
            } else {
                val definition = ProgramCatalog.byId(program.definitionId)
                combine(
                    workoutDao.observeWorkouts(program.id),
                    resultDao.observeCompletedWorkouts(program.id),
                ) { workouts, history ->
                    ProgramDetail(
                        summary = ProgramSummary(
                            definitionId = program.definitionId,
                            title = definition.title,
                            exerciseType = program.exerciseType,
                            currentLevel = program.currentLevel,
                            currentDifficulty = program.currentDifficulty,
                            preferredDays = program.preferredDays(),
                            nextWorkoutDate = workouts.firstOrNull { !it.workout.completed }?.workout?.scheduledDate,
                            bestGoalScore = program.bestGoalScore,
                            goalTarget = definition.goal.targetReps,
                            goalLabel = definition.goal.label,
                            maintenanceMode = program.maintenanceMode,
                        ),
                        workouts = workouts.map { it.toDomain() },
                        history = history.mapNotNull { item ->
                            val workout = item.workout ?: return@mapNotNull null
                            HistoryEntry(
                                completedAt = item.completion.completedAt,
                                scheduledDate = workout.scheduledDate,
                                title = workout.title,
                                level = workout.level,
                                difficulty = workout.difficulty,
                                successRate = item.completion.successRate,
                                summary = item.completion.summary,
                                totalVolume = item.totalVolume(),
                                isTest = workout.isTest,
                            )
                        },
                        techniqueTip = TrainingCatalog.techniqueTip(program.exerciseType, program.currentLevel),
                    )
                }
            }
        }

    fun observeDashboard(): Flow<DashboardState> =
        combine(
            programDao.observePrograms(),
            observeCalendarItems(),
        ) { programs, items ->
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val programCards = programs.map { program ->
                val definition = ProgramCatalog.byId(program.definitionId)
                val nextWorkout = items
                    .filter { it.programId == program.id && it.status == WorkoutCalendarStatus.PLANNED }
                    .minByOrNull(WorkoutCalendarItem::scheduledDate)
                DashboardProgram(
                    definitionId = program.definitionId,
                    title = definition.title,
                    exerciseType = program.exerciseType,
                    currentLevel = program.currentLevel,
                    currentDifficulty = program.currentDifficulty,
                    preferredDays = program.preferredDays(),
                    nextWorkout = nextWorkout,
                    pendingCount = items.count { it.programId == program.id && it.status == WorkoutCalendarStatus.PLANNED },
                    bestGoalScore = program.bestGoalScore,
                    goalTarget = definition.goal.targetReps,
                    maintenanceMode = program.maintenanceMode,
                )
            }.sortedWith(compareBy<DashboardProgram> { it.nextWorkout?.scheduledDate ?: LocalDate.MAX }.thenBy { it.exerciseType.name })

            DashboardState(
                today = today,
                programs = programCards,
                todayItems = items.filter { it.scheduledDate == today },
                tomorrowItems = items.filter { it.scheduledDate == tomorrow },
                laterItems = items.filter { it.scheduledDate > tomorrow }.sortedBy(WorkoutCalendarItem::scheduledDate),
            )
        }

    fun observeCalendarItems(): Flow<List<WorkoutCalendarItem>> =
        combine(
            programDao.observePrograms(),
            workoutDao.observeWorkoutEntities(),
            resultDao.observeAllCompletedWorkouts(),
        ) { programs, workouts, completions ->
            val programById = programs.associateBy(ProgramEntity::id)
            val completionByWorkoutId = completions.associateBy { it.completion.workoutId }
            val today = LocalDate.now()

            workouts.mapNotNull { workout ->
                val program = programById[workout.programId] ?: return@mapNotNull null
                val completion = completionByWorkoutId[workout.id]
                WorkoutCalendarItem(
                    id = workout.id,
                    programId = workout.programId,
                    exerciseType = program.exerciseType,
                    title = workout.title,
                    scheduledDate = workout.scheduledDate,
                    phaseIndex = workout.phaseIndex,
                    isTest = workout.isTest,
                    level = workout.level,
                    difficulty = workout.difficulty,
                    status = when {
                        completion != null && completion.completion.successRate >= 0.95 -> WorkoutCalendarStatus.COMPLETED_STRONG
                        completion != null -> WorkoutCalendarStatus.COMPLETED_OK
                        !workout.completed && workout.scheduledDate.isBefore(today) -> WorkoutCalendarStatus.MISSED
                        else -> WorkoutCalendarStatus.PLANNED
                    },
                    successRate = completion?.completion?.successRate,
                    totalVolume = completion?.totalVolume(),
                )
            }.sortedWith(compareBy<WorkoutCalendarItem> { it.scheduledDate }.thenBy { it.exerciseType.name }.thenBy { it.title })
        }

    fun observeAnalytics(exerciseType: ExerciseType): Flow<ProgramAnalytics?> =
        resultDao.observeAllCompletedWorkouts().map { history ->
            ProgramAnalytics(
                exerciseType = exerciseType,
                entries = history.mapNotNull { item ->
                    val workout = item.workout ?: return@mapNotNull null
                    if (workout.exerciseType != exerciseType) return@mapNotNull null
                    AnalyticsEntry(
                        date = item.completion.completedAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                        totalVolume = item.totalVolume(),
                        successRate = item.completion.successRate,
                        isTest = workout.isTest,
                        testScore = if (workout.isTest) item.primaryScore() else null,
                    )
                }.sortedBy(AnalyticsEntry::date),
            )
        }

    suspend fun createProgram(
        assessmentResult: AssessmentResult,
        preferredDays: List<DayOfWeek>,
    ) {
        database.withTransaction {
            val definition = ProgramCatalog.byExercise(assessmentResult.exerciseType)
            val existing = programDao.getProgram(assessmentResult.exerciseType)
            if (existing != null) {
                programDao.archiveProgramById(existing.id, Instant.now())
            }

            val phaseIndex = 1
            val initialGoalScore = assessmentResult.initialGoalScore()
            val maintenanceMode = initialGoalScore >= definition.goal.targetReps
            val programId = programDao.insertProgram(
                ProgramEntity(
                    definitionId = definition.id,
                    exerciseType = assessmentResult.exerciseType,
                    currentLevel = assessmentResult.level,
                    currentDifficulty = assessmentResult.difficulty,
                    preferredDays = preferredDays.encodeDays(),
                    activePhaseIndex = phaseIndex,
                    bestGoalScore = initialGoalScore,
                    maintenanceMode = maintenanceMode,
                    successfulSessions = 0,
                    underperformSessions = 0,
                    passedTests = 0,
                    passedBandedTests = 0,
                    changeBandUnlocked = false,
                    startedAt = LocalDate.now(),
                ),
            )
            insertPlan(
                programId = programId,
                plan = plannerEngine.buildInitialPlan(
                    assessmentResult = assessmentResult,
                    preferredDays = preferredDays,
                    phaseIndex = phaseIndex,
                    maintenanceMode = maintenanceMode,
                ),
            )
        }
    }

    suspend fun getWorkout(workoutId: String): WorkoutSession? =
        workoutDao.getWorkout(workoutId)?.toDomain()

    suspend fun completeWorkout(
        exerciseType: ExerciseType,
        workoutId: String,
        setResults: List<SetResult>,
    ): EvaluationSnapshot = database.withTransaction {
        val program = requireNotNull(programDao.getProgram(exerciseType))
        val definition = ProgramCatalog.byId(program.definitionId)
        val workoutWithPrescriptions = requireNotNull(workoutDao.getWorkout(workoutId))
        val sessionResult = SessionResult(
            workout = workoutWithPrescriptions.toDomain(),
            setResults = setResults,
        )
        val recentHistory = resultDao.recentCompletedWorkouts(program.id, 3).map { it.toSessionResult() }
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

        val goalProgress = updatedGoalProgress(
            current = program.bestGoalScore,
            workout = workoutWithPrescriptions.workout,
            setResults = setResults,
        )
        val rollingScore = definition.estimateRollingScore(recentHistory + sessionResult)
        val updatedProgram = program.applyEvaluation(evaluation).copy(
            bestGoalScore = maxOf(goalProgress, rollingScore.takeIf { program.currentLevel == definition.goal.targetLevel } ?: 0),
            maintenanceMode = program.maintenanceMode || goalProgress >= definition.goal.targetReps,
        )

        val shouldRegeneratePhase = when (definition.controlMode) {
            ControlMode.END_OF_PHASE -> workoutWithPrescriptions.workout.isTest
            ControlMode.NONE -> workoutDao.getPendingWorkoutEntities(program.id).none { !it.completed && it.id != workoutId }
        }

        if (shouldRegeneratePhase) {
            val nextPhaseIndex = program.activePhaseIndex + 1
            val programWithPhase = updatedProgram.copy(activePhaseIndex = nextPhaseIndex)
            programDao.updateProgram(programWithPhase)
            regenerateNextPhase(
                program = programWithPhase,
                startDate = workoutWithPrescriptions.workout.scheduledDate.plusDays(1),
                phaseIndex = nextPhaseIndex,
            )
        } else {
            programDao.updateProgram(updatedProgram)
        }

        evaluation
    }

    suspend fun moveWorkout(workoutId: String, newDate: LocalDate) {
        database.withTransaction {
            val workout = workoutDao.getWorkoutEntity(workoutId) ?: return@withTransaction
            if (workout.completed) return@withTransaction

            workoutDao.updateWorkoutScheduledDate(workoutId, newDate)

            if (!workout.isTest) {
                val program = programDao.getProgram(workout.exerciseType) ?: return@withTransaction
                val definition = ProgramCatalog.byId(program.definitionId)
                if (definition.controlMode == ControlMode.END_OF_PHASE) {
                    val phaseWorkouts = workoutDao.getWorkoutsForPhase(workout.programId, workout.phaseIndex)
                    val trainingDates = phaseWorkouts
                        .filter { !it.isTest }
                        .map { if (it.id == workoutId) newDate else it.scheduledDate }
                    val testWorkout = phaseWorkouts.firstOrNull { it.isTest && !it.completed }
                    if (testWorkout != null && trainingDates.isNotEmpty()) {
                        val desiredTestDate = nextAvailableTestDate(
                            latestTrainingDate = requireNotNull(trainingDates.maxOrNull()),
                            preferredDays = program.preferredDays(),
                        )
                        workoutDao.updateWorkoutScheduledDate(testWorkout.id, desiredTestDate)
                    }
                }
            }
        }
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

    private fun nextAvailableTestDate(
        latestTrainingDate: LocalDate,
        preferredDays: List<DayOfWeek>,
    ): LocalDate {
        var candidate = latestTrainingDate.plusDays(1)
        while (candidate.dayOfWeek in preferredDays) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private suspend fun insertPlan(
        programId: Long,
        plan: ProgramPlan,
    ) {
        val workouts = plan.weeks.flatMap(PlanWeek::sessions)
        workoutDao.insertWorkouts(workouts.map { it.toEntity(programId) })
        workoutDao.insertPrescriptions(
            workouts.flatMap { workout ->
                workout.prescriptions.map { it.toEntity(workout.id) }
            },
        )
    }

    private suspend fun regenerateNextPhase(
        program: ProgramEntity,
        startDate: LocalDate,
        phaseIndex: Int,
    ) {
        val pendingIds = workoutDao.getPendingWorkoutIds(program.id)
        if (pendingIds.isNotEmpty()) {
            workoutDao.deleteWorkoutsByIds(pendingIds)
        }
        insertPlan(
            programId = program.id,
            plan = plannerEngine.scheduleNextPhase(
                programState = program.toState(),
                preferredDays = program.preferredDays(),
                startDate = startDate,
                phaseIndex = phaseIndex,
            ),
        )
    }
}

private fun ProgramEntity.toState(): ProgramState = ProgramState(
    exerciseType = exerciseType,
    currentLevel = currentLevel,
    currentDifficulty = currentDifficulty,
    bestGoalScore = bestGoalScore,
    maintenanceMode = maintenanceMode,
    successfulSessions = successfulSessions,
    underperformSessions = underperformSessions,
    passedTests = passedTests,
    passedBandedTests = passedBandedTests,
    changeBandUnlocked = changeBandUnlocked,
)

private fun ProgramEntity.applyEvaluation(evaluation: EvaluationSnapshot): ProgramEntity = copy(
    currentLevel = evaluation.suggestedState.currentLevel,
    currentDifficulty = evaluation.suggestedState.currentDifficulty,
    bestGoalScore = evaluation.suggestedState.bestGoalScore,
    maintenanceMode = evaluation.suggestedState.maintenanceMode,
    successfulSessions = evaluation.suggestedState.successfulSessions,
    underperformSessions = evaluation.suggestedState.underperformSessions,
    passedTests = evaluation.suggestedState.passedTests,
    passedBandedTests = evaluation.suggestedState.passedBandedTests,
    changeBandUnlocked = evaluation.suggestedState.changeBandUnlocked,
)

private fun ProgramEntity.preferredDays(): List<DayOfWeek> =
    preferredDays.split(",")
        .mapNotNull { raw -> raw.takeIf(String::isNotBlank)?.let(DayOfWeek::valueOf) }
        .ifEmpty { ProgramCatalog.byId(definitionId).defaultPreferredDays }

private fun AssessmentResult.initialGoalScore(): Int = when (exerciseType) {
    ExerciseType.PULL_UP -> metrics.firstOrNull { it.key == "strict_reps" }?.value ?: 0
    ExerciseType.PUSH_UP -> metrics.firstOrNull { it.key == "classic_reps" }?.value ?: 0
}

private fun updatedGoalProgress(
    current: Int,
    workout: WorkoutEntity,
    setResults: List<SetResult>,
): Int {
    val goal: FinalGoal = workout.exerciseType.finalGoal()
    if (!workout.isTest || workout.level != goal.targetLevel) {
        return current
    }
    val bestObserved = setResults
        .minByOrNull(SetResult::setIndex)
        ?.let { it.actualReps ?: it.actualSeconds }
        ?: 0
    return maxOf(current, bestObserved)
}

private fun List<DayOfWeek>.encodeDays(): String =
    distinct()
        .sortedBy { it.value }
        .joinToString(",") { it.name }

private fun WorkoutWithPrescriptions.toDomain(): WorkoutSession = WorkoutSession(
    id = workout.id,
    exerciseType = workout.exerciseType,
    title = workout.title,
    phaseIndex = workout.phaseIndex,
    weekIndex = workout.weekIndex,
    sessionIndex = workout.sessionIndex,
    scheduledDate = workout.scheduledDate,
    isTest = workout.isTest,
    level = workout.level,
    difficulty = workout.difficulty,
    prescriptions = prescriptions.sortedBy(SetPrescriptionEntity::setIndex).map {
        SetPrescription(
            setIndex = it.setIndex,
            variant = it.variant(workout.level, workout.exerciseType),
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
    phaseIndex = phaseIndex,
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
    variantName = variant.name,
    targetReps = targetReps,
    targetSeconds = targetSeconds,
    restSeconds = restSeconds,
    note = note,
)

private fun CompletedWorkoutWithResults.totalVolume(): Int =
    setResults.sumOf { it.actualReps ?: it.actualSeconds ?: 0 }

private fun CompletedWorkoutWithResults.primaryScore(): Int? =
    setResults.minByOrNull(SetResultEntity::setIndex)?.let { it.actualReps ?: it.actualSeconds }

private fun CompletedWorkoutWithResults.toSessionResult(): SessionResult {
    val workout = requireNotNull(workout)
    return SessionResult(
        workout = WorkoutSession(
            id = workout.id,
            exerciseType = workout.exerciseType,
            title = workout.title,
            phaseIndex = workout.phaseIndex,
            weekIndex = workout.weekIndex,
            sessionIndex = workout.sessionIndex,
            scheduledDate = workout.scheduledDate,
            isTest = workout.isTest,
            level = workout.level,
            difficulty = workout.difficulty,
            prescriptions = prescriptions.sortedBy(SetPrescriptionEntity::setIndex).map {
                SetPrescription(
                    setIndex = it.setIndex,
                    variant = it.variant(workout.level, workout.exerciseType),
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

private fun SetPrescriptionEntity.variant(
    level: ProgressionLevel,
    exerciseType: ExerciseType,
): ExerciseVariant =
    variantName.takeIf(String::isNotBlank)?.let { raw ->
        runCatching { ExerciseVariant.valueOf(raw) }.getOrNull()
    } ?: ProgramCatalog.byExercise(exerciseType).fallbackVariant(level)
