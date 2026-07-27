package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.QuizResultEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.GeneratedQuiz
import com.example.data.remote.GeminiService
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Success(val message: String) : ChatUiState()
    data class Error(val error: String) : ChatUiState()
}

sealed class QuizUiState {
    object Idle : QuizUiState()
    object Generating : QuizUiState()
    data class ActiveQuiz(val quiz: GeneratedQuiz, val currentQuestionIndex: Int = 0, val userAnswers: Map<Int, Int> = emptyMap(), val isSubmitted: Boolean = false) : QuizUiState()
    data class Error(val error: String) : QuizUiState()
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository

    init {
        val dao = AppDatabase.getDatabase(application).studyDao()
        val geminiService = GeminiService()
        repository = StudyRepository(dao, geminiService)

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // Tasks
    val tasks: StateFlow<List<StudyTaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTask(title: String, subject: String, priority: String, dueDate: String, estimatedMins: Int) {
        viewModelScope.launch {
            repository.addTask(title, subject, priority, dueDate, estimatedMins)
        }
    }

    fun toggleTaskCompleted(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id, task.isCompleted)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    // Chat State & Actions
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _chatUiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatUiState: StateFlow<ChatUiState> = _chatUiState.asStateFlow()

    fun sendChatMessage(text: String, isSummary: Boolean = false) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _chatUiState.value = ChatUiState.Loading
            try {
                repository.sendChatMessage(text, isSummary)
                _chatUiState.value = ChatUiState.Idle
            } catch (e: Exception) {
                _chatUiState.value = ChatUiState.Error(e.message ?: "Failed to send message")
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    // Quiz State & Actions
    val quizHistory: StateFlow<List<QuizResultEntity>> = repository.quizResults.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _quizUiState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val quizUiState: StateFlow<QuizUiState> = _quizUiState.asStateFlow()

    fun generateQuiz(topic: String, count: Int, difficulty: String) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _quizUiState.value = QuizUiState.Generating
            try {
                val generated = repository.generateQuiz(topic, count, difficulty)
                _quizUiState.value = QuizUiState.ActiveQuiz(quiz = generated)
            } catch (e: Exception) {
                _quizUiState.value = QuizUiState.Error(e.message ?: "Failed to generate quiz")
            }
        }
    }

    fun selectQuizAnswer(questionIndex: Int, optionIndex: Int) {
        val currentState = _quizUiState.value
        if (currentState is QuizUiState.ActiveQuiz && !currentState.isSubmitted) {
            val updatedAnswers = currentState.userAnswers.toMutableMap()
            updatedAnswers[questionIndex] = optionIndex
            _quizUiState.value = currentState.copy(userAnswers = updatedAnswers)
        }
    }

    fun nextQuizQuestion() {
        val currentState = _quizUiState.value
        if (currentState is QuizUiState.ActiveQuiz) {
            if (currentState.currentQuestionIndex < currentState.quiz.questions.size - 1) {
                _quizUiState.value = currentState.copy(currentQuestionIndex = currentState.currentQuestionIndex + 1)
            }
        }
    }

    fun prevQuizQuestion() {
        val currentState = _quizUiState.value
        if (currentState is QuizUiState.ActiveQuiz) {
            if (currentState.currentQuestionIndex > 0) {
                _quizUiState.value = currentState.copy(currentQuestionIndex = currentState.currentQuestionIndex - 1)
            }
        }
    }

    fun submitQuiz() {
        val currentState = _quizUiState.value
        if (currentState is QuizUiState.ActiveQuiz && !currentState.isSubmitted) {
            var score = 0
            val total = currentState.quiz.questions.size
            currentState.quiz.questions.forEachIndexed { index, question ->
                if (currentState.userAnswers[index] == question.correctIndex) {
                    score++
                }
            }
            _quizUiState.value = currentState.copy(isSubmitted = true)

            // Save result to DB
            viewModelScope.launch {
                repository.saveQuizResult(
                    topic = currentState.quiz.topic,
                    score = score,
                    total = total,
                    difficulty = currentState.quiz.difficulty
                )
            }
        }
    }

    fun resetQuiz() {
        _quizUiState.value = QuizUiState.Idle
    }

    // Profile State & Actions
    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfileEntity()
    )

    fun updateProfile(name: String, university: String, major: String, gpa: String) {
        viewModelScope.launch {
            repository.updateProfile(name, university, major, gpa)
        }
    }
}
