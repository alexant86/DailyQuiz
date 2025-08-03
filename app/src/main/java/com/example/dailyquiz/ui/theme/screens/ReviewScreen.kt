package com.example.dailyquiz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyquiz.R
import com.example.dailyquiz.data.QuizRepository
import com.example.dailyquiz.data.local.QuizAttempt
import com.example.dailyquiz.data.local.QuizQuestion
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    repository: QuizRepository,
    quizId: Long,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var quizAttempt by remember { mutableStateOf<QuizAttempt?>(null) }

    LaunchedEffect(quizId) {
        scope.launch {
            quizAttempt = repository.getAttemptById(quizId)
            questions = repository.getQuestionsForAttempt(quizId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Результаты",
                        fontSize = 30.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 70.dp, end = 90.dp),

                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF6200EE)
    ) { paddingValues ->
        if (questions.isEmpty() || quizAttempt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ResultInfoSection(
                    correctAnswers = quizAttempt!!.correctAnswers,
                    totalQuestions = questions.size
                )

                Text(
                    text = "Твои ответы",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                QuestionsList(questions)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ResultInfoSection(correctAnswers: Int, totalQuestions: Int) {
    val (title, subtitle) = when (correctAnswers) {
        5 -> "Идеально!" to "5/5 — вы ответили на всё правильно. Это блестящий результат!"
        4 -> "Почти идеально!" to "4/5 — очень близко к совершенству. Ещё один шаг!"
        3 -> "Хороший результат!" to "3/5 — вы на верном пути. Продолжайте тренироваться!"
        2 -> "Есть над чем поработать" to "2/5 — не расстраивайтесь, попробуйте ещё раз!"
        1 -> "Сложный вопрос?" to "1/5 — иногда просто не ваш день. Следующая попытка будет лучше!"
        else -> "Бывает и так!" to "0/5 — не отчаивайтесь. Начните заново и удивите себя!"
    }

    Card(
        shape = RoundedCornerShape(35.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 4.dp)
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
        }
    }
}

@Composable
private fun QuestionsList(questions: List<QuizQuestion>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        questions.forEachIndexed { index, question ->
            QuestionReviewItem(
                question = question,
                questionNumber = index + 1,
                totalQuestions = questions.size
            )
        }
    }
}

@Composable
fun QuestionReviewItem(
    question: QuizQuestion,
    questionNumber: Int,
    totalQuestions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Вопрос $questionNumber из $totalQuestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (question.selectedAnswer != null) {
                    Icon(
                        imageVector = if (question.selectedAnswer == question.correctAnswer)
                            Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (question.selectedAnswer == question.correctAnswer)
                            "Правильный ответ" else "Неправильный ответ",
                        tint = if (question.selectedAnswer == question.correctAnswer)
                            Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = URLDecoder.decode(question.question, StandardCharsets.UTF_8.toString()),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            val allAnswers = (question.incorrectAnswers + question.correctAnswer).shuffled()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                allAnswers.forEach { answer ->
                    val decodedAnswer = URLDecoder.decode(answer, StandardCharsets.UTF_8.toString())

                    val isSelected = answer == question.selectedAnswer
                    val isCorrectAnswer = answer == question.correctAnswer

                    val backgroundColor = when {
                        isSelected && isCorrectAnswer -> Color(0xFFC8E6C9)
                        isSelected && !isCorrectAnswer -> Color(0xFFF8BBD0)
                        !isSelected && isCorrectAnswer -> Color.White
                        else -> Color(0xFFF5F5F5)
                    }

                    val borderColor = when {
                        isSelected && isCorrectAnswer -> Color(0xFF4CAF50)
                        isSelected && !isCorrectAnswer -> Color(0xFFF44336)
                        !isSelected && isCorrectAnswer -> Color(0xFF4CAF50)
                        else -> Color(0xFFE0E0E0)
                    }

                    val textColor = when {
                        isSelected && isCorrectAnswer -> Color(0xFF1B5E20)
                        isSelected && !isCorrectAnswer -> Color(0xFFB71C1C)
                        !isSelected && isCorrectAnswer -> Color(0xFF1B5E20)
                        else -> Color.Black
                    }

                    OutlinedCard(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = decodedAnswer,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected && isCorrectAnswer) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Правильный",
                                    tint = Color(0xFF4CAF50)
                                )
                            } else if (isSelected && !isCorrectAnswer) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Неправильный",
                                    tint = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}