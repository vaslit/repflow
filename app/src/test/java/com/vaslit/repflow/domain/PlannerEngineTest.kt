package com.vaslit.repflow.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PlannerEngineTest {

    private val fixedDate = LocalDate.of(2026, 4, 19)
    private val engine = DefaultPlannerEngine(startDateProvider = { fixedDate })

    @Test
    fun `build initial plan creates two week phase with control test outside preferred days`() {
        val plan = engine.buildInitialPlan(
            AssessmentResult(
                exerciseType = ExerciseType.PULL_UP,
                level = ProgressionLevel.NEGATIVE,
                difficulty = DifficultyLevel.BASE,
                metrics = emptyList(),
            ),
            preferredDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            phaseIndex = 1,
            maintenanceMode = false,
        )

        assertThat(plan.weeks).hasSize(2)
        assertThat(plan.weeks.flatMap { it.sessions }).hasSize(7)
        assertThat(plan.checkpoints).containsExactly(fixedDate.plusDays(13))
        assertThat(plan.weeks.flatMap { it.sessions }.count { it.isTest }).isEqualTo(1)
        assertThat(plan.weeks.flatMap { it.sessions }.last().scheduledDate.dayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
    }

    @Test
    fun `underperformance triggers deload`() {
        val workout = workoutSession(
            level = ProgressionLevel.CLASSIC,
            difficulty = DifficultyLevel.BASE,
            prescriptions = listOf(
                SetPrescription(0, variant = ExerciseVariant.PUSH_UP_CLASSIC, targetReps = 10, restSeconds = 60, note = ""),
                SetPrescription(1, variant = ExerciseVariant.PUSH_UP_CLASSIC, targetReps = 10, restSeconds = 60, note = ""),
            ),
            isTest = false,
        )
        val evaluation = engine.evaluateWorkout(
            sessionResult = SessionResult(
                workout = workout,
                setResults = listOf(
                    SetResult(0, actualReps = 4, completed = false),
                    SetResult(1, actualReps = 5, completed = false),
                ),
            ),
            recentHistory = emptyList(),
            currentState = ProgramState(
                exerciseType = ExerciseType.PUSH_UP,
                currentLevel = ProgressionLevel.CLASSIC,
                currentDifficulty = DifficultyLevel.BASE,
            ),
        )

        assertThat(evaluation.recommendation).isEqualTo(ProgressionRecommendation.DELOAD)
        assertThat(evaluation.suggestedState.currentDifficulty).isEqualTo(DifficultyLevel.EASY)
    }

    @Test
    fun `banded test with stable streak recommends lighter band first`() {
        val testWorkout = workoutSession(
            level = ProgressionLevel.BANDED,
            difficulty = DifficultyLevel.BASE,
            prescriptions = listOf(
                SetPrescription(0, variant = ExerciseVariant.PULL_UP_BANDED, targetReps = 8, restSeconds = 120, note = ""),
                SetPrescription(1, variant = ExerciseVariant.PULL_UP_NEGATIVE, targetReps = 6, restSeconds = 120, note = ""),
            ),
            isTest = true,
        )
        val evaluation = engine.evaluateWorkout(
            sessionResult = SessionResult(
                workout = testWorkout,
                setResults = listOf(
                    SetResult(0, actualReps = 8, completed = true),
                    SetResult(1, actualReps = 6, completed = true),
                ),
            ),
            recentHistory = listOf(successSession(), successSession()),
            currentState = ProgramState(
                exerciseType = ExerciseType.PULL_UP,
                currentLevel = ProgressionLevel.BANDED,
                currentDifficulty = DifficultyLevel.BASE,
                successfulSessions = 2,
            ),
        )

        assertThat(evaluation.recommendation).isEqualTo(ProgressionRecommendation.CHANGE_BAND)
        assertThat(evaluation.suggestedState.changeBandUnlocked).isTrue()
    }

    @Test
    fun `second successful banded test allows trying strict pull-ups`() {
        val testWorkout = workoutSession(
            level = ProgressionLevel.BANDED,
            difficulty = DifficultyLevel.HARD,
            prescriptions = listOf(SetPrescription(0, variant = ExerciseVariant.PULL_UP_BANDED, targetReps = 8, restSeconds = 120, note = "")),
            isTest = true,
        )
        val evaluation = engine.evaluateWorkout(
            sessionResult = SessionResult(
                workout = testWorkout,
                setResults = listOf(SetResult(0, actualReps = 9, completed = true)),
            ),
            recentHistory = listOf(successSession(), successSession()),
            currentState = ProgramState(
                exerciseType = ExerciseType.PULL_UP,
                currentLevel = ProgressionLevel.BANDED,
                currentDifficulty = DifficultyLevel.HARD,
                successfulSessions = 2,
                passedBandedTests = 1,
            ),
        )

        assertThat(evaluation.recommendation).isEqualTo(ProgressionRecommendation.TRY_STRICT)
        assertThat(evaluation.suggestedState.currentLevel).isEqualTo(ProgressionLevel.STRICT)
    }

    @Test
    fun `maintenance mode schedules support phase without checkpoints`() {
        val plan = engine.scheduleNextPhase(
            programState = ProgramState(
                exerciseType = ExerciseType.PULL_UP,
                currentLevel = ProgressionLevel.STRICT,
                currentDifficulty = DifficultyLevel.HARD,
                bestGoalScore = 10,
                maintenanceMode = true,
            ),
            preferredDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            startDate = fixedDate,
            phaseIndex = 3,
        )

        assertThat(plan.checkpoints).isEmpty()
        assertThat(plan.weeks.flatMap { it.sessions }).isNotEmpty()
        assertThat(plan.weeks.flatMap { it.sessions }.all { !it.isTest }).isTrue()
        assertThat(plan.weeks.flatMap { it.sessions }.all { it.level == ProgressionLevel.STRICT }).isTrue()
    }

    private fun successSession(): SessionResult {
        val workout = workoutSession(
            level = ProgressionLevel.BANDED,
            difficulty = DifficultyLevel.BASE,
            prescriptions = listOf(SetPrescription(0, variant = ExerciseVariant.PULL_UP_BANDED, targetReps = 6, restSeconds = 120, note = "")),
            isTest = false,
        )
        return SessionResult(
            workout = workout,
            setResults = listOf(SetResult(0, actualReps = 6, completed = true)),
        )
    }

    private fun workoutSession(
        level: ProgressionLevel,
        difficulty: DifficultyLevel,
        prescriptions: List<SetPrescription>,
        isTest: Boolean,
    ): WorkoutSession = WorkoutSession(
        id = "session-$level-$difficulty-$isTest",
        exerciseType = if (level in setOf(
                ProgressionLevel.SCAPULAR_HANG,
                ProgressionLevel.NEGATIVE,
                ProgressionLevel.BANDED,
                ProgressionLevel.STRICT,
            )
        ) {
            ExerciseType.PULL_UP
        } else {
            ExerciseType.PUSH_UP
        },
        title = "Test",
        phaseIndex = 1,
        weekIndex = 1,
        sessionIndex = 1,
        scheduledDate = fixedDate,
        isTest = isTest,
        level = level,
        difficulty = difficulty,
        prescriptions = prescriptions,
        techniqueTip = TechniqueTip("tip", "body"),
        transitionHint = "hint",
    )
}
