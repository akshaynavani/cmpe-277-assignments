package com.example.android.interviewassistant.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.android.interviewassistant.data.local.entity.DailyFaqEntity
import com.example.android.interviewassistant.data.local.entity.FlashcardEntity
import com.example.android.interviewassistant.domain.AppRepository
import com.example.android.interviewassistant.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyUiState(
    // Ask tab
    val question: String = "",
    val topic: String = "",
    val answer: String = "",
    val relatedTopics: List<String> = emptyList(),
    val isAsking: Boolean = false,
    val savedAsFlashcard: Boolean = false,

    // Generate tab
    val notesTopic: String = "",
    val notes: String = "",
    val generatedCards: List<FlashcardEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val cardsSaved: Boolean = false,

    // Daily FAQ tab
    val isFetchingFaqs: Boolean = false,
    val faqFetchError: String? = null,

    // Shared
    val error: String? = null,
    val selectedTab: Int = 0
)

class StudyViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    val dailyFaqs: StateFlow<List<DailyFaqEntity>> = repository.getAllDailyFaqs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuestion(v: String) = _uiState.update { it.copy(question = v, error = null) }
    fun updateTopic(v: String) = _uiState.update { it.copy(topic = v) }
    fun updateNotesTopic(v: String) = _uiState.update { it.copy(notesTopic = v, error = null) }
    fun updateNotes(v: String) = _uiState.update { it.copy(notes = v, error = null) }
    fun selectTab(index: Int) = _uiState.update { it.copy(selectedTab = index) }

    fun askQuestion() {
        val state = _uiState.value
        if (state.question.isBlank()) return
        _uiState.update { it.copy(isAsking = true, answer = "", relatedTopics = emptyList(), savedAsFlashcard = false, error = null) }

        viewModelScope.launch {
            when (val result = repository.askStudy(state.question.trim(), state.topic.ifBlank { null })) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isAsking = false,
                            answer = result.data.answer,
                            relatedTopics = result.data.related_topics,
                            savedAsFlashcard = result.data.save_as_flashcard
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isAsking = false, error = result.message) }
                }
            }
        }
    }

    fun generateFlashcards() {
        val state = _uiState.value
        if (state.notesTopic.isBlank() || state.notes.isBlank()) return
        _uiState.update { it.copy(isGenerating = true, generatedCards = emptyList(), cardsSaved = false, error = null) }

        viewModelScope.launch {
            when (val result = repository.generateFlashcards(state.notes.trim(), state.notesTopic.trim())) {
                is Result.Success -> {
                    _uiState.update { it.copy(isGenerating = false, generatedCards = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isGenerating = false, error = result.message) }
                }
            }
        }
    }

    fun saveGeneratedCards() {
        val cards = _uiState.value.generatedCards
        if (cards.isEmpty()) return
        viewModelScope.launch {
            repository.saveFlashcards(cards)
            _uiState.update { it.copy(cardsSaved = true) }
        }
    }

    /** Manually trigger a daily FAQ refresh (ignores the "already fetched today" guard). */
    fun refreshDailyFaqs(role: String, level: String) {
        if (_uiState.value.isFetchingFaqs) return
        _uiState.update { it.copy(isFetchingFaqs = true, faqFetchError = null) }
        viewModelScope.launch {
            when (val result = repository.fetchAndStoreDailyFaqs(topic = role, role = role, level = level)) {
                is Result.Success -> _uiState.update { it.copy(isFetchingFaqs = false) }
                is Result.Error   -> _uiState.update { it.copy(isFetchingFaqs = false, faqFetchError = result.message) }
            }
        }
    }

    fun saveFaqAsFlashcard(faq: DailyFaqEntity) {
        viewModelScope.launch { repository.saveFaqAsFlashcard(faq) }
    }

    fun clearError() = _uiState.update { it.copy(error = null, faqFetchError = null) }

    companion object {
        fun factory(repository: AppRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    StudyViewModel(repository) as T
            }
    }
}
