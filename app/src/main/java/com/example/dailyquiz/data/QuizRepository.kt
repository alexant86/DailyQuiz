package com.example.dailyquiz.data

import com.example.dailyquiz.data.local.QuizAttempt
import com.example.dailyquiz.data.local.QuizDao
import com.example.dailyquiz.data.local.QuizQuestion
import com.example.dailyquiz.data.remote.OpenTriviaApi
import com.example.dailyquiz.data.remote.Question
import kotlinx.coroutines.flow.Flow

class QuizRepository(
    private val api: OpenTriviaApi,
    private val quizDao: QuizDao
) {
    suspend fun fetchQuizQuestions(category: Int = 9, difficulty: String = "easy"): List<Question> {
        val response = api.getQuizQuestions(category = category, difficulty = difficulty)
        return response.results
    }

    suspend fun saveQuizAttempt(
        correctAnswers: Int,
        questions: List<Question>,
        selectedAnswers: List<String?>,
        category: String,
        difficulty: String,
        answersOrder: List<List<String>>
    ): Long {
        val timestamp = System.currentTimeMillis()
        val attempt = QuizAttempt(
            timestamp = timestamp,
            correctAnswers = correctAnswers,
            category = category,
            difficulty = difficulty
        )
        val attemptId = quizDao.insertQuizAttempt(attempt)

        val quizQuestions = questions.mapIndexed { index, question ->
            QuizQuestion(
                attemptId = attemptId,
                question = question.question,
                correctAnswer = question.correctAnswer,
                incorrectAnswers = question.incorrectAnswers,
                selectedAnswer = selectedAnswers[index]
            )
        }
        quizDao.insertQuizQuestions(quizQuestions)

        return attemptId
    }

    fun getAllAttempts(): Flow<List<QuizAttempt>> {
        return quizDao.getAllAttempts()
    }

    suspend fun getAttemptById(attemptId: Long): QuizAttempt? {
        return quizDao.getAttemptById(attemptId)
    }

    suspend fun getQuestionsForAttempt(attemptId: Long): List<QuizQuestion> {
        return quizDao.getQuestionsForAttempt(attemptId)
    }

    suspend fun deleteAttempt(attemptId: Long) {
        quizDao.deleteQuestionsForAttempt(attemptId)
        quizDao.deleteAttempt(attemptId)
    }
}