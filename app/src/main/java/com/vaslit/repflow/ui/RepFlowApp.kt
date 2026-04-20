package com.vaslit.repflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaslit.repflow.data.AnalyticsEntry
import com.vaslit.repflow.data.DashboardProgram
import com.vaslit.repflow.data.DashboardState
import com.vaslit.repflow.data.ProgramAnalytics
import com.vaslit.repflow.data.ProgramSummary
import com.vaslit.repflow.data.WorkoutCalendarItem
import com.vaslit.repflow.data.WorkoutCalendarStatus
import com.vaslit.repflow.domain.EvaluationSnapshot
import com.vaslit.repflow.domain.ExerciseType
import com.vaslit.repflow.domain.ProgressionLevel
import com.vaslit.repflow.domain.SetResult
import com.vaslit.repflow.domain.WorkoutSession
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun RepFlowTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Color(0xFF6FD3FF),
        onPrimary = Color(0xFF09111A),
        secondary = Color(0xFFFF9D5C),
        onSecondary = Color(0xFF24150B),
        tertiary = Color(0xFF9AE2B5),
        background = Color(0xFF0A1017),
        surface = Color(0xFF121A24),
        surfaceVariant = Color(0xFF182230),
        onSurface = Color(0xFFE8F0FF),
        onSurfaceVariant = Color(0xFF9FB0C7),
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
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route.orEmpty()
    val mainTabs = remember { listOf(MainTab.Plans, MainTab.Calendar, MainTab.Stats) }
    val showBottomBar = mainTabs.any { currentRoute.startsWith(it.route) }
    val dashboard by viewModel.dashboard.collectAsState()
    val programs by viewModel.programSummaries.collectAsState()
    val calendarItems by viewModel.calendarItems.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NeoBottomBar(
                    currentRoute = currentRoute,
                    tabs = mainTabs,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo("plans") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            Color(0xFF0F1825),
                            Color(0xFF0A1017),
                        ),
                    ),
                )
                .padding(padding),
        ) {
            NavHost(
                navController = navController,
                startDestination = "plans",
            ) {
                composable("plans") {
                    PlansScreen(
                        dashboard = dashboard,
                        programs = programs,
                        onStartAssessment = { navController.navigate("assessment/${it.name}") },
                        onOpenSession = { item ->
                            navController.navigate("session/${item.exerciseType.name}/${item.id}")
                        },
                    )
                }
                composable("calendar") {
                    CalendarScreen(
                        items = calendarItems,
                        onOpenSession = { item ->
                            navController.navigate("session/${item.exerciseType.name}/${item.id}")
                        },
                        onMoveWorkout = viewModel::moveWorkout,
                    )
                }
                composable("stats") {
                    StatsScreen(
                        programs = programs,
                        observeAnalytics = viewModel::observeAnalytics,
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
                            navController.navigate("plans") {
                                popUpTo("plans") { inclusive = true }
                            }
                        },
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
                            navController.navigate("plans") {
                                popUpTo("plans") { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}

private enum class MainTab(
    val route: String,
    val label: String,
) {
    Plans("plans", "Планы"),
    Calendar("calendar", "Календарь"),
    Stats("stats", "Статистика"),
}

@Composable
private fun NeoBottomBar(
    currentRoute: String,
    tabs: List<MainTab>,
    onSelect: (String) -> Unit,
) {
    Surface(
        tonalElevation = 0.dp,
        color = Color.Transparent,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .shadow(20.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.45f))
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp)),
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute.startsWith(tab.route)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab.route) },
                    icon = {
                        MainTabIcon(tab = tab, selected = selected)
                    },
                    label = { Text(tab.label) },
                )
            }
        }
    }
}

