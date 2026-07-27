package com.example.data.model

data class Question(
    val id: Int,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class GeneratedQuiz(
    val topic: String,
    val difficulty: String,
    val questions: List<Question>
)
