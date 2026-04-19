package com.vaslit.repflow.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssessmentEngineTest {

    @Test
    fun `pull-up assessment selects strict level when user can do strict reps`() {
        val result = AssessmentEngine.assessPullUps(
            listOf(
                AssessmentMetric("hang_seconds", "hang", 25),
                AssessmentMetric("negative_reps", "negative", 5),
                AssessmentMetric("banded_reps", "banded", 8),
                AssessmentMetric("strict_reps", "strict", 6),
            ),
        )

        assertThat(result.exerciseType).isEqualTo(ExerciseType.PULL_UP)
        assertThat(result.level).isEqualTo(ProgressionLevel.STRICT)
        assertThat(result.difficulty).isEqualTo(DifficultyLevel.BASE)
    }

    @Test
    fun `push-up assessment falls back to incline level for intermediate user`() {
        val result = AssessmentEngine.assessPushUps(
            listOf(
                AssessmentMetric("wall_reps", "wall", 22),
                AssessmentMetric("incline_reps", "incline", 16),
                AssessmentMetric("knee_reps", "knee", 6),
                AssessmentMetric("classic_reps", "classic", 2),
            ),
        )

        assertThat(result.exerciseType).isEqualTo(ExerciseType.PUSH_UP)
        assertThat(result.level).isEqualTo(ProgressionLevel.INCLINE)
        assertThat(result.difficulty).isEqualTo(DifficultyLevel.BASE)
    }
}