@Composable
private fun MainTabIcon(
    tab: MainTab,
    selected: Boolean,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(22.dp)) {
        val strokeWidth = 2.2.dp.toPx()
        val thinStroke = 1.6.dp.toPx()
        when (tab) {
            MainTab.Plans -> {
                val left = size.width * 0.14f
                val right = size.width * 0.86f
                val rows = listOf(0.28f, 0.5f, 0.72f)
                rows.forEach { yFactor ->
                    val y = size.height * yFactor
                    drawCircle(
                        color = color,
                        radius = 1.8.dp.toPx(),
                        center = Offset(left, y),
                    )
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.3f, y),
                        end = Offset(right, y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            MainTab.Calendar -> {
                val pad = size.width * 0.12f
                val top = size.height * 0.2f
                val bottom = size.height * 0.86f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pad, top),
                    size = androidx.compose.ui.geometry.Size(size.width - pad * 2, bottom - top),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = strokeWidth),
                )
                drawLine(
                    color = color,
                    start = Offset(pad, size.height * 0.36f),
                    end = Offset(size.width - pad, size.height * 0.36f),
                    strokeWidth = thinStroke,
                    cap = StrokeCap.Round,
                )
                listOf(0.32f, 0.68f).forEach { xFactor ->
                    val x = size.width * xFactor
                    drawLine(
                        color = color,
                        start = Offset(x, top - 1.dp.toPx()),
                        end = Offset(x, size.height * 0.28f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
                listOf(0.5f, 0.68f).forEach { yFactor ->
                    val y = size.height * yFactor
                    drawLine(
                        color = color.copy(alpha = 0.8f),
                        start = Offset(size.width * 0.26f, y),
                        end = Offset(size.width * 0.74f, y),
                        strokeWidth = thinStroke,
                        cap = StrokeCap.Round,
                    )
                }
            }

            MainTab.Stats -> {
                val xAxisY = size.height * 0.82f
                val xStart = size.width * 0.16f
                val xEnd = size.width * 0.86f
                val yStart = size.height * 0.16f
                drawLine(
                    color = color.copy(alpha = 0.75f),
                    start = Offset(xStart, yStart),
                    end = Offset(xStart, xAxisY),
                    strokeWidth = thinStroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color.copy(alpha = 0.75f),
                    start = Offset(xStart, xAxisY),
                    end = Offset(xEnd, xAxisY),
                    strokeWidth = thinStroke,
                    cap = StrokeCap.Round,
                )

                val path = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.72f)
                    lineTo(size.width * 0.42f, size.height * 0.58f)
                    lineTo(size.width * 0.58f, size.height * 0.63f)
                    lineTo(size.width * 0.78f, size.height * 0.34f)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                val arrow = Path().apply {
                    moveTo(size.width * 0.78f, size.height * 0.34f)
                    lineTo(size.width * 0.69f, size.height * 0.35f)
                    moveTo(size.width * 0.78f, size.height * 0.34f)
                    lineTo(size.width * 0.75f, size.height * 0.43f)
                }
                drawPath(
                    path = arrow,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
internal fun PlansScreen(
    dashboard: DashboardState,
    programs: List<ProgramSummary>,
    onStartAssessment: (ExerciseType) -> Unit,
    onOpenSession: (WorkoutCalendarItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            NeoPanel {
                Text("RepFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "План на сегодня, календарь тренировок и прогресс по целям.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text("Программы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        exerciseCards.forEach { type ->
            item {
                val summary = programs.firstOrNull { it.exerciseType == type }
                val programCard = dashboard.programs.firstOrNull { it.exerciseType == type }
                ProgramOverviewCard(
                    exerciseType = type,
                    summary = summary,
                    dashboardProgram = programCard,
                    onStartAssessment = { onStartAssessment(type) },
                    onOpenSession = {
                        programCard?.nextWorkout?.let(onOpenSession)
                    },
                )
            }
        }

        item {
            AgendaSection(
                title = "Сегодня",
                items = dashboard.todayItems,
                emptyText = "Сегодня нет запланированных тренировок.",
                onOpenSession = onOpenSession,
            )
        }
        item {
            AgendaSection(
                title = "Завтра",
                items = dashboard.tomorrowItems,
                emptyText = "На завтра пока пусто.",
                onOpenSession = onOpenSession,
            )
        }
        item {
            AgendaSection(
                title = "Позже",
                items = dashboard.laterItems.take(8),
                emptyText = "Следующие тренировки появятся после завершения текущего цикла.",
                onOpenSession = onOpenSession,
            )
        }
    }
}

@Composable
private fun ProgramOverviewCard(
    exerciseType: ExerciseType,
    summary: ProgramSummary?,
    dashboardProgram: DashboardProgram?,
    onStartAssessment: () -> Unit,
    onOpenSession: () -> Unit,
) {
    val accent = exerciseColor(exerciseType)
    NeoPanel(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(exerciseType.title(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (summary == null) {
                    Text("Пройди стартовый тест и выбери удобные дни тренировок.")
                } else {
                    Text(summary.title, color = accent, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Уровень: ${summary.currentLevel.title(exerciseType)} • ${summary.currentDifficulty.title()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Дни: ${summary.preferredDays.joinToString(" • ") { it.shortLabel() }}",
                        color = accent.copy(alpha = 0.9f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (summary.maintenanceMode) {
                            "Цель достигнута: ${summary.goalLabel}"
                        } else {
                            "Финальная цель: ${summary.goalLabel}"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    GoalProgressBar(
                        current = summary.bestGoalScore,
                        target = summary.goalTarget,
                        accent = accent,
                    )
                    summary.nextWorkoutDate?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Следующая: ${it.format(fullDateFormatter)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            PlanTypeBadge(exerciseType = exerciseType)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (dashboardProgram?.nextWorkout != null) {
                PrimaryNeoButton(
                    text = if (summary?.maintenanceMode == true) {
                        "Поддержка"
                    } else if (dashboardProgram.nextWorkout.scheduledDate == LocalDate.now()) {
                        "Старт сегодня"
                    } else {
                        "Открыть план"
                    },
                    accent = accent,
                    onClick = onOpenSession,
                )
            } else {
                PrimaryNeoButton(
                    text = "Пройти тест",
                    accent = accent,
                    onClick = onStartAssessment,
                )
            }
            SecondaryNeoButton(text = "Тест заново", onClick = onStartAssessment)
        }
    }
}

@Composable
private fun GoalProgressBar(
    current: Int,
    target: Int,
    accent: Color,
) {
    val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Прогресс к цели", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$current / $target", color = accent, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    }
}

@Composable
private fun AgendaSection(
    title: String,
    items: List<WorkoutCalendarItem>,
    emptyText: String,
    onOpenSession: (WorkoutCalendarItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) {
            NeoPanel {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items.forEach { item ->
                AgendaWorkoutCard(item = item, onOpenSession = { onOpenSession(item) })
            }
        }
    }
}

@Composable
private fun AgendaWorkoutCard(
    item: WorkoutCalendarItem,
    onOpenSession: () -> Unit,
) {
    val accent = exerciseColor(item.exerciseType)
    NeoPanel(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(
                    "${item.exerciseType.title()} • ${item.scheduledDate.format(fullDateFormatter)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(item.status)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (item.status == WorkoutCalendarStatus.PLANNED) {
            PrimaryNeoButton(
                text = if (item.isTest) "Открыть тест" else "Открыть тренировку",
                accent = accent,
                onClick = onOpenSession,
            )
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
    val existingProgram by viewModel.observeProgramDetail(exerciseType).collectAsState(initial = null)
    val metrics = remember(exerciseType) { viewModel.defaultMetrics(exerciseType) }
    val values = remember(exerciseType) {
        mutableStateMapOf<String, String>().apply {
            putAll(metrics.associate { it.key to "0" })
        }
    }
    val defaultDays = remember { listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY) }
    val selectedDays = remember(exerciseType) { mutableStateListOf<DayOfWeek>().apply { addAll(defaultDays) } }

    NeoScaffold(
        title = "Стартовый тест",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NeoPanel(accent = exerciseColor(exerciseType)) {
                Text(
                    text = "${exerciseType.title()}: внеси максимум по каждому варианту и выбери дни, когда тебе реально удобно тренироваться.",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (existingProgram != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Повторный тест начнет новый цикл тренировок, а текущий сохранится в истории.",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            NeoPanel {
                Text("Предпочитаемые дни", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    weekDays.forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                if (day in selectedDays) {
                                    if (selectedDays.size > 1) {
                                        selectedDays.remove(day)
                                    }
                                } else {
                                    selectedDays += day
                                }
                            },
                            label = { Text(day.shortLabel()) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "После теста приложение составит ближайший цикл тренировок и отдельный контрольный день.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            metrics.forEach { metric ->
                NeoPanel {
                    OutlinedTextField(
                        value = values.getValue(metric.key),
                        onValueChange = { updated -> values[metric.key] = updated.filter(Char::isDigit).ifEmpty { "0" } },
                        label = { Text(metric.label) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            PrimaryNeoButton(
                text = if (existingProgram == null) "Собрать фазу" else "Пересобрать фазу",
                accent = exerciseColor(exerciseType),
                enabled = selectedDays.isNotEmpty(),
                onClick = {
                    val result = viewModel.buildAssessmentResult(
                        exerciseType = exerciseType,
                        values = values.mapValues { (_, raw) -> raw.toIntOrNull() ?: 0 },
                    )
                    viewModel.createProgram(result, selectedDays.toList(), onCreated)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(
    items: List<WorkoutCalendarItem>,
    onOpenSession: (WorkoutCalendarItem) -> Unit,
    onMoveWorkout: (String, LocalDate) -> Unit,
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var workoutToMove by remember { mutableStateOf<WorkoutCalendarItem?>(null) }

    if (workoutToMove != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = workoutToMove?.scheduledDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { workoutToMove = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val newDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            onMoveWorkout(workoutToMove!!.id, newDate)
                        }
                        workoutToMove = null
                    },
                ) {
                    Text("Перенести")
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToMove = null }) {
                    Text("Отмена")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val monthItems = remember(items, month) {
        items.filter { YearMonth.from(it.scheduledDate) == month }.groupBy(WorkoutCalendarItem::scheduledDate)
    }
    val selectedItems = items.filter { it.scheduledDate == selectedDate }

    NeoScaffold(title = "Календарь") { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                NeoPanel {
                    CalendarHeader(month = month, onPrevious = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) })
                    Spacer(modifier = Modifier.height(12.dp))
                    CalendarGrid(
                        month = month,
                        itemsByDate = monthItems,
                        selectedDate = selectedDate,
                        onSelectDate = { selectedDate = it },
                    )
                }
            }
            item {
                Text(
                    "Выбрано: ${selectedDate.format(fullDateFormatter)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (selectedItems.isEmpty()) {
                item {
                    NeoPanel {
                        Text("На эту дату ничего не запланировано.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(selectedItems, key = { it.id }) { item ->
                    CalendarItemCard(
                        item = item,
                        onOpenSession = { onOpenSession(item) },
                        onMove = { workoutToMove = item },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("←") }
        Text(month.format(monthFormatter), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onNext) { Text("→") }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    itemsByDate: Map<LocalDate, List<WorkoutCalendarItem>>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day.shortLabel(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val firstDay = month.atDay(1)
        val offset = firstDay.dayOfWeek.value - 1
        val dates = buildList<LocalDate?> {
            repeat(offset) { add(null) }
            (1..month.lengthOfMonth()).forEach { add(month.atDay(it)) }
            while (size % 7 != 0) add(null)
        }

        dates.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        items = date?.let(itemsByDate::get).orEmpty(),
                        selected = date == selectedDate,
                        onClick = { date?.let(onSelectDate) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    items: List<WorkoutCalendarItem>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val dominantStatus = remember(items) { dominantCalendarStatus(items) }
    val dominantColor = dominantStatus?.let(::statusColor)
    val highlightedBackground = dominantColor?.copy(alpha = 0.16f)
    Box(
        modifier = modifier
            .aspectRatio(0.9f)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface))
                } else {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            highlightedBackground ?: MaterialTheme.colorScheme.background,
                        ),
                    )
                },
            )
            .border(
                1.dp,
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    dominantColor != null -> dominantColor.copy(alpha = 0.45f)
                    else -> Color.White.copy(alpha = 0.04f)
                },
                shape,
            )
            .clickable(enabled = date != null, onClick = onClick)
            .padding(8.dp),
    ) {
        if (date != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontWeight = if (date == LocalDate.now()) FontWeight.Black else FontWeight.Medium,
                    color = if (date == LocalDate.now()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.take(2).forEach { item ->
                        CalendarItemInlineBadge(item = item)
                    }
                    if (items.size > 2) {
                        Text(
                            "+${items.size - 2} еще",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarItemInlineBadge(
    item: WorkoutCalendarItem,
) {
    val status = statusColor(item.status)
    val accent = exerciseColor(item.exerciseType)
    Surface(
        color = status.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .border(1.dp, status, CircleShape),
            )
            Text(
                text = calendarItemCompactLabel(item),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CalendarItemCard(
    item: WorkoutCalendarItem,
    onOpenSession: () -> Unit,
    onMove: () -> Unit,
) {
    val accent = exerciseColor(item.exerciseType)
    NeoPanel(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(
                    item.exerciseType.title(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Блок ${item.phaseIndex}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(item.status)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (item.status == WorkoutCalendarStatus.PLANNED) {
                PrimaryNeoButton(
                    text = if (item.isTest) "Открыть тест" else "Открыть",
                    accent = accent,
                    onClick = onOpenSession,
                )
                SecondaryNeoButton(text = "Перенести", onClick = onMove)
            } else {
                Text(
                    when (item.status) {
                        WorkoutCalendarStatus.COMPLETED_STRONG -> "Выполнено уверенно"
                        WorkoutCalendarStatus.COMPLETED_OK -> "Выполнено с запасом на корректировку"
                        WorkoutCalendarStatus.MISSED -> "Пропущено"
                        WorkoutCalendarStatus.PLANNED -> ""
                    },
                    color = statusColor(item.status),
                )
            }
        }
    }
}

@Composable
internal fun StatsScreen(
    programs: List<ProgramSummary>,
    observeAnalytics: (ExerciseType) -> kotlinx.coroutines.flow.Flow<ProgramAnalytics?>,
) {
    var selectedType by remember { mutableStateOf<ExerciseType?>(null) }
    var selectedRange by rememberSaveable { mutableStateOf(StatsRange.MONTH) }

    LaunchedEffect(programs) {
        if (selectedType == null || programs.none { it.exerciseType == selectedType }) {
            selectedType = programs.firstOrNull()?.exerciseType
        }
    }

    val analyticsState = if (selectedType != null) {
        observeAnalytics(requireNotNull(selectedType)).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<ProgramAnalytics?>(null) }
    }
    val analytics by analyticsState

    val filteredEntries = remember(analytics, selectedRange) {
        val cutoff = LocalDate.now().minusDays(selectedRange.daysBack.toLong())
        analytics?.entries?.filter { !it.date.isBefore(cutoff) }.orEmpty()
    }
    val volumeSeries = remember(filteredEntries) {
        filteredEntries.groupBy(AnalyticsEntry::date).map { (date, entries) -> date to entries.sumOf(AnalyticsEntry::totalVolume) }.sortedBy { it.first }
    }
    val testSeries = remember(filteredEntries) {
        filteredEntries.filter { it.isTest && it.testScore != null }.map { it.date to requireNotNull(it.testScore) }
    }

    NeoScaffold(title = "Статистика") { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (programs.isEmpty()) {
                    NeoPanel {
                        Text("Сначала создай хотя бы одну программу, чтобы появилась статистика.")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Программа", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            programs.forEach { summary ->
                                FilterChip(
                                    selected = selectedType == summary.exerciseType,
                                    onClick = { selectedType = summary.exerciseType },
                                    label = { Text(summary.exerciseType.title()) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatsRange.entries.forEach { range ->
                                FilterChip(
                                    selected = selectedRange == range,
                                    onClick = { selectedRange = range },
                                    label = { Text(range.label) },
                                )
                            }
                        }
                    }
                }
            }

            if (selectedType != null) {
                item {
                    SummaryStatsPanel(
                        entries = filteredEntries,
                        exerciseType = requireNotNull(selectedType),
                    )
                }
                item {
                    ChartPanel(
                        title = "Объем по тренировкам",
                        subtitle = "Сумма повторов/секунд за выбранный период.",
                        points = volumeSeries,
                        lineColor = exerciseColor(requireNotNull(selectedType)),
                        emptyText = "Пока нет завершенных тренировок в этом диапазоне.",
                    )
                }
                item {
                    ChartPanel(
                        title = "Контрольные тесты",
                        subtitle = "Результат первого контрольного подхода.",
                        points = testSeries,
                        lineColor = MaterialTheme.colorScheme.secondary,
                        emptyText = "Контрольные тесты еще не выполнены.",
                    )
                }
            }
        }
    }
}

private enum class StatsRange(
    val label: String,
    val daysBack: Int,
) {
    WEEK("Неделя", 7),
    MONTH("Месяц", 30),
    THREE_MONTHS("3 месяца", 90),
    SIX_MONTHS("6 месяцев", 180),
}

private enum class SessionPhase {
    SET,
    REST,
    DONE,
}

@Composable
private fun SummaryStatsPanel(
    entries: List<AnalyticsEntry>,
    exerciseType: ExerciseType,
) {
    val totalVolume = entries.sumOf(AnalyticsEntry::totalVolume)
    val tests = entries.count(AnalyticsEntry::isTest)
    val averageSuccess = if (entries.isEmpty()) 0 else (entries.map(AnalyticsEntry::successRate).average() * 100).roundToInt()
    NeoPanel(accent = exerciseColor(exerciseType)) {
        Text("Сводка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(label = "Сессии", value = entries.size.toString(), modifier = Modifier.weight(1f))
            StatTile(label = "Объем", value = totalVolume.toString(), modifier = Modifier.weight(1f))
            StatTile(label = "Тесты", value = tests.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text("Средняя успешность: $averageSuccess%", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartPanel(
    title: String,
    subtitle: String,
    points: List<Pair<LocalDate, Int>>,
    lineColor: Color,
    emptyText: String,
) {
    NeoPanel {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(14.dp))
        if (points.isEmpty()) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LineChart(points = points, lineColor = lineColor)
        }
    }
}

@Composable
private fun LineChart(
    points: List<Pair<LocalDate, Int>>,
    lineColor: Color,
) {
    val maxValue = points.maxOf { it.second }.coerceAtLeast(1)
    val minValue = points.minOf { it.second }
    val labels = listOfNotNull(points.firstOrNull()?.first, points.getOrNull(points.lastIndex / 2)?.first, points.lastOrNull()?.first).distinct()
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val leftPadding = 28.dp.toPx()
            val bottomPadding = 26.dp.toPx()
            val topPadding = 16.dp.toPx()
            val width = size.width - leftPadding - 12.dp.toPx()
            val height = size.height - topPadding - bottomPadding

            repeat(4) { index ->
                val y = topPadding + height * index / 3f
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(leftPadding, y),
                    end = Offset(leftPadding + width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val normalized = points.mapIndexed { index, point ->
                val x = if (points.size == 1) leftPadding + width / 2 else leftPadding + width * index / (points.lastIndex.toFloat())
                val ratio = if (maxValue == minValue) 0.5f else (point.second - minValue).toFloat() / (maxValue - minValue).toFloat()
                val y = topPadding + height - (height * ratio)
                Offset(x, y)
            }

            val path = Path()
            normalized.forEachIndexed { index, offset ->
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
            normalized.forEach { offset ->
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = offset)
                drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = offset)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { label ->
                Text(label.format(shortDateFormatter), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
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
    var activeSetIndex by rememberSaveable { mutableIntStateOf(0) }
    var sessionPhase by rememberSaveable { mutableStateOf(SessionPhase.SET) }
    val actualInputs = remember { mutableStateMapOf<Int, String>() }
    val setDurationInputs = remember { mutableStateMapOf<Int, String>() }
    val restDurationInputs = remember { mutableStateMapOf<Int, String>() }
    var timerSeconds by rememberSaveable { mutableIntStateOf(0) }
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var currentActualDraft by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(workoutId) {
        workout = viewModel.loadWorkout(workoutId)
    }

    LaunchedEffect(workout?.id) {
        val currentWorkout = workout ?: return@LaunchedEffect
        activeSetIndex = 0
        sessionPhase = SessionPhase.SET
        timerSeconds = 0
        timerRunning = false
        currentActualDraft = ""
        actualInputs.clear()
        setDurationInputs.clear()
        restDurationInputs.clear()
        currentWorkout.prescriptions.forEachIndexed { index, prescription ->
            val defaultActual = (prescription.targetReps ?: prescription.targetSeconds ?: 0)
                .takeIf { it > 0 }
                ?.toString()
                .orEmpty()
            actualInputs[prescription.setIndex] = defaultActual
            setDurationInputs[prescription.setIndex] = ""
            restDurationInputs[prescription.setIndex] = if (index == currentWorkout.prescriptions.lastIndex) {
                "0"
            } else {
                prescription.restSeconds.toString()
            }
        }
    }

    LaunchedEffect(activeSetIndex, workout?.id) {
        val currentWorkout = workout ?: return@LaunchedEffect
        val set = currentWorkout.prescriptions.getOrNull(activeSetIndex) ?: return@LaunchedEffect
        currentActualDraft = actualInputs[set.setIndex].orEmpty()
    }

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1_000)
            timerSeconds += 1
        }
    }

    val currentWorkout = workout
    val activeSet = currentWorkout?.prescriptions?.getOrNull(activeSetIndex)

    NeoScaffold(
        title = if (currentWorkout?.isTest == true) "Контрольный тест" else "Тренировка",
        onBack = onBack,
    ) { padding ->
        when {
            currentWorkout == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Загружаю тренировку…")
                }
            }

            else -> {
                val totalDurationSeconds = currentWorkout.prescriptions.sumOf { prescription ->
                    (setDurationInputs[prescription.setIndex]?.toIntOrNull() ?: 0) +
                        (restDurationInputs[prescription.setIndex]?.toIntOrNull() ?: 0)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    NeoPanel(accent = exerciseColor(exerciseType)) {
                        Text("${exerciseType.title()} • ${currentWorkout.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Заполни таблицу, а ниже отмечай текущий этап и время по секундомеру.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    NeoPanel {
                        Text("Подходы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        SessionTableHeader()
                        Spacer(modifier = Modifier.height(8.dp))
                        currentWorkout.prescriptions.forEachIndexed { index, set ->
                            SessionEditableRow(
                                set = set,
                                rowIndex = index,
                                actualValue = actualInputs[set.setIndex].orEmpty(),
                                setDurationValue = setDurationInputs[set.setIndex].orEmpty(),
                                restDurationValue = restDurationInputs[set.setIndex].orEmpty(),
                                selected = activeSetIndex == index,
                                onActualChange = { actualInputs[set.setIndex] = it.filter(Char::isDigit) },
                                onSetDurationChange = { setDurationInputs[set.setIndex] = it.filter(Char::isDigit) },
                                onRestDurationChange = { restDurationInputs[set.setIndex] = it.filter(Char::isDigit) },
                            )
                            if (index != currentWorkout.prescriptions.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    NeoPanel {
                        Text("Общее время тренировки", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            formatDurationSeconds(totalDurationSeconds),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    activeSet?.let { currentSet ->
                        SessionCurrentStepCard(
                            workout = currentWorkout,
                            set = currentSet,
                            setNumber = activeSetIndex + 1,
                            totalSets = currentWorkout.prescriptions.size,
                            phase = sessionPhase,
                        )
                    }

                    NeoPanel(accent = sessionPhase.color()) {
                        Text("Секундомер", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            formatDurationSeconds(timerSeconds),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            activeSet?.let {
                                when (sessionPhase) {
                                    SessionPhase.SET -> "Подход ${activeSetIndex + 1}"
                                    SessionPhase.REST -> "Отдых"
                                    SessionPhase.DONE -> "Тренировка завершена"
                                }
                            } ?: "Без активного этапа",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SecondaryNeoButton(
                                text = if (timerRunning) "Пауза" else "Старт",
                                onClick = { timerRunning = !timerRunning },
                                modifier = Modifier.weight(1f),
                            )
                            PrimaryNeoButton(
                                text = when (sessionPhase) {
                                    SessionPhase.SET -> "След. этап"
                                    SessionPhase.REST -> "След. этап"
                                    SessionPhase.DONE -> "Этапы завершены"
                                },
                                accent = sessionPhase.color(),
                                enabled = activeSet != null && sessionPhase != SessionPhase.DONE,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val set = activeSet ?: return@PrimaryNeoButton
                                    when (sessionPhase) {
                                        SessionPhase.SET -> {
                                            setDurationInputs[set.setIndex] = timerSeconds.toString()
                                            timerSeconds = 0
                                            timerRunning = true
                                            sessionPhase = if (activeSetIndex == currentWorkout.prescriptions.lastIndex) {
                                                SessionPhase.DONE
                                            } else {
                                                SessionPhase.REST
                                            }
                                        }

                                        SessionPhase.REST -> {
                                            restDurationInputs[set.setIndex] = timerSeconds.toString()
                                            timerSeconds = 0
                                            timerRunning = true
                                            activeSetIndex += 1
                                            sessionPhase = SessionPhase.SET
                                        }

                                        SessionPhase.DONE -> Unit
                                    }
                                },
                            )
                        }
                    }

                    activeSet?.let { currentSet ->
                        SessionCurrentResultEditor(
                            set = currentSet,
                            value = currentActualDraft,
                            onValueChange = { currentActualDraft = it.filter(Char::isDigit) },
                            onSave = {
                                actualInputs[currentSet.setIndex] = currentActualDraft
                            },
                        )
                    }

                    PrimaryNeoButton(
                        text = "Завершить тренировку",
                        accent = exerciseColor(exerciseType),
                        enabled = !isSubmitting,
                        onClick = {
                            val results = currentWorkout.prescriptions.map { set ->
                                val target = set.targetReps ?: set.targetSeconds ?: 0
                                val actual = actualInputs[set.setIndex]?.toIntOrNull() ?: target
                                SetResult(
                                    setIndex = set.setIndex,
                                    actualReps = set.targetReps?.let { actual },
                                    actualSeconds = set.targetSeconds?.let { actual },
                                    setDurationSeconds = setDurationInputs[set.setIndex]?.toIntOrNull() ?: 0,
                                    restDurationSeconds = restDurationInputs[set.setIndex]?.toIntOrNull() ?: 0,
                                    completed = actual >= target,
                                )
                            }
                            isSubmitting = true
                            viewModel.completeWorkout(exerciseType, workoutId, results) {
                                isSubmitting = false
                                onFinished()
                            }
                        },
                    )
                    Text(currentWorkout.transitionHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionEditableRow(
    set: com.vaslit.repflow.domain.SetPrescription,
    rowIndex: Int,
    actualValue: String,
    setDurationValue: String,
    restDurationValue: String,
    selected: Boolean,
    onActualChange: (String) -> Unit,
    onSetDurationChange: (String) -> Unit,
    onRestDurationChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.04f),
                shape = shape,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TableCellText(
                    text = (rowIndex + 1).toString(),
                    modifier = Modifier.weight(0.55f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                TableCellText(
                    text = compactVariantLabel(set.variant),
                    modifier = Modifier.weight(1.15f),
                    color = MaterialTheme.colorScheme.secondary,
                )
                TableCellText(
                    text = targetShortLabel(set),
                    modifier = Modifier.weight(0.85f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                CompactNumericField(
                    value = actualValue,
                    onValueChange = onActualChange,
                    modifier = Modifier.weight(0.85f),
                )
                CompactNumericField(
                    value = setDurationValue,
                    onValueChange = onSetDurationChange,
                    modifier = Modifier.weight(0.95f),
                )
                CompactNumericField(
                    value = restDurationValue,
                    onValueChange = onRestDurationChange,
                    modifier = Modifier.weight(0.95f),
                )
            }
        }
    }
}

@Composable
private fun SessionTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableHeaderText("№", Modifier.weight(0.55f))
        TableHeaderText("Упр", Modifier.weight(1.15f))
        TableHeaderText("Цель", Modifier.weight(0.85f))
        TableHeaderText("Факт", Modifier.weight(0.85f))
        TableHeaderText("Подх", Modifier.weight(0.95f))
        TableHeaderText("Отд", Modifier.weight(0.95f))
    }
}

@Composable
private fun TableHeaderText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun TableCellText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Medium,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontWeight = fontWeight,
        style = MaterialTheme.typography.labelMedium,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SessionCurrentStepCard(
    workout: WorkoutSession,
    set: com.vaslit.repflow.domain.SetPrescription,
    setNumber: Int,
    totalSets: Int,
    phase: SessionPhase,
) {
    NeoPanel(accent = phase.color()) {
        Text(
            when (phase) {
                SessionPhase.SET -> "Подход $setNumber"
                SessionPhase.REST -> "Отдых"
                SessionPhase.DONE -> "Тренировка завершена"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (phase == SessionPhase.REST) {
                "После подхода $setNumber из $totalSets"
            } else {
                "Подход $setNumber из $totalSets"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(set.variant.title, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(targetLabelForSet(set), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (phase == SessionPhase.REST) {
                "Рекомендованный отдых: ${set.restSeconds} сек"
            } else {
                workout.techniqueTip.body
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionCurrentResultEditor(
    set: com.vaslit.repflow.domain.SetPrescription,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    NeoPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = {
                    Text(if (set.targetSeconds != null) "Фактически секунд" else "Фактически повторов")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            SecondaryNeoButton(text = "Сохранить", onClick = onSave)
        }
    }
}

@Composable
private fun CompactNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 9.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            "0",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun SessionMetricCard(workout: WorkoutSession, set: com.vaslit.repflow.domain.SetPrescription) {
    NeoPanel(accent = exerciseColor(workout.exerciseType)) {
        Text(workout.level.title(workout.exerciseType), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            set.variant.title,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (set.targetSeconds != null) {
                "План на подход: ${set.targetSeconds} сек"
            } else {
                "План на подход: ${set.targetReps} повторов"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Можно ввести меньше, если сегодня не идет. Приложение сохранит фактический результат.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Отдых после подхода: ${set.restSeconds} сек")
        Text(set.note)
        Text(workout.techniqueTip.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        NeoPanel(modifier = Modifier.padding(24.dp)) {
            Text("Отдых", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(12.dp))
            Text("$secondsLeft сек", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Дальше подход $nextSet из $totalSets")
            Spacer(modifier = Modifier.height(18.dp))
            SecondaryNeoButton(text = "Пропустить паузу", onClick = onSkip)
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
    NeoScaffold(title = "Итог сессии") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NeoPanel(accent = exerciseColor(exerciseType)) {
                Text(exerciseType.title(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                if (evaluation == null) {
                    Text("Нет данных о завершенной тренировке.")
                } else {
                    Text("Рекомендация: ${evaluation.recommendation.title()}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Успешность: ${(evaluation.successRate * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(evaluation.summary)
                }
            }
            PrimaryNeoButton(
                text = "Вернуться к планам",
                accent = exerciseColor(exerciseType),
                onClick = onDone,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeoScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("Назад") }
                    }
                },
            )
        },
    ) { padding ->
        content(padding)
    }
}

@Composable
private fun NeoPanel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.45f),
                ambientColor = Color.Black.copy(alpha = 0.35f),
            )
            .border(1.dp, Color.White.copy(alpha = 0.05f), shape),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (accent != null) {
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                content()
            }
        }
    }
}

@Composable
private fun PrimaryNeoButton(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = MaterialTheme.colorScheme.background,
        ),
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryNeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun StatusBadge(status: WorkoutCalendarStatus) {
    val color = statusColor(status)
    Surface(
        color = color.copy(alpha = 0.18f),
        contentColor = color,
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(
            text = when (status) {
                WorkoutCalendarStatus.PLANNED -> "Запланировано"
                WorkoutCalendarStatus.COMPLETED_STRONG -> "Выполнено"
                WorkoutCalendarStatus.COMPLETED_OK -> "Сделано"
                WorkoutCalendarStatus.MISSED -> "Пропущено"
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PlanTypeBadge(exerciseType: ExerciseType) {
    val color = exerciseColor(exerciseType)
    Surface(color = color.copy(alpha = 0.16f), contentColor = color, shape = RoundedCornerShape(20.dp)) {
        Text(
            text = if (exerciseType == ExerciseType.PULL_UP) "PU" else "PS",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontWeight = FontWeight.Black,
        )
    }
}

private fun exerciseColor(type: ExerciseType): Color = when (type) {
    ExerciseType.PULL_UP -> Color(0xFF68D8FF)
    ExerciseType.PUSH_UP -> Color(0xFFFFA164)
}

private fun statusColor(status: WorkoutCalendarStatus): Color = when (status) {
    WorkoutCalendarStatus.PLANNED -> Color(0xFF7F9BBD)
    WorkoutCalendarStatus.COMPLETED_STRONG -> Color(0xFF62E08A)
    WorkoutCalendarStatus.COMPLETED_OK -> Color(0xFFFFD65C)
    WorkoutCalendarStatus.MISSED -> Color(0xFFFF6B6B)
}

private fun dominantCalendarStatus(items: List<WorkoutCalendarItem>): WorkoutCalendarStatus? = when {
    items.isEmpty() -> null
    items.any { it.status == WorkoutCalendarStatus.MISSED } -> WorkoutCalendarStatus.MISSED
    items.any { it.status == WorkoutCalendarStatus.PLANNED } -> WorkoutCalendarStatus.PLANNED
    items.any { it.status == WorkoutCalendarStatus.COMPLETED_OK } -> WorkoutCalendarStatus.COMPLETED_OK
    else -> WorkoutCalendarStatus.COMPLETED_STRONG
}

private fun calendarItemCompactLabel(item: WorkoutCalendarItem): String {
    val head = if (item.isTest) "Тест" else item.exerciseType.shortLabel()
    val tail = when (item.status) {
        WorkoutCalendarStatus.PLANNED -> "план"
        WorkoutCalendarStatus.MISSED -> "пропуск"
        WorkoutCalendarStatus.COMPLETED_STRONG,
        WorkoutCalendarStatus.COMPLETED_OK -> "${((item.successRate ?: 0.0) * 100).toInt()}%"
    }
    return "$head $tail"
}

private fun SessionPhase.color(): Color = when (this) {
    SessionPhase.SET -> Color(0xFF68D8FF)
    SessionPhase.REST -> Color(0xFFFFD65C)
    SessionPhase.DONE -> Color(0xFF62E08A)
}

private fun targetLabelForSet(set: com.vaslit.repflow.domain.SetPrescription): String =
    if (set.targetSeconds != null) {
        "Цель ${set.targetSeconds} сек"
    } else {
        "Цель ${set.targetReps ?: 0}"
    }

private fun targetShortLabel(set: com.vaslit.repflow.domain.SetPrescription): String =
    if (set.targetSeconds != null) {
        "${set.targetSeconds}s"
    } else {
        "${set.targetReps ?: 0}"
    }

private fun compactVariantLabel(variant: com.vaslit.repflow.domain.ExerciseVariant): String = when (variant) {
    com.vaslit.repflow.domain.ExerciseVariant.PULL_UP_SCAPULAR_HANG -> "Вис"
    com.vaslit.repflow.domain.ExerciseVariant.PULL_UP_NEGATIVE -> "Негатив"
    com.vaslit.repflow.domain.ExerciseVariant.PULL_UP_BANDED -> "Резинка"
    com.vaslit.repflow.domain.ExerciseVariant.PULL_UP_STRICT -> "Строгие"
    com.vaslit.repflow.domain.ExerciseVariant.PUSH_UP_WALL -> "Стена"
    com.vaslit.repflow.domain.ExerciseVariant.PUSH_UP_INCLINE -> "Опора"
    com.vaslit.repflow.domain.ExerciseVariant.PUSH_UP_KNEE -> "Колени"
    com.vaslit.repflow.domain.ExerciseVariant.PUSH_UP_CLASSIC -> "Классика"
}

private fun formatDurationSeconds(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private val exerciseCards = listOf(ExerciseType.PULL_UP, ExerciseType.PUSH_UP)
private val weekDays = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)
private val shortDateFormatter = DateTimeFormatter.ofPattern("dd.MM")
private val fullDateFormatter = DateTimeFormatter.ofPattern("dd MMM")
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")

private fun ExerciseType.title(): String = when (this) {
    ExerciseType.PULL_UP -> "Подтягивания"
    ExerciseType.PUSH_UP -> "Отжимания"
}

private fun ExerciseType.shortLabel(): String = when (this) {
    ExerciseType.PULL_UP -> "П"
    ExerciseType.PUSH_UP -> "О"
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
    com.vaslit.repflow.domain.ProgressionRecommendation.KEEP -> "оставить текущий уровень"
    com.vaslit.repflow.domain.ProgressionRecommendation.DELOAD -> "снизить объем"
    com.vaslit.repflow.domain.ProgressionRecommendation.ADVANCE -> "перейти дальше"
    com.vaslit.repflow.domain.ProgressionRecommendation.CHANGE_BAND -> "ослабить резинку"
    com.vaslit.repflow.domain.ProgressionRecommendation.TRY_STRICT -> "пробовать без резинки"
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Вс"
}
