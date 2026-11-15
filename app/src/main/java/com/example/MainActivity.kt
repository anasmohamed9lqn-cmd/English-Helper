package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notifications.ReviewAlarmScheduler
import com.example.tts.TtsManager
import com.example.ui.screens.FlashcardQuizScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TopicDetailScreen
import com.example.ui.theme.EnglishHelperTheme
import com.example.ui.viewmodel.FlashcardQuizViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.TopicDetailViewModel

class MainActivity : ComponentActivity() {

    private var deepLinkTopicId by mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkTopicId = intent?.getLongExtra(ReviewAlarmScheduler.EXTRA_TOPIC_ID, -1L) ?: -1L

        setContent {
            val app = application as EnglishHelperApp
            val prefs by app.preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.preferences.AppPreferences()
            )
            EnglishHelperTheme(languageCode = prefs.languageCode) {
                EnglishHelperNavRoot(
                    app = app,
                    deepLinkTopicId = deepLinkTopicId,
                    onConsumeDeepLink = { deepLinkTopicId = -1L },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val topicId = intent.getLongExtra(ReviewAlarmScheduler.EXTRA_TOPIC_ID, -1L)
        if (topicId > 0L) {
            deepLinkTopicId = topicId
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val TOPIC = "topic/{topicId}"
    const val QUIZ = "quiz/{topicId}/{count}/{prioritize}"

    fun topic(topicId: Long) = "topic/$topicId"
    fun quiz(topicId: Long, count: Int, prioritize: Boolean) = "quiz/$topicId/$count/$prioritize"
}

@Composable
private fun EnglishHelperNavRoot(
    app: EnglishHelperApp,
    deepLinkTopicId: Long,
    onConsumeDeepLink: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val ttsManager = remember(context) { TtsManager(context) }

    DisposableEffect(ttsManager) {
        onDispose { ttsManager.shutdown() }
    }

    LaunchedEffect(deepLinkTopicId) {
        if (deepLinkTopicId > 0L) {
            navController.navigate(Routes.topic(deepLinkTopicId)) {
                launchSingleTop = true
            }
            onConsumeDeepLink()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(app.repository)
            )
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                uiState = uiState,
                onSearchQueryChange = homeViewModel::onSearchQueryChange,
                onToggleLanguage = homeViewModel::toggleLanguage,
                onCreateTopic = homeViewModel::createTopic,
                onRenameTopic = homeViewModel::renameTopic,
                onDeleteTopic = homeViewModel::deleteTopic,
                onExportBackupJson = {
                    val result = homeViewModel.exportBackup()
                    result.json to (result.topicCount to result.wordCount)
                },
                onImportBackupJson = { rawJson ->
                    homeViewModel.importBackup(rawJson).map {
                        it.importedTopicCount to it.importedWordCount
                    }
                },
                onOpenTopic = { topicId ->
                    navController.navigate(Routes.topic(topicId))
                },
            )
        }

        composable(
            route = Routes.TOPIC,
            arguments = listOf(navArgument("topicId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getLong("topicId") ?: return@composable
            val topicViewModel: TopicDetailViewModel = viewModel(
                key = "topic_$topicId",
                factory = TopicDetailViewModel.Factory(topicId, app.repository),
            )
            val uiState by topicViewModel.uiState.collectAsStateWithLifecycle()

            TopicDetailScreen(
                uiState = uiState,
                onNavigateBack = {
                    ttsManager.stop()
                    navController.popBackStack()
                },
                onSearchQueryChange = topicViewModel::onSearchQueryChange,
                onRenameTopic = topicViewModel::renameTopic,
                onDeleteTopic = {
                    topicViewModel.deleteTopic {
                        navController.popBackStack()
                    }
                },
                onUpdateReviewTimer = topicViewModel::updateReviewTimer,
                onAddWord = topicViewModel::addWord,
                onUpdateWord = topicViewModel::updateWord,
                onDeleteWord = topicViewModel::deleteWord,
                onToggleWordMastered = topicViewModel::toggleWordMastered,
                onSetPrioritizeUnmastered = topicViewModel::setPrioritizeUnmastered,
                onSpeakText = ttsManager::speak,
                onStartQuiz = { count, prioritize ->
                    navController.navigate(Routes.quiz(topicId, count, prioritize))
                },
            )
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument("topicId") { type = NavType.LongType },
                navArgument("count") { type = NavType.IntType },
                navArgument("prioritize") { type = NavType.BoolType },
            ),
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getLong("topicId") ?: return@composable
            val count = backStackEntry.arguments?.getInt("count") ?: 0
            val prioritize = backStackEntry.arguments?.getBoolean("prioritize") ?: true

            val quizViewModel: FlashcardQuizViewModel = viewModel(
                key = "quiz_${topicId}_${count}_$prioritize",
                factory = FlashcardQuizViewModel.Factory(
                    topicId = topicId,
                    requestedCount = count,
                    prioritizeUnmastered = prioritize,
                    repository = app.repository,
                ),
            )
            val uiState by quizViewModel.uiState.collectAsStateWithLifecycle()

            FlashcardQuizScreen(
                uiState = uiState,
                onClose = {
                    ttsManager.stop()
                    navController.popBackStack()
                },
                onFlipCard = quizViewModel::flipCard,
                onPrevious = quizViewModel::previousCard,
                onNext = quizViewModel::nextCard,
                onToggleMastered = quizViewModel::toggleMasteredCurrentCard,
                onRestart = quizViewModel::restartSession,
                onSpeakText = ttsManager::speak,
            )
        }
    }
}
