package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val priority: String = "Medium", // High, Medium, Low
    val dueDate: String,
    val estimatedMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val difficulty: String,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSummary: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Alex Rivera",
    val university: String = "Stanford University",
    val major: String = "Computer Science",
    val currentGpa: String = "3.85",
    val streakDays: Int = 5,
    val lastStudyDateEpochDay: Long = 0L,
    val totalHoursStudied: Double = 14.5,
    val totalQuizzesCompleted: Int = 8,
    val totalTasksCompleted: Int = 24
) {
    val effectiveStreak: Int
        get() {
            if (streakDays <= 0) return 0
            if (lastStudyDateEpochDay == 0L) return streakDays
            val today = java.time.LocalDate.now().toEpochDay()
            val diff = today - lastStudyDateEpochDay
            return when {
                diff <= 1 -> streakDays
                else -> 0
            }
        }

    val hasCompletedToday: Boolean
        get() = (lastStudyDateEpochDay == java.time.LocalDate.now().toEpochDay())
}
