package com.example.dailyquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dailyquiz.data.QuizRepository
import com.example.dailyquiz.data.local.AppDatabase
import com.example.dailyquiz.data.remote.OpenTriviaApi
import com.example.dailyquiz.ui.screens.HistoryScreen
import com.example.dailyquiz.ui.screens.QuizScreen
import com.example.dailyquiz.ui.screens.ReviewScreen
import com.example.dailyquiz.ui.theme.DailyQuizTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://opentdb.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(OpenTriviaApi::class.java)

        val database = AppDatabase.getDatabase(this)
        val repository = QuizRepository(api, database.quizDao())

        setContent {
            DailyQuizTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(repository)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(repository: QuizRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "quiz") {
        composable("quiz") {
            QuizScreen(
                repository = repository,
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToReview = { quizId -> navController.navigate("review/$quizId") }
            )
        }
        composable("history") {
            HistoryScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { quizId -> navController.navigate("review/$quizId") }
            )
        }
        composable("review/{quizId}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")?.toLong() ?: 0L
            ReviewScreen(
                repository = repository,
                quizId = quizId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}