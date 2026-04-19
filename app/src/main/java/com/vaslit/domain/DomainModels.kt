package com.vaslit.domain

import java.time.LocalDate

enum class ExerciseType {
    PULL_UP,
    PUSH_UP,
}

enum class ProgressionLevel {
    SCAPULAR_HANG,
    NEGATIVE,
    BANDED,
    STRICT,
    WALL,
    INCLINE,
    KNEE,
    CLASSIC,
}

enum class DifficultyLevel {
    EASY,
    BASE,
    HARD,
}

data class AssessmentMetric(
    val key: String,
    val label: String,
    val value: Int,
)

data class AssessmentResult(
    val exerciseType: ExerciseType,
    val level: ProgressionLevel,
    val difficulty: DifficultyLevel,
    val metrics: List<AssessmentMetric>,
)

data class ProgramPlan(
    val exerciseType: ExerciseType,
    val startDate: LocalDate,
    val weeks: List<PlanWeek>,
    val checkpoints: List<LocalDate>,
)

data class PlanWeek(
    val index: Int,
    val sessions: List<WorkoutSession>,
)

data class WorkoutSession(
    val id: String,
    val exerciseType: ExerciseType,
    val title: String,
    val weekIndex: Int,
    val sessionIndex: Int,
    val scheduledDate: LocalDate,
    val isTest: Boolean,
    val level: ProgressionLevel,
    val difficulty: DifficultyLevel,
    val prescriptions: List<SetPrescription>,
    val techniqueTip: TechniqueTip,
    val transitionHint: String,
    val completed: Boolean = false,
)

data class SetPrescription(
    val setIndex: Int,
    val targetReps: Int? = null,
    val targetSeconds: Int? = null,
    val restSeconds: Int,
    val note: String,
)

data class SetResult(
    val setIndex: Int,
    val actualReps: Int? = null,
    val actualSeconds: Int? = null,
    val completed: Boolean,
)

data class TechniqueTip(
    val title: String,
    val body: String,
)

enum class ProgressionRecommendation {
    KEEP,
    DELOAD,
    ADVANCE,
    CHANGE_BAND,
    TRY_STRICT,
}

data class SessionResult(
    val workout: WorkoutSession,
    val setResults: List<SetResult>,
)

data class ProgramState(
    val exerciseType: ExerciseType,
    val currentLevel: ProgressionLevel,
    val currentDifficulty: DifficultyLevel,
    val successfulSessions: Int = 0,
    val underperformSessions: Int = 0,
    val passedTests: Int = 0,
    val passedBandedTests: Int = 0,
    val changeBandUnlocked: Boolean = false,
)

data class EvaluationSnapshot(
    val successRate: Double,
    val recommendation: ProgressionRecommendation,
    val suggestedState: ProgramState,
    val summary: String,
)
