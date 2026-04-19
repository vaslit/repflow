package com.vaslit.repflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaslit.repflow.data.ProgramDetail
import com.vaslit.repflow.data.ProgramSummary
import com.vaslit.repflow.domain.EvaluationSnapshot
import com.vaslit.repflow.domain.ExerciseType
import com.vaslit.repflow.domain.ProgressionLevel
import com.vaslit.repflow.domain.SetResult
import com.vaslit.repflow.domain.WorkoutSession
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

@Composable
fun RepFlowTheme(content: @Composable () -> Unit) {
    val scheme = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF7A3E12),
        onPrimary = Color(0xFFFFF7F1),
        secondary = Color(0xFF49635A),
        onSecondary = Color(0xFFF6FBF7),
        tertiary = Color(0xFFA2462D),
        background = Color(0xFFF7F2EA),
        surface = Color(0xFFFFFCF7),
        surfaceVariant = Color(0xFFE9DDD0),
        onSurface = Color(0xFF261A14),
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}

@Composable
fun RepFlowApp(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val programs by viewModel.programSummaries.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeScreen(
                programs = programs,
                onStartAssessment = { navController.navigate("assessment/${it.name}") },
                onOpenPlan = { navController.navigate("plan/${it.name}") },
            )
        }
        composable(
            route = "assessment/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { backStackEntry ->
            val exerciseType = ExerciseType.valueOf(requireNotNull(backStackEntry.arguments?.getString("type")))
            AssessmentScreen(
                exerciseType = exerciseType,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCreated = {
                    navController.navigate("plan/${exerciseType.name}") {
                        popUpTo("home")
                    }
                },
            )
        }
        composable(
            route = "plan/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { backStackEntry ->
            val exerciseType = ExerciseType.valueOf(requireNotNull(backStackEntry.arguments?.getString("type")))
            val detail by viewModel.observeProgramDetail(exerciseType).collectAsState(initial = null)
            ProgramScreen(
                exerciseType = exerciseType,
                detail = detail,
                onBack = { navController.popBackStack() },
                onOpenSession = { workoutId -> navController.navigate("session/${exerciseType.name}/$workoutId") },
                onOpenHistory = { navController.navigate("history/${exerciseType.name}") },
                onOpenTechnique = { navController.navigate("technique/${exerciseType.name}") },
            )
        }
        composable(
            route = "session/{type}/{workoutId}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("workoutId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val exerciseType = ExerciseType.valueOf(requireNotNull(backStackEntry.arguments?.getString("type")))
            val workoutId = requireNotNull(backStackEntry.arguments?.getString("workoutId"))
            SessionScreen(
                exerciseType = exerciseType,
                workoutId = workoutId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onFinished = { navController.navigate("summary/${exerciseType.name}") },
            )
        }
        composable(
            route = "summary/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { backStackEntry ->
            val exerciseType = ExerciseType.valueOf(requireNotNull(backStackEntry.arguments?.getString("type")))
            val evaluation by viewModel.lastEvaluation.collectAsState()
            CompletionScreen(
                exerciseType = exerciseType,
                evaluation = evaluation,
                onDone = {
                    navController.navigate("plan/${exerciseType.name}") {
                        popUpTo("home")
                    }
                },
            )
        }
        composable(
            route = "history/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { backStackEntry ->
            val exerciseType = ExerciseType.valueOf(requireNotNull(backStackEntry.arguments?.getString("type")))
            val detail by viewModel.observeProgramDetail(exerciseType).collectAsState(initial = null)
            HistoryScreen(
                exerciseType = exerciseType,
                detail = detail,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "technique/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { backStackEntry ->
            val exerciseType = ExerciseType.valueOf(requireNotNull(backStackEntry.arguments?.getString("type")))
            TechniqueScreen(
                exerciseType = exerciseType,
                guide = viewModel.techniqueGuide(exerciseType),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    programs: List<ProgramSummary>,
    onStartAssessment: (ExerciseType) -> Unit,
    onOpenPlan: (ExerciseType) -> Unit,
) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFFEFDCC7), Color(0xFFF7F2EA)))
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Тренер прогрессии") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Подтягивания и отжимания с тестами, паузами и адаптивным планом на 6 недель.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            exerciseCards.forEach { type ->
                val summary = programs.firstOrNull { it.exerciseType == type }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(type.title(), style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (summary == null) {
                                "Пройди первичный тест, чтобы получить стартовый уровень, паузы и план."
                            } else {
                                "Текущий уровень: ${summary.currentLevel.title(type)}. Сложность: ${summary.currentDifficulty.title()}."
                            },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (summary == null) {
                                Button(onClick = { onStartAssessment(type) }) {
                                    Text("Пройти тест")
                                }
                            } else {
                                Button(onClick = { onOpenPlan(type) }) {
                                    Text("Открыть план")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssessmentScreen(
    exerciseType: ExerciseType,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    val metrics = remember(exerciseType) { viewModel.defaultMetrics(exerciseType) }
    val values = remember(exerciseType) {
        mutableStateMapOf<String, String>().apply {
            putAll(metrics.associate { it.key to "0" })
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Стартовый тест") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "${exerciseType.title()}: внеси максимум по каждому варианту. План начнется с самого сильного уверенного уровня.",
                style = MaterialTheme.typography.titleMedium,
            )
            metrics.forEach { metric ->
                OutlinedTextField(
                    value = values.getValue(metric.key),
                    onValueChange = { updated -> values[metric.key] = updated.filter(Char::isDigit).ifEmpty { "0" } },
                    label = { Text(metric.label) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = {
                    val result = viewModel.buildAssessmentResult(
                        exerciseType = exerciseType,
                        values = values.mapValues { (_, raw) -> raw.toIntOrNull() ?: 0 },
                    )
                    viewModel.createProgram(result, onCreated)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Создать программу")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramScreen(
    exerciseType: ExerciseType,
    detail: ProgramDetail?,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTechnique: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseType.title()) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
            )
        },
    ) { padding ->
        if (detail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Сначала создай программу на главном экране.")
            }
        } else {
            val nextWorkout = detail.workouts.firstOrNull { !it.completed }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SummaryCard(
                        detail = detail,
                        nextWorkout = nextWorkout,
                        onOpenSession = onOpenSession,
                        onOpenHistory = onOpenHistory,
                        onOpenTechnique = onOpenTechnique,
                    )
                }
                items(detail.workouts, key = { it.id }) { workout ->
                    WorkoutCard(workout = workout, onOpenSession = onOpenSession)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    detail: ProgramDetail,
    nextWorkout: WorkoutSession?,
    onOpenSession: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTechnique: () -> Unit,
) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Текущий блок", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(detail.summary.currentLevel.title(detail.summary.exerciseType)) })
                AssistChip(onClick = {}, label = { Text(detail.summary.currentDifficulty.title()) })
            }
            Text(detail.techniqueTip.body)
            if (nextWorkout != null) {
                Button(onClick = { onOpenSession(nextWorkout.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Начать: ${nextWorkout.title.lowercase()}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onOpenHistory) { Text("История") }
                TextButton(onClick = onOpenTechnique) { Text("Техника") }
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutSession,
    onOpenSession: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (workout.completed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("${workout.title} • неделя ${workout.weekIndex}", fontWeight = FontWeight.SemiBold)
            Text("${workout.scheduledDate.format(dateFormatter)} • ${workout.level.title(workout.exerciseType)}")
            Text(if (workout.isTest) "Контрольный тест для перехода уровня." else "Пауза между подходами: ${workout.prescriptions.firstOrNull()?.restSeconds ?: 0} сек.")
            Text(workout.transitionHint)
            if (!workout.completed) {
                Button(onClick = { onOpenSession(workout.id) }) { Text("Открыть") }
            } else {
                Text("Выполнено", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionScreen(
    exerciseType: ExerciseType,
    workoutId: String,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    var workout by remember { mutableStateOf<WorkoutSession?>(null) }
    var currentSetIndex by rememberSaveable { mutableIntStateOf(0) }
    val results = remember { mutableStateListOf<SetResult>() }
    val inputs = remember { mutableStateMapOf<Int, String>() }
    var restSecondsLeft by rememberSaveable { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(workoutId) {
        workout = viewModel.loadWorkout(workoutId)
    }

    val currentWorkout = workout
    val currentSet = currentWorkout?.prescriptions?.getOrNull(currentSetIndex)

    LaunchedEffect(restSecondsLeft) {
        if (restSecondsLeft > 0) {
            delay(1_000)
            restSecondsLeft -= 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сессия") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
            )
        },
    ) { padding ->
        when {
            currentWorkout == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Загружаю тренировку…")
                }
            }

            restSecondsLeft > 0 -> {
                RestScreen(
                    modifier = Modifier.padding(padding),
                    secondsLeft = restSecondsLeft,
                    nextSet = currentSetIndex + 1,
                    totalSets = currentWorkout.prescriptions.size,
                    onSkip = { restSecondsLeft = 0 },
                )
            }

            currentSet == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Сессия завершена.")
                }
            }

            else -> {
                val inputValue = inputs[currentSet.setIndex] ?: ""
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("${exerciseType.title()} • ${currentWorkout.title}", style = MaterialTheme.typography.titleLarge)
                    Text("Подход ${currentSetIndex + 1} из ${currentWorkout.prescriptions.size}")
                    SessionMetricCard(currentWorkout, currentSet)
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputs[currentSet.setIndex] = it.filter(Char::isDigit) },
                        label = {
                            Text(if (currentSet.targetSeconds != null) "Фактически секунд" else "Фактически повторов")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            val actual = inputValue.toIntOrNull() ?: 0
                            results.removeAll { it.setIndex == currentSet.setIndex }
                            results += SetResult(
                                setIndex = currentSet.setIndex,
                                actualReps = currentSet.targetReps?.let { actual },
                                actualSeconds = currentSet.targetSeconds?.let { actual },
                                completed = actual >= (currentSet.targetReps ?: currentSet.targetSeconds ?: 0),
                            )

                            if (currentSetIndex == currentWorkout.prescriptions.lastIndex) {
                                isSubmitting = true
                                viewModel.completeWorkout(exerciseType, workoutId, results.toList()) {
                                    isSubmitting = false
                                    onFinished()
                                }
                            } else {
                                currentSetIndex += 1
                                restSecondsLeft = currentSet.restSeconds
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (currentSetIndex == currentWorkout.prescriptions.lastIndex) "Завершить тренировку" else "Сохранить подход")
                    }
                    Text(currentWorkout.transitionHint, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun SessionMetricCard(workout: WorkoutSession, set: com.vaslit.repflow.domain.SetPrescription) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(workout.level.title(workout.exerciseType), style = MaterialTheme.typography.titleLarge)
            Text(
                if (set.targetSeconds != null) "Цель: ${set.targetSeconds} сек" else "Цель: ${set.targetReps} повторов",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text("Отдых после подхода: ${set.restSeconds} сек")
            Text(set.note)
            Text(workout.techniqueTip.body)
        }
    }
}

@Composable
private fun RestScreen(
    modifier: Modifier = Modifier,
    secondsLeft: Int,
    nextSet: Int,
    totalSets: Int,
    onSkip: () -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Отдых", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text("$secondsLeft сек", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Дальше подход $nextSet из $totalSets")
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onSkip) { Text("Пропустить паузу") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionScreen(
    exerciseType: ExerciseType,
    evaluation: EvaluationSnapshot?,
    onDone: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Итог тренировки") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(exerciseType.title(), style = MaterialTheme.typography.headlineMedium)
            if (evaluation == null) {
                Text("Нет данных о завершенной тренировке.")
            } else {
                Card(shape = RoundedCornerShape(28.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Рекомендация: ${evaluation.recommendation.title()}", fontWeight = FontWeight.Bold)
                        Text("Успешность: ${(evaluation.successRate * 100).toInt()}%")
                        Text(evaluation.summary)
                    }
                }
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Вернуться к плану")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    exerciseType: ExerciseType,
    detail: ProgramDetail?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История: ${exerciseType.title()}") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
            )
        },
    ) { padding ->
        if (detail == null || detail.history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("История появится после первой завершенной тренировки.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(detail.history, key = { "${it.completedAt}-${it.title}" }) { entry ->
                    Card(shape = RoundedCornerShape(22.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(entry.title, fontWeight = FontWeight.SemiBold)
                            Text("${entry.level.title(exerciseType)} • ${entry.difficulty.title()}")
                            Text("Успешность ${(entry.successRate * 100).toInt()}%")
                            Text(entry.summary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechniqueScreen(
    exerciseType: ExerciseType,
    guide: List<Pair<ProgressionLevel, com.vaslit.repflow.domain.TechniqueTip>>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Техника: ${exerciseType.title()}") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(guide, key = { it.first.name }) { (level, tip) ->
                Card(shape = RoundedCornerShape(26.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(level.title(exerciseType), fontWeight = FontWeight.Bold)
                        Text(tip.title, color = MaterialTheme.colorScheme.tertiary)
                        Text(tip.body)
                    }
                }
            }
        }
    }
}

private val exerciseCards = listOf(ExerciseType.PULL_UP, ExerciseType.PUSH_UP)
private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")

private fun ExerciseType.title(): String = when (this) {
    ExerciseType.PULL_UP -> "Подтягивания"
    ExerciseType.PUSH_UP -> "Отжимания"
}

private fun ProgressionLevel.title(exerciseType: ExerciseType): String = when (this) {
    ProgressionLevel.SCAPULAR_HANG -> "Вис и лопатки"
    ProgressionLevel.NEGATIVE -> "Негативы"
    ProgressionLevel.BANDED -> "С резинкой"
    ProgressionLevel.STRICT -> "Без резинки"
    ProgressionLevel.WALL -> "От стены"
    ProgressionLevel.INCLINE -> "От высокой опоры"
    ProgressionLevel.KNEE -> "С колен"
    ProgressionLevel.CLASSIC -> if (exerciseType == ExerciseType.PUSH_UP) "Классические" else "Классика"
}

private fun com.vaslit.repflow.domain.DifficultyLevel.title(): String = when (this) {
    com.vaslit.repflow.domain.DifficultyLevel.EASY -> "легкая"
    com.vaslit.repflow.domain.DifficultyLevel.BASE -> "базовая"
    com.vaslit.repflow.domain.DifficultyLevel.HARD -> "усиленная"
}

private fun com.vaslit.repflow.domain.ProgressionRecommendation.title(): String = when (this) {
    com.vaslit.repflow.domain.ProgressionRecommendation.KEEP -> "оставить текущий план"
    com.vaslit.repflow.domain.ProgressionRecommendation.DELOAD -> "снизить объем"
    com.vaslit.repflow.domain.ProgressionRecommendation.ADVANCE -> "перейти дальше"
    com.vaslit.repflow.domain.ProgressionRecommendation.CHANGE_BAND -> "ослабить резинку"
    com.vaslit.repflow.domain.ProgressionRecommendation.TRY_STRICT -> "пробовать без резинки"
}
