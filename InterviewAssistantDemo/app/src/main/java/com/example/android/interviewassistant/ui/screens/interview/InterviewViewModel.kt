package com.example.android.interviewassistant.ui.screens.interview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.android.interviewassistant.data.remote.ConversationMessageDto
import com.example.android.interviewassistant.domain.AppRepository
import com.example.android.interviewassistant.domain.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI state sealed class ─────────────────────────────────────────────────────

data class ConfigState(
    val role: String = "Software Engineer",
    val level: String = "mid",
    val domain: String = "algorithms"
)

data class RubricState(
    val topic: String = "",
    val clarity: Float = 0f,
    val correctness: Float = 0f,
    val communication: Float = 0f,
    val edgeCases: Float = 0f,
    val feedback: String = ""
) {
    val average: Float get() = (clarity + correctness + communication + edgeCases) / 4f
}

sealed class InterviewPhase {
    object Config : InterviewPhase()
    object Starting : InterviewPhase()
    data class Active(
        val sessionId: String,
        val cachedContext: String,
        val isLoading: Boolean = false,
        val lastRubric: RubricState? = null,
        val errorMessage: String? = null
    ) : InterviewPhase()
    data class Summary(
        val overallScore: Float,
        val strongAreas: List<String>,
        val weakSpots: List<String>,
        val summary: String,
        val nextFocus: String
    ) : InterviewPhase()
}

class InterviewViewModel(private val repository: AppRepository) : ViewModel() {

    private val _phase = MutableStateFlow<InterviewPhase>(InterviewPhase.Config)
    val phase: StateFlow<InterviewPhase> = _phase.asStateFlow()

    private val _config = MutableStateFlow(ConfigState())
    val config: StateFlow<ConfigState> = _config.asStateFlow()

    private val _startError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val startError: SharedFlow<String> = _startError.asSharedFlow()

    // Messages driven by DB flow — populated once we have a sessionId
    private val _sessionId = MutableStateFlow<String?>(null)
    val messages = _sessionId
        .flatMapLatest { id ->
            if (id != null) repository.getSessionMessages(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // In-memory conversation history for the backend (role+content pairs)
    private val conversationHistory = mutableListOf<ConversationMessageDto>()

    // Running session delta for the backend
    private val sessionDeltaLines = mutableListOf<String>()
    private var questionCounter = 0

    fun updateRole(value: String) = _config.update { it.copy(role = value) }
    fun updateLevel(value: String) = _config.update { it.copy(level = value) }
    fun updateDomain(value: String) = _config.update { it.copy(domain = value) }

    fun startSession() {
        _phase.value = InterviewPhase.Starting
        conversationHistory.clear()
        sessionDeltaLines.clear()
        questionCounter = 0

        viewModelScope.launch {
            val cfg = _config.value
            when (val result = repository.startSession(cfg.role, cfg.level, cfg.domain)) {
                is Result.Success -> {
                    val (sessionId, context) = result.data
                    _sessionId.value = sessionId
                    _phase.value = InterviewPhase.Active(
                        sessionId = sessionId,
                        cachedContext = context
                    )
                    // Seed the conversation history with the first question
                    val firstMessage = repository.getSessionMessages(sessionId).first()
                    firstMessage.firstOrNull()?.let {
                        conversationHistory.add(ConversationMessageDto("interviewer", it.content))
                    }
                }
                is Result.Error -> {
                    _startError.tryEmit(result.message)
                    _phase.value = InterviewPhase.Config
                }
            }
        }
    }

    fun sendAnswer(answer: String) {
        val active = _phase.value as? InterviewPhase.Active ?: return
        if (active.isLoading || answer.isBlank()) return

        _phase.value = active.copy(isLoading = true, lastRubric = null)

        viewModelScope.launch {
            // Add candidate message to DB
            repository.addMessage(active.sessionId, "candidate", answer)
            conversationHistory.add(ConversationMessageDto("candidate", answer))

            val cfg = _config.value
            val result = repository.evaluateAnswer(
                sessionId = active.sessionId,
                role = cfg.role,
                level = cfg.level,
                domain = cfg.domain,
                context = active.cachedContext,
                sessionDelta = buildSessionDelta(),
                history = conversationHistory.toList(),
                answer = answer
            )

            when (result) {
                is Result.Success -> {
                    val resp = result.data
                    questionCounter++
                    val avgScore = (resp.scores.clarity + resp.scores.correctness +
                            resp.scores.communication + resp.scores.edge_cases) / 4f
                    sessionDeltaLines.add(
                        "Q$questionCounter (${resp.topic}): avg ${"%.0f".format(avgScore)}/100"
                    )

                    val rubric = RubricState(
                        topic = resp.topic,
                        clarity = resp.scores.clarity,
                        correctness = resp.scores.correctness,
                        communication = resp.scores.communication,
                        edgeCases = resp.scores.edge_cases,
                        feedback = resp.feedback
                    )

                    // Add next question to DB and history
                    repository.addMessage(active.sessionId, "interviewer", resp.next_question)
                    conversationHistory.add(ConversationMessageDto("interviewer", resp.next_question))

                    _phase.value = active.copy(isLoading = false, lastRubric = rubric)
                }
                is Result.Error -> {
                    // Remove the optimistically-added message from history
                    if (conversationHistory.isNotEmpty()) {
                        conversationHistory.removeLastOrNull()
                    }
                    _phase.value = active.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun endSession() {
        val active = _phase.value as? InterviewPhase.Active ?: return
        _phase.value = active.copy(isLoading = true)

        viewModelScope.launch {
            val cfg = _config.value
            val result = repository.wrapSession(
                sessionId = active.sessionId,
                role = cfg.role,
                level = cfg.level,
                context = active.cachedContext,
                sessionDelta = buildSessionDelta()
            )

            when (result) {
                is Result.Success -> {
                    val resp = result.data
                    _phase.value = InterviewPhase.Summary(
                        overallScore = resp.overall_score,
                        strongAreas = resp.strong_areas,
                        weakSpots = resp.weak_spots,
                        summary = resp.summary,
                        nextFocus = resp.next_focus
                    )
                }
                is Result.Error -> {
                    _phase.value = active.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun resetToConfig() {
        _sessionId.value = null
        conversationHistory.clear()
        sessionDeltaLines.clear()
        questionCounter = 0
        _phase.value = InterviewPhase.Config
    }

    fun clearError() {
        val active = _phase.value as? InterviewPhase.Active ?: return
        _phase.value = active.copy(errorMessage = null)
    }

    private fun buildSessionDelta(): String =
        sessionDeltaLines.joinToString("\n")

    companion object {
        fun factory(repository: AppRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    InterviewViewModel(repository) as T
            }
    }
}
