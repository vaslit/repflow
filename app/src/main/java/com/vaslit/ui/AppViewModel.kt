package com.vaslit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaslit.data.AppRepository
import com.vaslit.data.ProgramDetail
import com.vaslit.data.ProgramSummary
import com.vaslit.domain.AssessmentEngine
import com.vaslit.domain.AssessmentMetric
import com.vaslit.domain.AssessmentResult
import com.vaslit.domain.EvaluationSnapshot
import com.vaslit.domain.ExerciseType
import com.vaslit.domain.ProgressionLevel
import com.vaslit.domain.SetResult
import com.vaslit.domain.TechniqueTip
import com.vaslit.domain.TrainingCatalog
import com.vaslit.domain.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: AppRepository,
) : ViewModel() {
    val programSummaries: StateFlow<List<ProgramSummary>> = repository.observeProgramSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastEvaluation = MutableStateFlow<EvaluationSnapshot?>(null)
    val lastEvaluation = _lastEvaluation.asStateFlow()

    fun observeProgramDetail(exerciseType: ExerciseType): kotlinx.coroutines.flow.Flow<ProgramDetail?> =
        repository.observeProgramDetail(exerciseType)

    fun defaultMetrics(exerciseType: ExerciseType): List<AssessmentMetric> =
        TrainingCatalog.defaultAssessmentMetrics(exerciseType)

    fun createProgram(
        assessmentResult: AssessmentResult,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.createProgram(assessmentResult)
            onComplete()
        }
    }

    suspend fun loadWorkout(workoutId: String): WorkoutSession? = repository.getWorkout(workoutId)

    fun completeWorkout(
        exerciseType: ExerciseType,
        workoutId: String,
        setResults: List<SetResult>,
        onComplete: (EvaluationSnapshot) -> Unit,
    ) {
        viewModelScope.launch {
            val evaluation = repository.completeWorkout(exerciseType, workoutId, setResults)
            _lastEvaluation.value = evaluation
            onComplete(evaluation)
        }
    }

    fun techniqueGuide(exerciseType: ExerciseType): List<Pair<ProgressionLevel, TechniqueTip>> =
        repository.techniqueGuide(exerciseType)

    fun buildAssessmentResult(
        exerciseType: ExerciseType,
        values: Map<String, Int>,
    ): AssessmentResult {
        val metrics = defaultMetrics(exerciseType).map { metric ->
            metric.copy(value = values[metric.key] ?: 0)
        }
        return when (exerciseType) {
            ExerciseType.PULL_UP -> AssessmentEngine.assessPullUps(metrics)
            ExerciseType.PUSH_UP -> AssessmentEngine.assessPushUps(metrics)
        }
    }

    class Factory(
        private val repository: AppRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repository) as T
    }
}
