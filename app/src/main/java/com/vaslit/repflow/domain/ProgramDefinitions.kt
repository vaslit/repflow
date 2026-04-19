package com.vaslit.repflow.domain

import java.time.DayOfWeek
import kotlin.math.max
import kotlin.math.roundToInt

enum class ExerciseVariant(
    val exerciseType: ExerciseType,
    val title: String,
    val timed: Boolean = false,
) {
    PULL_UP_SCAPULAR_HANG(ExerciseType.PULL_UP, "Вис и лопатки", timed = true),
    PULL_UP_NEGATIVE(ExerciseType.PULL_UP, "Негативные подтягивания"),
    PULL_UP_BANDED(ExerciseType.PULL_UP, "Подтягивания с резинкой"),
    PULL_UP_STRICT(ExerciseType.PULL_UP, "Подтягивания без резинки"),
    PUSH_UP_WALL(ExerciseType.PUSH_UP, "Отжимания от стены"),
    PUSH_UP_INCLINE(ExerciseType.PUSH_UP, "Отжимания от опоры"),
    PUSH_UP_KNEE(ExerciseType.PUSH_UP, "Отжимания с колен"),
    PUSH_UP_CLASSIC(ExerciseType.PUSH_UP, "Классические отжимания"),
}

data class ExerciseBlockTemplate(
    val variant: ExerciseVariant,
    val sets: Int,
    val restSeconds: Int,
    val note: String,
    val repsResolver: ((ProgressionLevel, DifficultyLevel, Int) -> Int)? = null,
    val secondsResolver: ((ProgressionLevel, DifficultyLevel, Int) -> Int)? = null,
)

data class DayProgramTemplate(
    val id: String,
    val title: String,
    val blocks: List<ExerciseBlockTemplate>,
)

enum class ControlMode {
    NONE,
    END_OF_PHASE,
}

interface TrainingProgramDefinition {
    val id: String
    val title: String
    val exerciseType: ExerciseType
    val goal: FinalGoal
    val phaseLengthDays: Int
    val controlMode: ControlMode
    val defaultPreferredDays: List<DayOfWeek>
    val assessmentMetrics: List<AssessmentMetric>

    fun trainingTemplates(level: ProgressionLevel, maintenanceMode: Boolean): List<DayProgramTemplate>
    fun controlTemplate(level: ProgressionLevel): DayProgramTemplate?
    fun estimateInstantScore(sessionResult: SessionResult): Int
    fun estimateRollingScore(history: List<SessionResult>): Int
    fun fallbackVariant(level: ProgressionLevel): ExerciseVariant
}

object ProgramCatalog {
    private val definitions = listOf(
        PullUpProgramDefinition,
        PushUpProgramDefinition,
    )

    fun byExercise(exerciseType: ExerciseType): TrainingProgramDefinition =
        requireNotNull(definitions.firstOrNull { it.exerciseType == exerciseType }) {
            "No program definition for $exerciseType"
        }

    fun byId(id: String): TrainingProgramDefinition =
        requireNotNull(definitions.firstOrNull { it.id == id }) {
            "No program definition with id=$id"
        }
}

