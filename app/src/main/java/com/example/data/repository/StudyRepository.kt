package com.example.data.repository

import com.example.data.local.ChatMessageEntity
import com.example.data.local.QuizResultEntity
import com.example.data.local.StudyDao
import com.example.data.local.StudyTaskEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.GeneratedQuiz
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudyRepository(
    private val dao: StudyDao,
    private val geminiService: GeminiService
) {
    // Tasks
    val allTasks: Flow<List<StudyTaskEntity>> = dao.getAllTasks()

    suspend fun addTask(title: String, subject: String, priority: String, dueDate: String, estimatedMins: Int) {
        val task = StudyTaskEntity(
            title = title,
            subject = subject,
            priority = priority,
            dueDate = dueDate,
            estimatedMinutes = estimatedMins,
            isCompleted = false
        )
        dao.insertTask(task)
    }

    suspend fun toggleTaskCompleted(taskId: Long, currentStatus: Boolean) {
        val newStatus = !currentStatus
        dao.setTaskCompleted(taskId, newStatus)
        if (newStatus) {
            recordTaskCompletionAndStreak()
        }
    }

    private suspend fun recordTaskCompletionAndStreak() {
        val currentProfile = dao.getUserProfileOneShot() ?: UserProfileEntity()
        val todayEpochDay = java.time.LocalDate.now().toEpochDay()
        val lastEpoch = currentProfile.lastStudyDateEpochDay

        val newStreak = when {
            lastEpoch == todayEpochDay -> {
                // Already completed a task today, keep current streak count
                if (currentProfile.streakDays > 0) currentProfile.streakDays else 1
            }
            lastEpoch == todayEpochDay - 1 -> {
                // Completed task on consecutive day!
                currentProfile.effectiveStreak + 1
            }
            else -> {
                // Missed a day or first time, start streak at 1
                1
            }
        }

        val updatedProfile = currentProfile.copy(
            totalTasksCompleted = currentProfile.totalTasksCompleted + 1,
            totalHoursStudied = currentProfile.totalHoursStudied + 0.5,
            streakDays = newStreak,
            lastStudyDateEpochDay = todayEpochDay
        )
        dao.insertOrUpdateProfile(updatedProfile)
    }

    suspend fun deleteTask(taskId: Long) {
        dao.deleteTaskById(taskId)
    }

    // Chat
    val chatMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()

    suspend fun sendChatMessage(userText: String, isSummaryRequest: Boolean = false): String {
        // Save user message
        dao.insertChatMessage(
            ChatMessageEntity(sender = "user", text = userText, isSummary = isSummaryRequest)
        )

        // Get AI response
        val aiResponse = if (isSummaryRequest) {
            geminiService.summarizeNotes(userText)
        } else {
            geminiService.askAI(userText)
        }

        // Save AI response
        dao.insertChatMessage(
            ChatMessageEntity(sender = "ai", text = aiResponse, isSummary = isSummaryRequest)
        )

        return aiResponse
    }

    suspend fun clearChatHistory() {
        dao.clearChat()
    }

    // Quiz
    val quizResults: Flow<List<QuizResultEntity>> = dao.getAllQuizResults()

    suspend fun generateQuiz(topic: String, count: Int, difficulty: String): GeneratedQuiz {
        return geminiService.generateQuiz(topic, count, difficulty)
    }

    suspend fun saveQuizResult(topic: String, score: Int, total: Int, difficulty: String) {
        dao.insertQuizResult(
            QuizResultEntity(
                topic = topic,
                score = score,
                totalQuestions = total,
                difficulty = difficulty
            )
        )
        dao.incrementQuizStats()
    }

    // Profile
    val userProfile: Flow<UserProfileEntity> = dao.getUserProfile().map { profile ->
        profile ?: UserProfileEntity()
    }

    suspend fun updateProfile(name: String, university: String, major: String, gpa: String) {
        val current = dao.getUserProfileOneShot() ?: UserProfileEntity()
        dao.insertOrUpdateProfile(
            current.copy(
                name = name,
                university = university,
                major = major,
                currentGpa = gpa
            )
        )
    }

    suspend fun seedSampleDataIfEmpty() {
        val existing = dao.getUserProfileOneShot()
        if (existing == null) {
            val todayEpochDay = java.time.LocalDate.now().toEpochDay()
            dao.insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1,
                    name = "Alex Rivera",
                    university = "Stanford University",
                    major = "Computer Science",
                    currentGpa = "3.85",
                    streakDays = 7,
                    lastStudyDateEpochDay = todayEpochDay,
                    totalHoursStudied = 28.5,
                    totalQuizzesCompleted = 14,
                    totalTasksCompleted = 38
                )
            )
        }
    }
}
