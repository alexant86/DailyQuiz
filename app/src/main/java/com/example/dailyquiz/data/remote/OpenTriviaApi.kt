package com.example.dailyquiz.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenTriviaApi {
    @GET("api.php")
    suspend fun getQuizQuestions(
        @Query("amount") amount: Int = 5,
        @Query("type") type: String = "multiple",
        @Query("category") category: Int = 9,
        @Query("difficulty") difficulty: String = "easy"
    ): QuizResponse
}