private object PullUpProgramDefinition : TrainingProgramDefinition {
    override val id: String = "pull_up_foundation_v1"
    override val title: String = "Подтягивания: сила и техника"
    override val exerciseType: ExerciseType = ExerciseType.PULL_UP
    override val goal: FinalGoal = exerciseType.finalGoal()
    override val phaseLengthDays: Int = 14
    override val controlMode: ControlMode = ControlMode.END_OF_PHASE
    override val defaultPreferredDays: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.FRIDAY,
    )
    override val assessmentMetrics: List<AssessmentMetric> = listOf(
        AssessmentMetric("hang_seconds", "Вис и активация лопаток, сек", 0),
        AssessmentMetric("negative_reps", "Негативные подтягивания, повторы", 0),
        AssessmentMetric("banded_reps", "Подтягивания с резинкой, повторы", 0),
        AssessmentMetric("strict_reps", "Подтягивания без резинки, повторы", 0),
    )

    override fun trainingTemplates(level: ProgressionLevel, maintenanceMode: Boolean): List<DayProgramTemplate> {
        if (maintenanceMode) {
            return listOf(
                DayProgramTemplate(
                    id = "pull-maint-a",
                    title = "Поддержка силы",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_STRICT,
                            sets = 4,
                            restSeconds = 150,
                            note = "Держи 2-3 повтора в запасе.",
                            repsResolver = { _, _, setIndex -> max(3, goal.targetReps / 2 - (setIndex / 2)) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_NEGATIVE,
                            sets = 2,
                            restSeconds = 120,
                            note = "Опускание 4-5 секунд.",
                            repsResolver = { _, _, _ -> 3 },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "pull-maint-b",
                    title = "Поддержка объема",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_STRICT,
                            sets = 5,
                            restSeconds = 120,
                            note = "Чистая техника, без отказа.",
                            repsResolver = { _, _, setIndex -> max(3, goal.targetReps / 2 - (setIndex / 3)) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_SCAPULAR_HANG,
                            sets = 2,
                            restSeconds = 75,
                            note = "Контроль плеч и нижней точки.",
                            secondsResolver = { _, _, _ -> 20 },
                        ),
                    ),
                ),
            )
        }

        return when (level) {
            ProgressionLevel.SCAPULAR_HANG -> listOf(
                DayProgramTemplate(
                    id = "pull-hang-a",
                    title = "Активация и вис",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_SCAPULAR_HANG,
                            sets = 4,
                            restSeconds = 75,
                            note = "Опусти лопатки и удерживай корпус собранным.",
                            secondsResolver = { _, difficulty, setIndex -> hangSeconds(difficulty) + (setIndex * 2) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_NEGATIVE,
                            sets = 3,
                            restSeconds = 120,
                            note = "Если можешь стартовать сверху, опускайся медленно.",
                            repsResolver = { _, difficulty, _ -> negativeReps(difficulty) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "pull-hang-b",
                    title = "Контроль корпуса",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_SCAPULAR_HANG,
                            sets = 5,
                            restSeconds = 75,
                            note = "Стабилизируй плечи и корпус.",
                            secondsResolver = { _, difficulty, setIndex -> max(10, hangSeconds(difficulty) - (setIndex / 2)) },
                        ),
                    ),
                ),
            )

            ProgressionLevel.NEGATIVE -> listOf(
                DayProgramTemplate(
                    id = "pull-negative-a",
                    title = "Негативы и контроль",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_NEGATIVE,
                            sets = 4,
                            restSeconds = 120,
                            note = "Опускание 4-5 секунд без рывка.",
                            repsResolver = { _, difficulty, setIndex -> max(2, negativeReps(difficulty) - (setIndex / 2)) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_SCAPULAR_HANG,
                            sets = 3,
                            restSeconds = 75,
                            note = "Фиксируй лопатки и дыхание.",
                            secondsResolver = { _, difficulty, _ -> hangSeconds(difficulty) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "pull-negative-b",
                    title = "Негативы объём",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_NEGATIVE,
                            sets = 5,
                            restSeconds = 120,
                            note = "Каждый повтор как отдельный качественный спуск.",
                            repsResolver = { _, difficulty, setIndex -> max(2, negativeReps(difficulty) + 1 - (setIndex / 2)) },
                        ),
                    ),
                ),
            )

            ProgressionLevel.BANDED -> listOf(
                DayProgramTemplate(
                    id = "pull-banded-a",
                    title = "Сила + негативы",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_BANDED,
                            sets = 4,
                            restSeconds = 120,
                            note = "Ровная траектория, полная амплитуда.",
                            repsResolver = { _, difficulty, setIndex -> basePullPrimary(difficulty, setIndex) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_NEGATIVE,
                            sets = 3,
                            restSeconds = 120,
                            note = "Добирать эксцентрическую силу после основного блока.",
                            repsResolver = { _, difficulty, _ -> max(2, negativeReps(difficulty) - 1) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "pull-banded-b",
                    title = "Объем + вис",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_BANDED,
                            sets = 5,
                            restSeconds = 120,
                            note = "Работай в чистой технике, не до отказа.",
                            repsResolver = { _, difficulty, setIndex -> max(3, basePullPrimary(difficulty, setIndex) + 1 - (setIndex / 3)) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_SCAPULAR_HANG,
                            sets = 2,
                            restSeconds = 75,
                            note = "Контроль плеч и нижней точки.",
                            secondsResolver = { _, difficulty, _ -> hangSeconds(difficulty) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "pull-banded-c",
                    title = "Плотность + негативы",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_BANDED,
                            sets = 6,
                            restSeconds = 105,
                            note = "Чаще подходы, но каждый субмаксимальный.",
                            repsResolver = { _, difficulty, setIndex -> max(3, basePullPrimary(difficulty, setIndex) - 1) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_NEGATIVE,
                            sets = 2,
                            restSeconds = 120,
                            note = "Спуск 5 секунд.",
                            repsResolver = { _, difficulty, _ -> max(2, negativeReps(difficulty) - 1) },
                        ),
                    ),
                ),
            )

            ProgressionLevel.STRICT -> listOf(
                DayProgramTemplate(
                    id = "pull-strict-a",
                    title = "Сила без резинки",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_STRICT,
                            sets = 4,
                            restSeconds = 150,
                            note = "Рабочие подходы на чистую силу.",
                            repsResolver = { _, difficulty, setIndex -> baseStrictPrimary(difficulty, setIndex) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_BANDED,
                            sets = 3,
                            restSeconds = 120,
                            note = "Добор объема после строгих повторов.",
                            repsResolver = { _, difficulty, setIndex -> max(4, basePullPrimary(difficulty, setIndex) + 1) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_NEGATIVE,
                            sets = 2,
                            restSeconds = 120,
                            note = "Удержать эксцентрику даже после усталости.",
                            repsResolver = { _, _, _ -> 2 },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "pull-strict-b",
                    title = "Плотность и техника",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_STRICT,
                            sets = 5,
                            restSeconds = 135,
                            note = "Субмаксимальные подходы, без срыва техники.",
                            repsResolver = { _, difficulty, setIndex -> max(2, baseStrictPrimary(difficulty, setIndex) - 1) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_SCAPULAR_HANG,
                            sets = 2,
                            restSeconds = 75,
                            note = "Контроль плечевого пояса.",
                            secondsResolver = { _, difficulty, _ -> hangSeconds(difficulty) + 5 },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "pull-strict-c",
                    title = "Объем и добор",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_STRICT,
                            sets = 6,
                            restSeconds = 135,
                            note = "Работа на общий объем без полного отказа.",
                            repsResolver = { _, difficulty, setIndex -> max(2, baseStrictPrimary(difficulty, setIndex) - (setIndex / 3)) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PULL_UP_BANDED,
                            sets = 2,
                            restSeconds = 120,
                            note = "Легкий добор объема резинкой.",
                            repsResolver = { _, difficulty, _ -> max(5, basePullPrimary(difficulty, 0) + 2) },
                        ),
                    ),
                ),
            )

            else -> emptyList()
        }
    }

    override fun controlTemplate(level: ProgressionLevel): DayProgramTemplate? = when (level) {
        ProgressionLevel.SCAPULAR_HANG -> DayProgramTemplate(
            id = "pull-control-hang",
            title = "Контрольный тест",
            blocks = listOf(
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PULL_UP_SCAPULAR_HANG,
                    sets = 1,
                    restSeconds = 150,
                    note = "Максимальный качественный вис после разминки.",
                    secondsResolver = { _, _, _ -> 20 },
                ),
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PULL_UP_NEGATIVE,
                    sets = 1,
                    restSeconds = 120,
                    note = "Контрольный негатив на качество.",
                    repsResolver = { _, _, _ -> 3 },
                ),
            ),
        )

        ProgressionLevel.NEGATIVE -> DayProgramTemplate(
            id = "pull-control-negative",
            title = "Контрольный тест",
            blocks = listOf(
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PULL_UP_NEGATIVE,
                    sets = 1,
                    restSeconds = 150,
                    note = "Сколько качественных негативов подряд без срыва.",
                    repsResolver = { _, _, _ -> 4 },
                ),
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PULL_UP_SCAPULAR_HANG,
                    sets = 1,
                    restSeconds = 75,
                    note = "Удержание активации.",
                    secondsResolver = { _, _, _ -> 20 },
                ),
            ),
        )

        ProgressionLevel.BANDED -> DayProgramTemplate(
            id = "pull-control-banded",
            title = "Контрольный тест",
            blocks = listOf(
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PULL_UP_BANDED,
                    sets = 1,
                    restSeconds = 150,
                    note = "Основной контрольный подход.",
                    repsResolver = { _, _, _ -> 8 },
                ),
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PULL_UP_NEGATIVE,
                    sets = 1,
                    restSeconds = 120,
                    note = "Проверка эксцентрики после теста.",
                    repsResolver = { _, _, _ -> 3 },
                ),
            ),
        )

        ProgressionLevel.STRICT -> DayProgramTemplate(
            id = "pull-control-strict",
            title = "Контрольный тест",
            blocks = listOf(
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PULL_UP_STRICT,
                    sets = 1,
                    restSeconds = 180,
                    note = "Основной контрольный подход без резинки.",
                    repsResolver = { _, _, _ -> 5 },
                ),
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PULL_UP_BANDED,
                    sets = 1,
                    restSeconds = 120,
                    note = "Технический back-off после основного теста.",
                    repsResolver = { _, _, _ -> 8 },
                ),
            ),
        )

        else -> null
    }

    override fun estimateInstantScore(sessionResult: SessionResult): Int {
        val weighted = sessionResult.workout.prescriptions.map { prescription ->
            val result = sessionResult.setResults.firstOrNull { it.setIndex == prescription.setIndex } ?: return@map 0.0
            val actual = result.actualReps ?: result.actualSeconds ?: 0
            when (prescription.variant) {
                ExerciseVariant.PULL_UP_STRICT -> actual.toDouble()
                ExerciseVariant.PULL_UP_BANDED -> actual * 0.75
                ExerciseVariant.PULL_UP_NEGATIVE -> actual * 0.6
                ExerciseVariant.PULL_UP_SCAPULAR_HANG -> actual / 4.0
                else -> 0.0
            }
        }.sum()
        return weighted.roundToInt()
    }

    override fun estimateRollingScore(history: List<SessionResult>): Int {
        if (history.isEmpty()) return 0
        val weightedScores = history.takeLast(4).mapIndexed { index, item ->
            val weight = index + 1
            estimateInstantScore(item) * weight
        }
        val weights = (1..weightedScores.size).sum()
        return (weightedScores.sum().toDouble() / weights.toDouble()).roundToInt()
    }

    override fun fallbackVariant(level: ProgressionLevel): ExerciseVariant = when (level) {
        ProgressionLevel.SCAPULAR_HANG -> ExerciseVariant.PULL_UP_SCAPULAR_HANG
        ProgressionLevel.NEGATIVE -> ExerciseVariant.PULL_UP_NEGATIVE
        ProgressionLevel.BANDED -> ExerciseVariant.PULL_UP_BANDED
        ProgressionLevel.STRICT -> ExerciseVariant.PULL_UP_STRICT
        else -> ExerciseVariant.PULL_UP_BANDED
    }
}

