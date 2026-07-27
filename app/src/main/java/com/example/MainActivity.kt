package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.BottomNavBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.theme.StudyMateTheme
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudyMateTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val tasks by viewModel.tasks.collectAsStateWithLifecycle()
                val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
                val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
                val quizUiState by viewModel.quizUiState.collectAsStateWithLifecycle()
                val quizHistory by viewModel.quizHistory.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                userProfile = userProfile,
                                tasks = tasks,
                                onNavigate = { targetRoute ->
                                    navController.navigate(targetRoute) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(Screen.AIChat.route) {
                            ChatScreen(
                                messages = chatMessages,
                                uiState = chatUiState,
                                onSendMessage = { text, isSummary ->
                                    viewModel.sendChatMessage(text, isSummary)
                                },
                                onClearChat = {
                                    viewModel.clearChat()
                                }
                            )
                        }

                        composable(Screen.QuizGenerator.route) {
                            QuizScreen(
                                quizUiState = quizUiState,
                                quizHistory = quizHistory,
                                onGenerateQuiz = { topic, count, difficulty ->
                                    viewModel.generateQuiz(topic, count, difficulty)
                                },
                                onSelectAnswer = { qIdx, optIdx ->
                                    viewModel.selectQuizAnswer(qIdx, optIdx)
                                },
                                onNextQuestion = { viewModel.nextQuizQuestion() },
                                onPrevQuestion = { viewModel.prevQuizQuestion() },
                                onSubmitQuiz = { viewModel.submitQuiz() },
                                onResetQuiz = { viewModel.resetQuiz() }
                            )
                        }

                        composable(Screen.StudyPlanner.route) {
                            PlannerScreen(
                                tasks = tasks,
                                onAddTask = { title, subject, priority, dueDate, estimatedMins ->
                                    viewModel.addTask(title, subject, priority, dueDate, estimatedMins)
                                },
                                onToggleTask = { task ->
                                    viewModel.toggleTaskCompleted(task)
                                },
                                onDeleteTask = { taskId ->
                                    viewModel.deleteTask(taskId)
                                }
                            )
                        }

                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                userProfile = userProfile,
                                onUpdateProfile = { name, uni, major, gpa ->
                                    viewModel.updateProfile(name, uni, major, gpa)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
