package com.vaslit.repflow.domain

import java.time.LocalDate
import java.util.UUID
import kotlin.math.max

interface PlannerEngine {
    fun buildInitialPlan(assessmentResult: AssessmentResult): ProgramPlan
    fun evaluateWorkout(
        sessionResult: SessionResult,
        recentHistory: List<SessionResult>,
        currentState: ProgramState,
    ): EvaluationSnapshot

    fun scheduleNextPhase(programState: ProgramState): ProgramPlan
}

class DefaultPlannerEngine(
    private val startDateProvider: () -> LocalDate = { LocalDate.now() },
) : PlannerEngine {

    override fun buildInitialPlan(assessmentResult: AssessmentResult): ProgramPlan {
        val checkpoints = listOf(2, 4, 6).map { week ->
            startDateProvider().plusDays(((week - 1) * 7 + 5).toLong())
        }
        val weeks = (1..6).map { week ->
            val sessions = (1..3).map { sessionIndex ->
                val isTest = sessionIndex == 3 && week % 2 == 0
                buildSession(
                    exerciseType = assessmentResult.exerciseType,
                    level = assessmentResult.level,
                    difficulty = progressionForWeek(assessmentResult.difficulty, week),
                    weekIndex = week,
                    sessionIndex = sessionIndex,
                    scheduledDate = startDateProvider().plusDays(((week - 1) * 7L) + (sessionIndex * 2L) - 2L),
                    isTest = isTest,
                )
            }
            PlanWeek(index = week, sessions = sessions)
        }
        return ProgramPlan(
            exerciseType = assessmentResult.exerciseType,
            startDate = startDateProvider(),
            weeks = weeks,
            checkpoints = checkpoints,
        )
    }

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
            ProgressionRecommendation.DELOAD -> "Есть недобор по объему. Следующая тренировка станет легче."
            ProgressionRecommendation.ADVANCE -> "Можно перейти к следующему этапу прогрессии."
            ProgressionRecommendation.CHANGE_BAND -> "Пора попробовать более легкую резинку на следующих тренировках."
            ProgressionRecommendation.TRY_STRICT -> "Тест пройден. Можно пробовать подтягивания без резинки."
        }

        return EvaluationSnapshot(
            successRate = successRate,
            recommendation = recommendation,
            suggestedState = suggestedState,
            summary = summary,
        )
    }

    override fun scheduleNextPhase(programState: ProgramState): ProgramPlan {
        return buildInitialPlan(
            AssessmentResult(
                exerciseType = programState.exerciseType,
                level = programState.currentLevel,
                difficulty = programState.currentDifficulty,
                metrics = emptyList(),
            ),
        )
    }

    private fun progressionForWeek(baseDifficulty: DifficultyLevel, week: Int): DifficultyLevel {
        return when {
            week >= 5 && baseDifficulty != DifficultyLevel.HARD -> DifficultyLevel.values()[baseDifficulty.ordinal + 1]
            else -> baseDifficulty
        }
    }

    private fun buildSession(
        exerciseType: ExerciseType,
        level: ProgressionLevel,
        difficulty: DifficultyLevel,
        weekIndex: Int,
        sessionIndex: Int,
        scheduledDate: LocalDate,
        isTest: Boolean,
    ): WorkoutSession {
        val prescriptions = if (isTest) {
            buildTestPrescriptions(level)
        } else {
            buildTrainingPrescriptions(level, difficulty, sessionIndex)
        }

        return WorkoutSession(
            id = UUID.randomUUID().toString(),
            exerciseType = exerciseType,
            title = if (isTest) "Контрольный тест" else "Тренировка $sessionIndex",
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

    private fun buildTrainingPrescriptions(
        level: ProgressionLevel,
        difficulty: DifficultyLevel,
        sessionIndex: Int,
    ): List<SetPrescription> {
        val baseTarget = when (level) {
            ProgressionLevel.SCAPULAR_HANG -> 15
            ProgressionLevel.NEGATIVE -> 3
            ProgressionLevel.BANDED -> 4
            ProgressionLevel.STRICT -> 3
            ProgressionLevel.WALL -> 10
            ProgressionLevel.INCLINE -> 8
            ProgressionLevel.KNEE -> 6
            ProgressionLevel.CLASSIC -> 5
        }
        val increment = when (difficulty) {
            DifficultyLevel.EASY -> 0
            DifficultyLevel.BASE -> 2
            DifficultyLevel.HARD -> 4
        }
        val sets = if (sessionIndex == 1) 4 else 5
        return (0 until sets).map { index ->
            SetPrescription(
                setIndex = index,
                targetReps = if (level in timedLevels) null else max(1, baseTarget + increment - (index / 2)),
                targetSeconds = if (level in timedLevels) baseTarget + increment + (index * 2) else null,
                restSeconds = when (level) {
                    ProgressionLevel.STRICT,
                    ProgressionLevel.BANDED,
                    ProgressionLevel.NEGATIVE -> 120
                    else -> 75
                },
                note = when (level) {
                    ProgressionLevel.SCAPULAR_HANG -> "Пауза в нижнем положении, лопатки вниз."
                    ProgressionLevel.NEGATIVE -> "Опускание 3-5 секунд."
                    else -> "Работай в полном диапазоне и без рывков."
                },
            )
        }
    }

    private fun buildTestPrescriptions(level: ProgressionLevel): List<SetPrescription> {
        return listOf(
            SetPrescription(
                setIndex = 0,
                targetReps = if (level in timedLevels) null else testTarget(level),
                targetSeconds = if (level in timedLevels) testTarget(level) else null,
                restSeconds = 150,
                note = "Один основной тестовый подход после разминки.",
            ),
            SetPrescription(
                setIndex = 1,
                targetReps = if (level in timedLevels) null else max(1, testTarget(level) - 2),
                targetSeconds = if (level in timedLevels) max(10, testTarget(level) - 5) else null,
                restSeconds = 120,
                note = "Контрольный подход на качество техники.",
            ),
        )
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

    private fun testTarget(level: ProgressionLevel): Int = when (level) {
        ProgressionLevel.SCAPULAR_HANG -> 20
        ProgressionLevel.NEGATIVE -> 4
        ProgressionLevel.BANDED -> 8
        ProgressionLevel.STRICT -> 5
        ProgressionLevel.WALL -> 18
        ProgressionLevel.INCLINE -> 15
        ProgressionLevel.KNEE -> 12
        ProgressionLevel.CLASSIC -> 10
    }

    private companion object {
        val timedLevels = setOf(ProgressionLevel.SCAPULAR_HANG)
    }
}