private object PushUpProgramDefinition : TrainingProgramDefinition {
    override val id: String = "push_up_foundation_v1"
    override val title: String = "Отжимания: сила и объем"
    override val exerciseType: ExerciseType = ExerciseType.PUSH_UP
    override val goal: FinalGoal = exerciseType.finalGoal()
    override val phaseLengthDays: Int = 14
    override val controlMode: ControlMode = ControlMode.END_OF_PHASE
    override val defaultPreferredDays: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.FRIDAY,
    )
    override val assessmentMetrics: List<AssessmentMetric> = listOf(
        AssessmentMetric("wall_reps", "Отжимания от стены, повторы", 0),
        AssessmentMetric("incline_reps", "Отжимания от высокой опоры, повторы", 0),
        AssessmentMetric("knee_reps", "Отжимания с колен, повторы", 0),
        AssessmentMetric("classic_reps", "Классические отжимания, повторы", 0),
    )

    override fun trainingTemplates(level: ProgressionLevel, maintenanceMode: Boolean): List<DayProgramTemplate> {
        if (maintenanceMode) {
            return listOf(
                DayProgramTemplate(
                    id = "push-maint-a",
                    title = "Поддержка силы",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_CLASSIC,
                            sets = 4,
                            restSeconds = 90,
                            note = "Не доводи до отказа, оставляй запас.",
                            repsResolver = { _, _, setIndex -> max(10, goal.targetReps / 2 - (setIndex / 2)) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_INCLINE,
                            sets = 2,
                            restSeconds = 75,
                            note = "Легкий добор объема с темпом 3-1-1.",
                            repsResolver = { _, _, _ -> 12 },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "push-maint-b",
                    title = "Поддержка объема",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_CLASSIC,
                            sets = 5,
                            restSeconds = 75,
                            note = "Плотный, но контролируемый объем.",
                            repsResolver = { _, _, setIndex -> max(10, goal.targetReps / 2 - (setIndex / 3)) },
                        ),
                    ),
                ),
            )
        }

        return when (level) {
            ProgressionLevel.WALL -> listOf(
                DayProgramTemplate(
                    id = "push-wall-a",
                    title = "Стена и контроль",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_WALL,
                            sets = 4,
                            restSeconds = 60,
                            note = "Полный диапазон без провала корпуса.",
                            repsResolver = { _, difficulty, setIndex -> wallBase(difficulty, setIndex) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "push-wall-b",
                    title = "Стена в темпе",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_WALL,
                            sets = 5,
                            restSeconds = 60,
                            note = "Темп 3-1-1, пауза внизу.",
                            repsResolver = { _, difficulty, setIndex -> max(8, wallBase(difficulty, setIndex) - 2) },
                        ),
                    ),
                ),
            )

            ProgressionLevel.INCLINE -> listOf(
                DayProgramTemplate(
                    id = "push-incline-a",
                    title = "Сила на опоре",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_INCLINE,
                            sets = 4,
                            restSeconds = 75,
                            note = "Основной объем на текущей высоте опоры.",
                            repsResolver = { _, difficulty, setIndex -> inclineBase(difficulty, setIndex) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_WALL,
                            sets = 2,
                            restSeconds = 60,
                            note = "Темповый добор, корпус жесткий.",
                            repsResolver = { _, difficulty, _ -> max(10, wallBase(difficulty, 0) - 2) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "push-incline-b",
                    title = "Объем на опоре",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_INCLINE,
                            sets = 5,
                            restSeconds = 75,
                            note = "Повторы не до отказа, но плотнее по отдыху.",
                            repsResolver = { _, difficulty, setIndex -> max(6, inclineBase(difficulty, setIndex) - 1) },
                        ),
                    ),
                ),
            )

            ProgressionLevel.KNEE -> listOf(
                DayProgramTemplate(
                    id = "push-knee-a",
                    title = "С колен и добор",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_KNEE,
                            sets = 4,
                            restSeconds = 75,
                            note = "Рабочие подходы на силу.",
                            repsResolver = { _, difficulty, setIndex -> kneeBase(difficulty, setIndex) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_INCLINE,
                            sets = 2,
                            restSeconds = 60,
                            note = "Легкий объем в полном диапазоне.",
                            repsResolver = { _, difficulty, _ -> max(8, inclineBase(difficulty, 0)) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "push-knee-b",
                    title = "Темп и объем",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_KNEE,
                            sets = 5,
                            restSeconds = 75,
                            note = "Темп 3-1-1, грудь низко.",
                            repsResolver = { _, difficulty, setIndex -> max(5, kneeBase(difficulty, setIndex) - 1) },
                        ),
                    ),
                ),
            )

            ProgressionLevel.CLASSIC -> listOf(
                DayProgramTemplate(
                    id = "push-classic-a",
                    title = "Сила классики",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_CLASSIC,
                            sets = 4,
                            restSeconds = 90,
                            note = "Главные рабочие подходы на классике.",
                            repsResolver = { _, difficulty, setIndex -> classicBase(difficulty, setIndex) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_KNEE,
                            sets = 2,
                            restSeconds = 60,
                            note = "Легкий объем после основной работы.",
                            repsResolver = { _, difficulty, _ -> max(8, kneeBase(difficulty, 0) + 2) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "push-classic-b",
                    title = "Объем и техника",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_CLASSIC,
                            sets = 5,
                            restSeconds = 75,
                            note = "Плотный объем без потери амплитуды.",
                            repsResolver = { _, difficulty, setIndex -> max(4, classicBase(difficulty, setIndex) - 1) },
                        ),
                    ),
                ),
                DayProgramTemplate(
                    id = "push-classic-c",
                    title = "Классика + добор",
                    blocks = listOf(
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_CLASSIC,
                            sets = 6,
                            restSeconds = 75,
                            note = "Подходы субмаксимальные, держи ровный темп.",
                            repsResolver = { _, difficulty, setIndex -> max(4, classicBase(difficulty, setIndex) - (setIndex / 3)) },
                        ),
                        ExerciseBlockTemplate(
                            variant = ExerciseVariant.PUSH_UP_INCLINE,
                            sets = 2,
                            restSeconds = 60,
                            note = "Пауза внизу на доборе.",
                            repsResolver = { _, difficulty, _ -> max(10, inclineBase(difficulty, 0) + 2) },
                        ),
                    ),
                ),
            )

            else -> emptyList()
        }
    }

    override fun controlTemplate(level: ProgressionLevel): DayProgramTemplate? = when (level) {
        ProgressionLevel.WALL -> DayProgramTemplate(
            id = "push-control-wall",
            title = "Контрольный тест",
            blocks = listOf(
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PUSH_UP_WALL,
                    sets = 1,
                    restSeconds = 120,
                    note = "Основной тестовый подход.",
                    repsResolver = { _, _, _ -> 18 },
                ),
            ),
        )

        ProgressionLevel.INCLINE -> DayProgramTemplate(
            id = "push-control-incline",
            title = "Контрольный тест",
            blocks = listOf(
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PUSH_UP_INCLINE,
                    sets = 1,
                    restSeconds = 120,
                    note = "Основной тестовый подход.",
                    repsResolver = { _, _, _ -> 15 },
                ),
            ),
        )

        ProgressionLevel.KNEE -> DayProgramTemplate(
            id = "push-control-knee",
            title = "Контрольный тест",
            blocks = listOf(
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PUSH_UP_KNEE,
                    sets = 1,
                    restSeconds = 120,
                    note = "Основной тестовый подход.",
                    repsResolver = { _, _, _ -> 12 },
                ),
            ),
        )

        ProgressionLevel.CLASSIC -> DayProgramTemplate(
            id = "push-control-classic",
            title = "Контрольный тест",
            blocks = listOf(
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PUSH_UP_CLASSIC,
                    sets = 1,
                    restSeconds = 150,
                    note = "Основной тестовый подход.",
                    repsResolver = { _, _, _ -> 10 },
                ),
                ExerciseBlockTemplate(
                    variant = ExerciseVariant.PUSH_UP_INCLINE,
                    sets = 1,
                    restSeconds = 90,
                    note = "Технический back-off после теста.",
                    repsResolver = { _, _, _ -> 12 },
                ),
            ),
        )

        else -> null
    }

    override fun estimateInstantScore(sessionResult: SessionResult): Int {
        val weighted = sessionResult.workout.prescriptions.map { prescription ->
            val result = sessionResult.setResults.firstOrNull { it.setIndex == prescription.setIndex } ?: return@map 0.0
            val actual = result.actualReps ?: result.actualSeconds ?: 0
            when (prescription.variant) {
                ExerciseVariant.PUSH_UP_CLASSIC -> actual.toDouble()
                ExerciseVariant.PUSH_UP_KNEE -> actual * 0.7
                ExerciseVariant.PUSH_UP_INCLINE -> actual * 0.55
                ExerciseVariant.PUSH_UP_WALL -> actual * 0.4
                else -> 0.0
            }
        }.sum()
        return weighted.roundToInt()
    }

    override fun estimateRollingScore(history: List<SessionResult>): Int {
        if (history.isEmpty()) return 0
        val recent = history.takeLast(4)
        val weightedScores = recent.mapIndexed { index, item ->
            val weight = index + 1
            estimateInstantScore(item) * weight
        }
        val weights = (1..recent.size).sum()
        return (weightedScores.sum().toDouble() / weights.toDouble()).roundToInt()
    }

    override fun fallbackVariant(level: ProgressionLevel): ExerciseVariant = when (level) {
        ProgressionLevel.WALL -> ExerciseVariant.PUSH_UP_WALL
        ProgressionLevel.INCLINE -> ExerciseVariant.PUSH_UP_INCLINE
        ProgressionLevel.KNEE -> ExerciseVariant.PUSH_UP_KNEE
        ProgressionLevel.CLASSIC -> ExerciseVariant.PUSH_UP_CLASSIC
        else -> ExerciseVariant.PUSH_UP_INCLINE
    }
}

