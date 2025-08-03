package com.example.dailyquiz.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.dailyquiz.R
import com.example.dailyquiz.data.QuizRepository
import com.example.dailyquiz.data.remote.Question
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed class ScreenState {
    object Initial : ScreenState()
    object CategorySelection : ScreenState()
    object Loading : ScreenState()
    object Error : ScreenState()
    object Quiz : ScreenState()
    data class Result(val correctAnswers: Int) : ScreenState()
}

object QuizCategories {
    val categories = mapOf(
        9 to "General Knowledge",
        10 to "Books",
        11 to "Film",
        12 to "Music",
        17 to "Science & Nature",
        18 to "Science: Computers",
        21 to "Sports",
        22 to "Geography",
        23 to "History"
    )
}

object QuizDifficulties {
    val difficulties = listOf("easy", "medium", "hard")
}


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    repository: QuizRepository,
    onNavigateToHistory: () -> Unit,
    onNavigateToReview: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Initial) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var selectedAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }
    var answersOrder by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var correctAnswers by remember { mutableIntStateOf(0) }
    var attemptId by remember { mutableLongStateOf(0L) }
    var remainingTime by remember { mutableIntStateOf(3) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showTimeUpDialog by remember { mutableStateOf(false) }

    val formattedTime by derivedStateOf {
        String.format("%02d:%02d", remainingTime / 60, remainingTime % 60)
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
            if (screenState == ScreenState.Quiz) {
                scope.launch {
                    if (remainingTime == 0) {
                        showTimeUpDialog = true
                        isTimerRunning = false
                    } else {
                        attemptId = repository.saveQuizAttempt(
                            correctAnswers = correctAnswers,
                            questions = questions,
                            selectedAnswers = selectedAnswers,
                            answersOrder = answersOrder,
                            category = questions.firstOrNull()?.category ?: "General Knowledge",
                            difficulty = questions.firstOrNull()?.difficulty ?: "easy"
                        )
                        screenState = ScreenState.Result(correctAnswers)
                        isTimerRunning = false
                    }
                }
            }
        }
    }

    Scaffold(
    ) { padding ->
        when (screenState) {
            is ScreenState.Initial -> InitialState(
                modifier = Modifier.padding(padding),
                onStartQuiz = {
                    screenState = ScreenState.CategorySelection
                },
                onNavigateToHistory = onNavigateToHistory
            )

            is ScreenState.CategorySelection -> CategorySelectionScreen(
                modifier = Modifier.padding(padding),
                onStartQuiz = { category, difficulty ->
                    screenState = ScreenState.Loading
                    scope.launch {
                        try {
                            questions = repository.fetchQuizQuestions(
                                category = category,
                                difficulty = difficulty
                            )
                            selectedAnswers = List(questions.size) { null }
                            answersOrder = questions.map {
                                (it.incorrectAnswers + it.correctAnswer).shuffled()
                            }
                            correctAnswers = 0
                            remainingTime = 300
                            isTimerRunning = true
                            screenState = ScreenState.Quiz
                        } catch (e: Exception) {
                            screenState = ScreenState.Error
                        }
                    }
                },
                onBack = {
                    screenState = ScreenState.Initial
                },
                onNavigateToHistory = onNavigateToHistory
            )

            is ScreenState.Loading -> LoadingState(modifier = Modifier.padding(padding))

            is ScreenState.Error -> ErrorState(
                modifier = Modifier.padding(padding),
                onRetry = { category, difficulty ->
                    screenState = ScreenState.Loading
                    scope.launch {
                        try {
                            questions = repository.fetchQuizQuestions(
                                category = category,
                                difficulty = difficulty
                            )
                            selectedAnswers = List(questions.size) { null }
                            answersOrder = questions.map {
                                (it.incorrectAnswers + it.correctAnswer).shuffled()
                            }
                            correctAnswers = 0
                            remainingTime = 300
                            isTimerRunning = true
                            screenState = ScreenState.Quiz
                        } catch (e: Exception) {
                            screenState = ScreenState.Error
                        }
                    }
                }
            )

            is ScreenState.Quiz -> {
                if (currentQuestionIndex < questions.size) {
                    QuizQuestionState(
                        modifier = Modifier.padding(padding),
                        question = questions[currentQuestionIndex],
                        questionNumber = currentQuestionIndex + 1,
                        totalQuestions = questions.size,
                        selectedAnswer = selectedAnswers[currentQuestionIndex],
                        remainingTime = remainingTime,
                        formattedTime = formattedTime,
                        answers = answersOrder[currentQuestionIndex],
                        onAnswerSelected = { answer ->
                            selectedAnswers = selectedAnswers.toMutableList().apply {
                                set(currentQuestionIndex, answer)
                            }
                            if (answer == questions[currentQuestionIndex].correctAnswer) {
                                correctAnswers++
                            }
                        },
                        onNextQuestion = {
                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                            } else {
                                isTimerRunning = false
                                scope.launch {
                                    attemptId = repository.saveQuizAttempt(
                                        correctAnswers = correctAnswers,
                                        questions = questions,
                                        selectedAnswers = selectedAnswers,
                                        answersOrder = answersOrder,
                                        category = questions.firstOrNull()?.category ?: "General Knowledge",
                                        difficulty = questions.firstOrNull()?.difficulty ?: "easy"
                                    )
                                    screenState = ScreenState.Result(correctAnswers)
                                }
                            }
                        }
                    )
                }
            }

            is ScreenState.Result -> ResultState(
                modifier = Modifier.padding(padding),
                correctAnswers = correctAnswers,
                totalQuestions = questions.size,
                onStartAgain = {
                    screenState = ScreenState.Initial
                    currentQuestionIndex = 0
                    correctAnswers = 0
                    questions = emptyList()
                    selectedAnswers = emptyList()
                    answersOrder = emptyList()
                    remainingTime = 300
                    isTimerRunning = false
                },
                onReviewQuiz = { onNavigateToReview(attemptId) }
            )
        }
    }


    if (showTimeUpDialog) {
        AlertDialog(
            onDismissRequest = {  },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Время вышло!",
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Вы не успели завершить викторину.\nПопробуйте еще раз!",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTimeUpDialog = false
                        screenState = ScreenState.Initial
                        currentQuestionIndex = 0
                        correctAnswers = 0
                        questions = emptyList()
                        selectedAnswers = emptyList()
                        answersOrder = emptyList()
                        remainingTime = 300
                        isTimerRunning = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                ) {
                    Text(
                        text = "НАЧАТЬ ЗАНОВО",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun InitialState(
    modifier: Modifier,
    onStartQuiz: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF6200EE)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .wrapContentSize()
                .padding(top = 45.dp)
                .align(Alignment.TopCenter)
                .clickable { onNavigateToHistory() }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "История",
                    color = Color(0xFF6200EE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Image(
                    painter = painterResource(id = R.drawable.history_icon),
                    contentDescription = "History Icon",
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF6200EE))
                )
            }
        }

        Text(
            text = "DAILYQUIZ",
            fontSize = 100.sp,
            color = Color.White,
            letterSpacing = (-0.1).em,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 300.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Text(
                    text = "Добро пожаловать\nв DailyQuiz!",
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                ) {
                    Text(
                        text = "НАЧАТЬ ВИКТОРИНУ",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelectionScreen(
    modifier: Modifier,
    onStartQuiz: (category: Int, difficulty: String) -> Unit,
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var selectedCategory by remember { mutableIntStateOf(9) }
    var selectedCategoryName by remember { mutableStateOf(QuizCategories.categories[9] ?: "") }
    var selectedDifficulty by remember { mutableStateOf("easy") }

    val categorySheetState = rememberModalBottomSheetState()
    var showCategorySheet by remember { mutableStateOf(false) }

    val difficultySheetState = rememberModalBottomSheetState()
    var showDifficultySheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF6200EE)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White
            )
        }

        Text(
            text = "DAILYQUIZ",
            fontSize = 100.sp,
            color = Color.White,
            letterSpacing = (-0.1).em,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Почти готовы!",
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Осталось выбрать категорию\nи сложность викторины.",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                OutlinedButton(
                    onClick = { showCategorySheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedCategoryName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Arrow Right",
                            tint = Color.Gray
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showDifficultySheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedDifficulty.replaceFirstChar { it.uppercase() },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Arrow Right",
                            tint = Color.Gray
                        )
                    }
                }

                Button(
                    onClick = { onStartQuiz(selectedCategory, selectedDifficulty) },
                    enabled = selectedCategory != 0 && selectedDifficulty.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCategory != 0 && selectedDifficulty.isNotEmpty()) {
                            Color(0xFF6200EE)
                        } else {
                            Color(0xFFE0E0E0)
                        }
                    )
                ) {
                    Text(
                        text = "ДАЛЕЕ",
                        color = if (selectedCategory != 0 && selectedDifficulty.isNotEmpty()) {
                            Color.White
                        } else {
                            Color(0xFF616161)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = categorySheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Выберите категорию",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                QuizCategories.categories.forEach { (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategory = id
                                selectedCategoryName = name
                                showCategorySheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategory == id,
                            onClick = {
                                selectedCategory = id
                                selectedCategoryName = name
                                showCategorySheet = false
                            }
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDifficultySheet) {
        ModalBottomSheet(
            onDismissRequest = { showDifficultySheet = false },
            sheetState = difficultySheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Выберите сложность",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                QuizDifficulties.difficulties.forEach { difficulty ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDifficulty = difficulty
                                showDifficultySheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDifficulty == difficulty,
                            onClick = {
                                selectedDifficulty = difficulty
                                showDifficultySheet = false
                            }
                        )
                        Text(
                            text = difficulty.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier,
    onRetry: (category: Int, difficulty: String) -> Unit
) {
    var selectedCategory by remember { mutableIntStateOf(9) }
    var selectedDifficulty by remember { mutableStateOf("easy") }
    var showCategoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ошибка загрузки вопросов",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { showCategoryDialog = true },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Категория: ${QuizCategories.categories[selectedCategory]}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Сложность:", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuizDifficulties.difficulties.forEach { difficulty ->
                FilterChip(
                    selected = selectedDifficulty == difficulty,
                    onClick = { selectedDifficulty = difficulty },
                    label = { Text(difficulty.replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onRetry(selectedCategory, selectedDifficulty) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Попробовать снова")
        }
        if (showCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                title = { Text("Выберите категорию") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        QuizCategories.categories.forEach { (id, name) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategory = id
                                        showCategoryDialog = false
                                    }
                                    .padding(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedCategory == id,
                                    onClick = {
                                        selectedCategory = id
                                        showCategoryDialog = false
                                    }
                                )
                                Text(text = name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoryDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}

@Composable
private fun QuizQuestionState(
    modifier: Modifier,
    question: Question,
    questionNumber: Int,
    totalQuestions: Int,
    selectedAnswer: String?,
    remainingTime: Int,
    formattedTime: String,
    answers: List<String>,
    onAnswerSelected: (String) -> Unit,
    onNextQuestion: () -> Unit
) {
    var isAnswerChecked by remember { mutableStateOf(false) }
    val isAnswerCorrect = selectedAnswer == question.correctAnswer

    if (isAnswerChecked) {
        LaunchedEffect(Unit) {
            delay(2000)
            onNextQuestion()
            isAnswerChecked = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF6200EE)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "DAILYQUIZ",
            fontSize = 100.sp,
            color = Color.White,
            letterSpacing = (-0.1).em,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
        )

        Card(
            shape = RoundedCornerShape(35.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp)
                .padding(top = 100.dp)
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            remainingTime <= 30 -> Color.Red
                            remainingTime <= 60 -> Color(0xFFFFA500)
                            else -> Color.Black
                        }
                    )
                    LinearProgressIndicator(
                        progress = remainingTime / 300f,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp),
                        color = when {
                            remainingTime <= 30 -> Color.Red
                            remainingTime <= 60 -> Color(0xFFFFA500)
                            else -> Color(0xFF6200EE)
                        },
                        trackColor = Color(0xFFE0E0E0)
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            remainingTime <= 30 -> Color.Red
                            remainingTime <= 60 -> Color(0xFFFFA500)
                            else -> Color.Black
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Вопрос $questionNumber из $totalQuestions",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = URLDecoder.decode(question.question, StandardCharsets.UTF_8.toString()),
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 16.dp)
                )

                answers.forEach { answer ->
                    val isSelected = selectedAnswer == answer
                    val isCorrectAnswer = answer == question.correctAnswer

                    val backgroundColor = when {
                        isAnswerChecked && isSelected && isCorrectAnswer -> Color(0xFFC8E6C9)
                        isAnswerChecked && isSelected && !isCorrectAnswer -> Color(0xFFF8BBD0)
                        isSelected -> Color(0xFFE0E0E0)
                        else -> Color(0xFFF5F5F5)
                    }

                    val borderColor = when {
                        isAnswerChecked && isSelected && isCorrectAnswer -> Color(0xFF4CAF50)
                        isAnswerChecked && isSelected && !isCorrectAnswer -> Color(0xFFF44336)
                        isSelected -> Color(0xFF9E9E9E)
                        else -> Color(0xFFE0E0E0)
                    }

                    val textColor = when {
                        isAnswerChecked && isSelected && isCorrectAnswer -> Color(0xFF1B5E20)
                        isAnswerChecked && isSelected && !isCorrectAnswer -> Color(0xFFB71C1C)
                        else -> Color.Black
                    }

                    OutlinedCard(
                        onClick = { if (!isAnswerChecked) onAnswerSelected(answer) },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = borderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = URLDecoder.decode(answer, StandardCharsets.UTF_8.toString()),
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                            if (isAnswerChecked && isSelected && isCorrectAnswer) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Correct",
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                            if (isAnswerChecked && isSelected && !isCorrectAnswer) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Incorrect",
                                    tint = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { isAnswerChecked = true },
                    enabled = selectedAnswer != null && !isAnswerChecked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAnswer != null) Color(0xFF6200EE) else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = "ДАЛЕЕ",
                        color = if (selectedAnswer != null) Color.White else Color(0xFF616161),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = "Вернуться к предыдущим вопросам нельзя",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}


@Composable
private fun ResultState(
    modifier: Modifier,
    correctAnswers: Int,
    totalQuestions: Int,
    onStartAgain: () -> Unit,
    onReviewQuiz: () -> Unit
) {
    val (title, subtitle) = when (correctAnswers) {
        5 -> "Идеально!" to "5/5 — вы ответили на всё правильно. Это блестящий результат!"
        4 -> "Почти идеально!" to "4/5 — очень близко к совершенству. Ещё один шаг!"
        3 -> "Хороший результат!" to "3/5 — вы на верном пути. Продолжайте тренироваться!"
        2 -> "Есть над чем поработать" to "2/5 — не расстраивайтесь, попробуйте ещё раз!"
        1 -> "Сложный вопрос?" to "1/5 — иногда просто не ваш день. Следующая попытка будет лучше!"
        else -> "Бывает и так!" to "0/5 — не отчаивайтесь. Начните заново и удивите себя!"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF6200EE)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Результаты",
            fontSize = 50.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 75.dp)
        )

        Card(
            shape = RoundedCornerShape(35.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp)
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..totalQuestions) {
                        if (i <= correctAnswers) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.empty_star),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.LightGray)
                            )
                        }
                    }
                }
                Text(
                    text = "$correctAnswers из $totalQuestions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = subtitle,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStartAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                ) {
                    Text(
                        text = "НАЧАТЬ ЗАНОВО",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}