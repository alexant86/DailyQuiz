package com.example.dailyquiz.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "quiz_attempts")
data class QuizAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val correctAnswers: Int,
    val category: String,
    val difficulty: String
)

@Entity(tableName = "quiz_questions")
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val question: String,
    val correctAnswer: String,
    val incorrectAnswers: List<String>,

    val selectedAnswer: String?
) {


}