private fun hangSeconds(difficulty: DifficultyLevel): Int = when (difficulty) {
    DifficultyLevel.EASY -> 12
    DifficultyLevel.BASE -> 18
    DifficultyLevel.HARD -> 25
}

private fun negativeReps(difficulty: DifficultyLevel): Int = when (difficulty) {
    DifficultyLevel.EASY -> 2
    DifficultyLevel.BASE -> 3
    DifficultyLevel.HARD -> 4
}

private fun basePullPrimary(difficulty: DifficultyLevel, setIndex: Int): Int {
    val base = when (difficulty) {
        DifficultyLevel.EASY -> 4
        DifficultyLevel.BASE -> 6
        DifficultyLevel.HARD -> 8
    }
    return max(3, base - (setIndex / 2))
}

private fun baseStrictPrimary(difficulty: DifficultyLevel, setIndex: Int): Int {
    val base = when (difficulty) {
        DifficultyLevel.EASY -> 3
        DifficultyLevel.BASE -> 5
        DifficultyLevel.HARD -> 7
    }
    return max(2, base - (setIndex / 2))
}

private fun wallBase(difficulty: DifficultyLevel, setIndex: Int): Int {
    val base = when (difficulty) {
        DifficultyLevel.EASY -> 10
        DifficultyLevel.BASE -> 14
        DifficultyLevel.HARD -> 18
    }
    return max(8, base - (setIndex / 2))
}

private fun inclineBase(difficulty: DifficultyLevel, setIndex: Int): Int {
    val base = when (difficulty) {
        DifficultyLevel.EASY -> 8
        DifficultyLevel.BASE -> 11
        DifficultyLevel.HARD -> 15
    }
    return max(6, base - (setIndex / 2))
}

private fun kneeBase(difficulty: DifficultyLevel, setIndex: Int): Int {
    val base = when (difficulty) {
        DifficultyLevel.EASY -> 6
        DifficultyLevel.BASE -> 9
        DifficultyLevel.HARD -> 13
    }
    return max(5, base - (setIndex / 2))
}

private fun classicBase(difficulty: DifficultyLevel, setIndex: Int): Int {
    val base = when (difficulty) {
        DifficultyLevel.EASY -> 5
        DifficultyLevel.BASE -> 8
        DifficultyLevel.HARD -> 12
    }
    return max(4, base - (setIndex / 2))
}
