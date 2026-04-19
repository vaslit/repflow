package com.vaslit.repflow.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vaslit.repflow.data.AnalyticsEntry
import com.vaslit.repflow.data.DashboardProgram
import com.vaslit.repflow.data.DashboardState
import com.vaslit.repflow.data.ProgramAnalytics
import com.vaslit.repflow.data.ProgramSummary
import com.vaslit.repflow.data.WorkoutCalendarItem
import com.vaslit.repflow.data.WorkoutCalendarStatus
import com.vaslit.repflow.domain.DifficultyLevel
import com.vaslit.repflow.domain.ExerciseType
import com.vaslit.repflow.domain.ProgressionLevel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RepFlowScreensTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun plansScreen_showsProgramAndAgendaSections() {
        val today = LocalDate.of(2026, 4, 19)
        val todayWorkout = plannedWorkout(
            id = "w-today",
            exerciseType = ExerciseType.PULL_UP,
            title = "Тренировка 1",
            date = today,
        )
        composeRule.setContent {
            RepFlowTheme {
                PlansScreen(
                    dashboard = DashboardState(
                        today = today,
                        programs = listOf(
                            DashboardProgram(
                                definitionId = "pull_up_foundation_v1",
                                title = "Подтягивания: сила и техника",
                                exerciseType = ExerciseType.PULL_UP,
                                currentLevel = ProgressionLevel.BANDED,
                                currentDifficulty = DifficultyLevel.BASE,
                                preferredDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                                nextWorkout = todayWorkout,
                                pendingCount = 3,
                                bestGoalScore = 4,
                                goalTarget = 10,
                                maintenanceMode = false,
                            ),
                        ),
                        todayItems = listOf(todayWorkout),
                        tomorrowItems = emptyList(),
                        laterItems = emptyList(),
                    ),
                    programs = listOf(
                        ProgramSummary(
                            definitionId = "pull_up_foundation_v1",
                            title = "Подтягивания: сила и техника",
                            exerciseType = ExerciseType.PULL_UP,
                            currentLevel = ProgressionLevel.BANDED,
                            currentDifficulty = DifficultyLevel.BASE,
                            preferredDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                            nextWorkoutDate = today,
                            bestGoalScore = 4,
                            goalTarget = 10,
                            goalLabel = "10 подтягиваний без резинки",
                            maintenanceMode = false,
                        ),
                    ),
                    onStartAssessment = {},
                    onOpenSession = {},
                )
            }
        }

        composeRule.onNodeWithText("RepFlow").assertIsDisplayed()
        composeRule.onNodeWithText("Подтягивания").assertIsDisplayed()
        composeRule.onNodeWithText("Сегодня").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Открыть тренировку").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calendarScreen_showsSelectedWorkoutAndMoveAction() {
        val today = LocalDate.now()
        composeRule.setContent {
            RepFlowTheme {
                CalendarScreen(
                    items = listOf(
                        plannedWorkout(
                            id = "calendar-item",
                            exerciseType = ExerciseType.PUSH_UP,
                            title = "Тренировка 2",
                            date = today,
                        ),
                    ),
                    onOpenSession = {},
                    onMoveWorkout = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Календарь").assertIsDisplayed()
        composeRule.onNodeWithText("Тренировка 2").assertIsDisplayed()
        composeRule.onNodeWithText("Перенести").assertIsDisplayed()
    }

    @Test
    fun statsScreen_switchesProgramAndShowsSummary() {
        val today = LocalDate.of(2026, 4, 19)
        val analyticsByType = mapOf(
            ExerciseType.PULL_UP to ProgramAnalytics(
                exerciseType = ExerciseType.PULL_UP,
                entries = listOf(
                    AnalyticsEntry(date = today.minusDays(2), totalVolume = 22, successRate = 0.92, isTest = false, testScore = null),
                    AnalyticsEntry(date = today.minusDays(1), totalVolume = 8, successRate = 1.0, isTest = true, testScore = 8),
                ),
            ),
            ExerciseType.PUSH_UP to ProgramAnalytics(
                exerciseType = ExerciseType.PUSH_UP,
                entries = listOf(
                    AnalyticsEntry(date = today.minusDays(3), totalVolume = 40, successRate = 0.88, isTest = false, testScore = null),
                ),
            ),
        )

        composeRule.setContent {
            RepFlowTheme {
                StatsScreen(
                    programs = listOf(
                        ProgramSummary(
                            definitionId = "pull_up_foundation_v1",
                            title = "Подтягивания: сила и техника",
                            exerciseType = ExerciseType.PULL_UP,
                            currentLevel = ProgressionLevel.BANDED,
                            currentDifficulty = DifficultyLevel.BASE,
                            preferredDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                            nextWorkoutDate = today,
                            bestGoalScore = 4,
                            goalTarget = 10,
                            goalLabel = "10 подтягиваний без резинки",
                            maintenanceMode = false,
                        ),
                        ProgramSummary(
                            definitionId = "push_up_foundation_v1",
                            title = "Отжимания: сила и объем",
                            exerciseType = ExerciseType.PUSH_UP,
                            currentLevel = ProgressionLevel.CLASSIC,
                            currentDifficulty = DifficultyLevel.BASE,
                            preferredDays = listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                            nextWorkoutDate = today.plusDays(1),
                            bestGoalScore = 18,
                            goalTarget = 30,
                            goalLabel = "30 классических отжиманий",
                            maintenanceMode = false,
                        ),
                    ),
                    observeAnalytics = { type -> flowOf(analyticsByType[type]) },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Сводка").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Сводка").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Отжимания").performClick()
        composeRule.onNodeWithText("Объем по тренировкам").performScrollTo().assertIsDisplayed()
    }

    private fun plannedWorkout(
        id: String,
        exerciseType: ExerciseType,
        title: String,
        date: LocalDate,
    ): WorkoutCalendarItem = WorkoutCalendarItem(
        id = id,
        programId = 1,
        exerciseType = exerciseType,
        title = title,
        scheduledDate = date,
        phaseIndex = 1,
        isTest = false,
        level = if (exerciseType == ExerciseType.PULL_UP) ProgressionLevel.BANDED else ProgressionLevel.CLASSIC,
        difficulty = DifficultyLevel.BASE,
        status = WorkoutCalendarStatus.PLANNED,
        successRate = null,
        totalVolume = null,
    )
}
