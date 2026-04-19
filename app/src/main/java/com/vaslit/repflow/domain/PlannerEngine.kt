package com.vaslit.repflow.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max

interface PlannerEngine {
    fun buildInitialPlan(
        assessmentResult: AssessmentResult,
        preferredDays: List<DayOfWeek>,
        phaseIndex: Int,
        maintenanceMode: Boolean,
    ): ProgramPlan

    fun evaluateWorkout(
        sessionResult: SessionResult,
        recentHistory: List<SessionResult>,
        currentState: ProgramState,
    ): EvaluationSnapshot

    fun scheduleNextPhase(
        programState: ProgramState,
        preferredDays: List<DayOfWeek>,
        startDate: LocalDate,
        phaseIndex: Int,
    ): ProgramPlan
}

class DefaultPlannerEngine(
    private val startDateProvider: () -> LocalDate = { LocalDate.now() },
) : PlannerEngine {

    override fun buildInitialPlan(
        assessmentResult: AssessmentResult,
        preferredDays: List<DayOfWeek>,
        phaseIndex: Int,
        maintenanceMode: Boolean,
    ): ProgramPlan = buildPhasePlan(
        exerciseType = assessmentResult.exerciseType,
        level = assessmentResult.level,
        difficulty = assessmentResult.difficulty,
        preferredDays = preferredDays,
        phaseIndex = phaseIndex,
        phaseStart = startDateProvider(),
        maintenanceMode = maintenanceMode,
    )

    override fun evaluateWorkout(
        sessionResult: SessionResult,
        recentHistory: List<SessionResult>,
        currentState: ProgramState,
    ): EvaluationSnapshot {
        val successRate = successRate(sessionResult)
        val passedTest = sessionResult.workout.isTest && passesTest(sessionResult)
        val previousSuccess = recentHistory.takeLast(2).all { successRate(it) >= 0.9 }

        val recommendation = when {
            sessionResult.workout.level == ProgressionLevel.BANDED &&
                passedTest &&
                currentState.passedBandedTests >= 1 &&
                currentState.successfulSessions >= 2 -> ProgressionRecommendation.TRY_STRICT

            sessionResult.workout.level == ProgressionLevel.BANDED &&
                passedTest &&
                currentState.successfulSessions >= 2 -> ProgressionRecommendation.CHANGE_BAND

            sessionResult.workout.level == ProgressionLevel.BANDED &&
                passedTest -> ProgressionRecommendation.KEEP

            passedTest -> ProgressionRecommendation.ADVANCE
            successRate < 0.7 -> ProgressionRecommendation.DELOAD
            successRate >= 0.9 && previousSuccess -> ProgressionRecommendation.ADVANCE
            else -> ProgressionRecommendation.KEEP
        }

        val successfulSessions = when {
            successRate >= 0.9 -> currentState.successfulSessions + 1
            else -> 0
        }
        val underperformSessions = when {
            successRate < 0.7 -> currentState.underperformSessions + 1
            else -> 0
        }

        val suggestedState = currentState.copy(
            currentLevel = nextLevelIfNeeded(currentState, recommendation),
            currentDifficulty = nextDifficulty(currentState.currentDifficulty, recommendation),
            successfulSessions = successfulSessions,
            underperformSessions = underperformSessions,
            passedTests = currentState.passedTests + if (passedTest) 1 else 0,
            passedBandedTests = currentState.passedBandedTests + if (passedTest && currentState.currentLevel == ProgressionLevel.BANDED) 1 else 0,
            changeBandUnlocked = currentState.changeBandUnlocked || (
                recommendation == ProgressionRecommendation.CHANGE_BAND ||
                    (currentState.currentLevel == ProgressionLevel.BANDED && passedTest && successfulSessions >= 2)
                ),
        )

        val summary = when (recommendation) {
            ProgressionRecommendation.KEEP -> "План сохраняется без изменений. Держи технику и стабильность."
            ProgressionRecommendation.DELOAD -> "Есть недобор по объему. Следующая фаза станет легче."
            ProgressionRecommendation.ADVANCE -> "Можно перейти к следующему этапу прогрессии."
            ProgressionRecommendation.CHANGE_BAND -> "Пора попробовать более легкую резинку на следующей фазе."
            ProgressionRecommendation.TRY_STRICT -> "Тест пройден. Можно пробовать подтягивания без резинки."
        }

        return EvaluationSnapshot(
            successRate = successRate,
            recommendation = recommendation,
            suggestedState = suggestedState,
            summary = summary,
        )
    }

    override fun scheduleNextPhase(
        programState: ProgramState,
        preferredDays: List<DayOfWeek>,
        startDate: LocalDate,
        phaseIndex: Int,
    ): ProgramPlan = buildPhasePlan(
        exerciseType = programState.exerciseType,
        level = programState.currentLevel,
        difficulty = programState.currentDifficulty,
        preferredDays = preferredDays,
        phaseIndex = phaseIndex,
        phaseStart = startDate,
        maintenanceMode = programState.maintenanceMode,
    )

    private fun buildPhasePlan(
        exerciseType: ExerciseType,
        level: ProgressionLevel,
        difficulty: DifficultyLevel,
        preferredDays: List<DayOfWeek>,
        phaseIndex: Int,
        phaseStart: LocalDate,
        maintenanceMode: Boolean,
    ): ProgramPlan {
        val definition = ProgramCatalog.byExercise(exerciseType)
        if (maintenanceMode) {
            return buildMaintenancePlan(
                definition = definition,
                exerciseType = exerciseType,
                level = level,
                preferredDays = preferredDays,
                phaseIndex = phaseIndex,
                phaseStart = phaseStart,
            )
        }

        val normalizedDays = normalizePreferredDays(preferredDays)
        val trainingDates = generateTrainingDates(
            phaseStart = phaseStart,
            preferredDays = normalizedDays,
        )
        val sessionsByWeek = linkedMapOf(1 to mutableListOf<WorkoutSession>(), 2 to mutableListOf<WorkoutSession>())
        val templates = definition.trainingTemplates(level, maintenanceMode = false)

        val weeklyCounters = mutableMapOf(1 to 0, 2 to 0)
        trainingDates.forEachIndexed { index, date ->
            val weekIndex = if (date.isBefore(phaseStart.plusDays(7))) 1 else 2
            val sessionIndex = weeklyCounters.getValue(weekIndex) + 1
            weeklyCounters[weekIndex] = sessionIndex
            val template = templates[index % templates.size]
            sessionsByWeek.getValue(weekIndex) += buildSession(
                exerciseType = exerciseType,
                level = level,
                difficulty = difficulty,
                phaseIndex = phaseIndex,
                weekIndex = weekIndex,
                sessionIndex = sessionIndex,
                scheduledDate = date,
                template = template,
                isTest = false,
            )
        }

        val checkpoints = if (definition.controlMode == ControlMode.END_OF_PHASE) {
            val controlTemplate = definition.controlTemplate(level)
            if (controlTemplate != null && trainingDates.isNotEmpty()) {
                val testDate = nextNonPreferredDay(requireNotNull(trainingDates.maxOrNull()).plusDays(1), normalizedDays)
                val testWeekIndex = if (testDate.isBefore(phaseStart.plusDays(7))) 1 else 2
                val testSessionIndex = sessionsByWeek.getValue(testWeekIndex).size + 1
                sessionsByWeek.getValue(testWeekIndex) += buildSession(
                    exerciseType = exerciseType,
                    level = level,
                    difficulty = difficulty,
                    phaseIndex = phaseIndex,
                    weekIndex = testWeekIndex,
                    sessionIndex = testSessionIndex,
                    scheduledDate = testDate,
                    template = controlTemplate,
                    isTest = true,
                )
                listOf(testDate)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        return ProgramPlan(
            exerciseType = exerciseType,
            startDate = phaseStart,
            weeks = sessionsByWeek.entries.map { (week, sessions) ->
                PlanWeek(index = week, sessions = sessions.sortedBy(WorkoutSession::scheduledDate))
            },
            checkpoints = checkpoints,
        )
    }

    private fun buildMaintenancePlan(
        definition: TrainingProgramDefinition,
        exerciseType: ExerciseType,
        level: ProgressionLevel,
        preferredDays: List<DayOfWeek>,
        phaseIndex: Int,
        phaseStart: LocalDate,
    ): ProgramPlan {
        val normalizedDays = normalizePreferredDays(preferredDays)
        val weeklyDates = generateTrainingDates(
            phaseStart = phaseStart,
            preferredDays = normalizedDays,
        )
            .groupBy { if (it.isBefore(phaseStart.plusDays(7))) 1 else 2 }
            .mapValues { (_, dates) -> dates.sorted().take(2) }

        val templates = definition.trainingTemplates(level, maintenanceMode = true)
        val weeks = (1..2).map { weekIndex ->
            val sessions = weeklyDates[weekIndex].orEmpty().mapIndexed { index, date ->
                val template = templates[index % templates.size]
                buildSession(
                    exerciseType = exerciseType,
                    level = level,
                    difficulty = DifficultyLevel.EASY,
                    phaseIndex = phaseIndex,
                    weekIndex = weekIndex,
                    sessionIndex = index + 1,
                    scheduledDate = date,
                    template = template,
                    isTest = false,
                )
            }
            PlanWeek(index = weekIndex, sessions = sessions)
        }

        return ProgramPlan(
            exerciseType = exerciseType,
            startDate = phaseStart,
            weeks = weeks,
            checkpoints = emptyList(),
        )
    }

    private fun normalizePreferredDays(preferredDays: List<DayOfWeek>): List<DayOfWeek> {
        val normalized = preferredDays.distinct().sortedBy { it.value }
        return if (normalized.isEmpty()) {
            listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        } else {
            normalized
        }
    }

    private fun generateTrainingDates(
        phaseStart: LocalDate,
        preferredDays: List<DayOfWeek>,
    ): List<LocalDate> {
        val days = (0L..13L)
            .map { phaseStart.plusDays(it) }
            .filter { it.dayOfWeek in preferredDays }
        if (days.isNotEmpty()) {
            return days
        }

        return listOf(phaseStart, phaseStart.plusDays(2), phaseStart.plusDays(4), phaseStart.plusDays(7), phaseStart.plusDays(9), phaseStart.plusDays(11))
    }

    fun nextNonPreferredDay(
        candidate: LocalDate,
        preferredDays: List<DayOfWeek>,
    ): LocalDate {
        var date = candidate
        while (date.dayOfWeek in preferredDays) {
            date = date.plusDays(1)
        }
        return date
    }

    private fun buildSession(
        exerciseType: ExerciseType,
        level: ProgressionLevel,
        difficulty: DifficultyLevel,
        phaseIndex: Int,
        weekIndex: Int,
        sessionIndex: Int,
        scheduledDate: LocalDate,
        template: DayProgramTemplate,
        isTest: Boolean,
    ): WorkoutSession {
        val prescriptions = buildPrescriptionsFromTemplate(template, level, difficulty)

        return WorkoutSession(
            id = UUID.randomUUID().toString(),
            exerciseType = exerciseType,
            title = template.title,
            phaseIndex = phaseIndex,
            weekIndex = weekIndex,
            sessionIndex = sessionIndex,
            scheduledDate = scheduledDate,
            isTest = isTest,
            level = level,
            difficulty = difficulty,
            prescriptions = prescriptions,
            techniqueTip = TrainingCatalog.techniqueTip(exerciseType, level),
            transitionHint = TrainingCatalog.transitionHint(exerciseType, level),
            completed = false,
        )
    }

    private fun buildPrescriptionsFromTemplate(
        template: DayProgramTemplate,
        level: ProgressionLevel,
        difficulty: DifficultyLevel,
    ): List<SetPrescription> {
        var setIndex = 0
        return template.blocks.flatMap { block ->
            (0 until block.sets).map { localIndex ->
                SetPrescription(
                    setIndex = setIndex++,
                    variant = block.variant,
                    targetReps = block.repsResolver?.invoke(level, difficulty, localIndex),
                    targetSeconds = block.secondsResolver?.invoke(level, difficulty, localIndex),
                    restSeconds = block.restSeconds,
                    note = block.note,
                )
            }
        }
    }

    private fun nextLevelIfNeeded(
        state: ProgramState,
        recommendation: ProgressionRecommendation,
    ): ProgressionLevel {
        if (recommendation !in setOf(ProgressionRecommendation.ADVANCE, ProgressionRecommendation.TRY_STRICT)) {
            return state.currentLevel
        }
        return when (state.currentLevel) {
            ProgressionLevel.SCAPULAR_HANG -> ProgressionLevel.NEGATIVE
            ProgressionLevel.NEGATIVE -> ProgressionLevel.BANDED
            ProgressionLevel.BANDED -> ProgressionLevel.STRICT
            ProgressionLevel.WALL -> ProgressionLevel.INCLINE
            ProgressionLevel.INCLINE -> ProgressionLevel.KNEE
            ProgressionLevel.KNEE -> ProgressionLevel.CLASSIC
            else -> state.currentLevel
        }
    }

    private fun nextDifficulty(
        current: DifficultyLevel,
        recommendation: ProgressionRecommendation,
    ): DifficultyLevel = when (recommendation) {
        ProgressionRecommendation.DELOAD -> when (current) {
            DifficultyLevel.EASY -> DifficultyLevel.EASY
            DifficultyLevel.BASE -> DifficultyLevel.EASY
            DifficultyLevel.HARD -> DifficultyLevel.BASE
        }

        ProgressionRecommendation.ADVANCE,
        ProgressionRecommendation.CHANGE_BAND,
        ProgressionRecommendation.TRY_STRICT -> when (current) {
            DifficultyLevel.EASY -> DifficultyLevel.BASE
            DifficultyLevel.BASE -> DifficultyLevel.HARD
            DifficultyLevel.HARD -> DifficultyLevel.HARD
        }

        ProgressionRecommendation.KEEP -> current
    }

    private fun successRate(sessionResult: SessionResult): Double {
        val ratios = sessionResult.workout.prescriptions.map { prescription ->
            val result = sessionResult.setResults.firstOrNull { it.setIndex == prescription.setIndex } ?: return@map 0.0
            val target = prescription.targetReps ?: prescription.targetSeconds ?: 1
            val actual = result.actualReps ?: result.actualSeconds ?: 0
            (actual.toDouble() / target.toDouble()).coerceIn(0.0, 1.2)
        }
        return if (ratios.isEmpty()) 0.0 else ratios.average()
    }

    private fun passesTest(sessionResult: SessionResult): Boolean {
        val firstPrescription = sessionResult.workout.prescriptions.firstOrNull() ?: return false
        val firstResult = sessionResult.setResults.firstOrNull { it.setIndex == firstPrescription.setIndex } ?: return false
        val target = firstPrescription.targetReps ?: firstPrescription.targetSeconds ?: return false
        val actual = firstResult.actualReps ?: firstResult.actualSeconds ?: 0
        return actual >= target
    }

}
