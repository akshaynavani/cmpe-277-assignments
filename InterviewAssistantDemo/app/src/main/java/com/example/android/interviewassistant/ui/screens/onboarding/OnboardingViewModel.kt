package com.example.android.interviewassistant.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.android.interviewassistant.domain.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val role: String = "Software Engineer",
    val level: String = "junior",
    val targetCompany: String = "",
    val interviewDate: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val nameError: String? = null
)

class OnboardingViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateName(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun updateRole(value: String) = _uiState.update { it.copy(role = value) }
    fun updateLevel(value: String) = _uiState.update { it.copy(level = value) }
    fun updateTargetCompany(value: String) = _uiState.update { it.copy(targetCompany = value) }
    fun updateInterviewDate(millis: Long) = _uiState.update { it.copy(interviewDate = millis) }

    fun submit() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val role = state.role.ifBlank { "Software Engineer" }
            val level = state.level
            repository.saveProfile(
                name = state.name.trim(),
                role = role,
                level = level,
                targetCompany = state.targetCompany.trim(),
                interviewDate = state.interviewDate
            )
            repository.seedDemoData(role, level)
            _uiState.update { it.copy(isLoading = false, isComplete = true) }
        }
    }

    companion object {
        fun factory(repository: AppRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OnboardingViewModel(repository) as T
            }
    }
}
