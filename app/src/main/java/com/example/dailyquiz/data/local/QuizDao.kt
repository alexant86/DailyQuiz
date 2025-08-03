package com.example.dailyquiz.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempt(attempt: QuizAttempt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<QuizQuestion>)

    @Query("SELECT * FROM quiz_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<QuizAttempt>>

    @Query("SELECT * FROM quiz_questions WHERE attemptId = :attemptId")
    suspend fun getQuestionsForAttempt(attemptId: Long): List<QuizQuestion>

    @Query("DELETE FROM quiz_questions WHERE attemptId = :attemptId")
    suspend fun deleteQuestionsForAttempt(attemptId: Long)

    @Query("SELECT * FROM quiz_attempts WHERE id = :attemptId")
    suspend fun getAttemptById(attemptId: Long): QuizAttempt?

    @Query("DELETE FROM quiz_attempts WHERE id = :attemptId")
    suspend fun deleteAttempt(attemptId: Long)
}