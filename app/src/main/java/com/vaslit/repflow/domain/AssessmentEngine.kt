package com.vaslit.repflow.domain

object AssessmentEngine {

    fun assessPullUps(metrics: List<AssessmentMetric>): AssessmentResult {
        val strictReps = metrics.valueOf("strict_reps")
        val bandedReps = metrics.valueOf("banded_reps")
        val negativeReps = metrics.valueOf("negative_reps")
        val hangSeconds = metrics.valueOf("hang_seconds")

        val level = when {
            strictReps >= 3 -> ProgressionLevel.STRICT
            bandedReps >= 4 -> ProgressionLevel.BANDED
            negativeReps >= 3 -> ProgressionLevel.NEGATIVE
            else -> ProgressionLevel.SCAPULAR_HANG
        }

        val difficulty = when (level) {
            ProgressionLevel.STRICT -> when {
                strictReps >= 8 -> DifficultyLevel.HARD
                strictReps >= 5 -> DifficultyLevel.BASE
                else -> DifficultyLevel.EASY
            }

            ProgressionLevel.BANDED -> when {
                bandedReps >= 10 -> DifficultyLevel.HARD
                bandedReps >= 7 -> DifficultyLevel.BASE
                else -> DifficultyLevel.EASY
            }

            ProgressionLevel.NEGATIVE -> when {
                negativeReps >= 6 -> DifficultyLevel.HARD
                negativeReps >= 4 -> DifficultyLevel.BASE
                else -> DifficultyLevel.EASY
            }

            else -> when {
                hangSeconds >= 30 -> DifficultyLevel.HARD
                hangSeconds >= 20 -> DifficultyLevel.BASE
                else -> DifficultyLevel.EASY
            }
        }

        return AssessmentResult(
            exerciseType = ExerciseType.PULL_UP,
            level = level,
            difficulty = difficulty,
            metrics = metrics,
        )
    }

    fun assessPushUps(metrics: List<AssessmentMetric>): AssessmentResult {
        val classicReps = metrics.valueOf("classic_reps")
        val kneeReps = metrics.valueOf("knee_reps")
        val inclineReps = metrics.valueOf("incline_reps")
        val wallReps = metrics.valueOf("wall_reps")

        val level = when {
            classicReps >= 5 -> ProgressionLevel.CLASSIC
            kneeReps >= 8 -> ProgressionLevel.KNEE
            inclineReps >= 10 -> ProgressionLevel.INCLINE
            else -> ProgressionLevel.WALL
        }

        val difficulty = when (level) {
            ProgressionLevel.CLASSIC -> when {
                classicReps >= 20 -> DifficultyLevel.HARD
                classicReps >= 12 -> DifficultyLevel.BASE
                else -> DifficultyLevel.EASY
            }

            ProgressionLevel.KNEE -> when {
                kneeReps >= 20 -> DifficultyLevel.HARD
                kneeReps >= 14 -> DifficultyLevel.BASE
                else -> DifficultyLevel.EASY
            }

            ProgressionLevel.INCLINE -> when {
                inclineReps >= 22 -> DifficultyLevel.HARD
                inclineReps >= 15 -> DifficultyLevel.BASE
                else -> DifficultyLevel.EASY
            }

            else -> when {
                wallReps >= 25 -> DifficultyLevel.HARD
                wallReps >= 18 -> DifficultyLevel.BASE
                else -> DifficultyLevel.EASY
            }
        }

        return AssessmentResult(
            exerciseType = ExerciseType.PUSH_UP,
            level = level,
            difficulty = difficulty,
            metrics = metrics,
        )
    }
}

private fun List<AssessmentMetric>.valueOf(key: String): Int =
    firstOrNull { it.key == key }?.value ?: 0
