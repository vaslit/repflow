package com.vaslit.domain

object TrainingCatalog {
    fun defaultAssessmentMetrics(type: ExerciseType): List<AssessmentMetric> = when (type) {
        ExerciseType.PULL_UP -> listOf(
            AssessmentMetric("hang_seconds", "Вис и активация лопаток, сек", 0),
            AssessmentMetric("negative_reps", "Негативные подтягивания, повторы", 0),
            AssessmentMetric("banded_reps", "Подтягивания с резинкой, повторы", 0),
            AssessmentMetric("strict_reps", "Подтягивания без резинки, повторы", 0),
        )

        ExerciseType.PUSH_UP -> listOf(
            AssessmentMetric("wall_reps", "Отжимания от стены, повторы", 0),
            AssessmentMetric("incline_reps", "Отжимания от высокой опоры, повторы", 0),
            AssessmentMetric("knee_reps", "Отжимания с колен, повторы", 0),
            AssessmentMetric("classic_reps", "Классические отжимания, повторы", 0),
        )
    }

    fun techniqueTip(type: ExerciseType, level: ProgressionLevel): TechniqueTip = when (type) {
        ExerciseType.PULL_UP -> when (level) {
            ProgressionLevel.SCAPULAR_HANG -> TechniqueTip(
                title = "Контроль корпуса",
                body = "Держи ребра собранными, не висни на пассивных плечах, сначала опусти лопатки вниз.",
            )

            ProgressionLevel.NEGATIVE -> TechniqueTip(
                title = "Медленное опускание",
                body = "Стартуй сверху и опускайся 3-5 секунд, не проваливай плечи и не бросай нижнюю точку.",
            )

            ProgressionLevel.BANDED -> TechniqueTip(
                title = "Работа с резинкой",
                body = "Сохраняй одинаковую траекторию, не раскачивайся. Меняй резинку только после успешного теста.",
            )

            ProgressionLevel.STRICT -> TechniqueTip(
                title = "Чистые повторы",
                body = "Полный вис внизу, подбородок выше перекладины вверху, без рывков ногами.",
            )

            else -> TechniqueTip("Техника", "Следи за контролем лопаток и темпом.")
        }

        ExerciseType.PUSH_UP -> when (level) {
            ProgressionLevel.WALL -> TechniqueTip(
                title = "Линия корпуса",
                body = "Сохраняй прямую линию от головы до пяток и не проваливай поясницу.",
            )

            ProgressionLevel.INCLINE -> TechniqueTip(
                title = "Высота опоры",
                body = "Опора должна позволять полный диапазон движения без боли и перекоса корпуса.",
            )

            ProgressionLevel.KNEE -> TechniqueTip(
                title = "Корпус единым блоком",
                body = "От колен до плеч одна линия, локти уходят назад под умеренным углом.",
            )

            ProgressionLevel.CLASSIC -> TechniqueTip(
                title = "Полная амплитуда",
                body = "Грудь опускается низко, корпус жесткий, без кивания головой и без неполных повторов.",
            )

            else -> TechniqueTip("Техника", "Контролируй локти и корпус.")
        }
    }

    fun transitionHint(type: ExerciseType, level: ProgressionLevel): String = when (type) {
        ExerciseType.PULL_UP -> when (level) {
            ProgressionLevel.SCAPULAR_HANG -> "Переход к негативам после уверенного виса 20+ сек и чистой активации лопаток."
            ProgressionLevel.NEGATIVE -> "Переход к резинке после 4+ медленных негативов без срыва."
            ProgressionLevel.BANDED -> "Ослабляй резинку только после успешного теста и двух стабильных тренировок подряд."
            ProgressionLevel.STRICT -> "Усложняйся только при 2 успешных тренировках подряд без недобора."
            else -> ""
        }

        ExerciseType.PUSH_UP -> when (level) {
            ProgressionLevel.WALL -> "Переходи ниже по опоре, когда легко делаешь все подходы в полном диапазоне."
            ProgressionLevel.INCLINE -> "Переход к коленям после уверенного контроля на текущей высоте опоры."
            ProgressionLevel.KNEE -> "Переход к классике после успешного теста и 2 чистых тренировок подряд."
            ProgressionLevel.CLASSIC -> "Усложняй объем только если техника остается стабильной во всех подходах."
            else -> ""
        }
    }
}
