package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.TopicEntity
import com.example.data.local.WordEntity
import com.example.ui.components.AddEditWordDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.LtrContent
import com.example.ui.components.QuizSetupDialog
import com.example.ui.components.ReviewTimerDialog
import com.example.ui.components.TopicEditorDialog
import com.example.ui.components.formatCountdownLabel
import com.example.ui.components.isTopicDue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAppResources
import com.example.ui.theme.tr
import com.example.ui.viewmodel.TopicDetailUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    uiState: TopicDetailUiState,
    onNavigateBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRenameTopic: (String) -> Unit,
    onDeleteTopic: () -> Unit,
    onUpdateReviewTimer: (Int) -> Unit,
    onAddWord: (word: String, phonetics: String, translation: String, example: String) -> Unit,
    onUpdateWord: (WordEntity) -> Unit,
    onDeleteWord: (Long) -> Unit,
    onToggleWordMastered: (WordEntity) -> Unit,
    onSetPrioritizeUnmastered: (Boolean) -> Unit,
    onSpeakText: (String) -> Boolean,
    onStartQuiz: (count: Int, prioritizeUnmastered: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val appRes = LocalAppResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteTopicDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showAddWordDialog by remember { mutableStateOf(false) }
    var wordToEdit by remember { mutableStateOf<WordEntity?>(null) }
    var wordToDelete by remember { mutableStateOf<WordEntity?>(null) }
    var showQuizSetupDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            snackbarHostState.showSnackbar(
                appRes.getString(
                    if (granted) R.string.msg_notifications_enabled
                    else R.string.msg_notifications_denied
                )
            )
        }
    }

    fun speakWithFeedback(text: String) {
        if (text.isBlank()) return
        val ok = onSpeakText(text)
        if (!ok) {
            scope.launch {
                snackbarHostState.showSnackbar(appRes.getString(R.string.msg_tts_unavailable))
            }
        }
    }

    val topic = uiState.topic

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("topic_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = tr(R.string.cd_navigate_back),
                        )
                    }
                },
                title = {
                    // Render exact user-saved Topic title without modification
                    Text(
                        text = topic?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("topic_detail_menu_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = tr(R.string.cd_topic_options),
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(tr(R.string.review_timer_label)) },
                                leadingIcon = { Icon(Icons.Rounded.Alarm, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showTimerDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(tr(R.string.action_rename)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showRenameDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(tr(R.string.action_delete)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteTopicDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddWordDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(tr(R.string.fab_add_word)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_word_fab"),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 104.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (topic != null) {
                    val isDue = isTopicDue(topic, uiState.currentTimeMillis)
                    item(key = "topic_due_banner") {
                        AnimatedVisibility(visible = isDue) {
                            TopicDueAlertCard(topic = topic)
                        }
                    }

                    item(key = "timer_card") {
                        ReviewTimerConfigCard(
                            topic = topic,
                            nowMillis = uiState.currentTimeMillis,
                            onConfigureClick = { showTimerDialog = true },
                        )
                    }

                    item(key = "start_quiz_cta") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { showQuizSetupDialog = true },
                                enabled = uiState.allWords.isNotEmpty(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("start_quiz_button"),
                            ) {
                                Icon(Icons.Rounded.Style, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = tr(R.string.btn_start_quiz),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            if (uiState.allWords.isEmpty()) {
                                Text(
                                    text = tr(R.string.hint_add_words_for_quiz),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                item(key = "words_search") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(tr(R.string.search_words_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = tr(R.string.cd_clear_search),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("words_search_input"),
                    )
                }

                if (uiState.allWords.isEmpty() && !uiState.isLoading) {
                    item(key = "empty_words") {
                        EmptyWordsPlaceholder()
                    }
                } else if (uiState.filteredWords.isEmpty() && !uiState.isLoading) {
                    item(key = "empty_word_search") {
                        Text(
                            text = tr(R.string.empty_search_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                } else {
                    items(
                        items = uiState.filteredWords,
                        key = { it.id },
                    ) { wordItem ->
                        WordListCard(
                            wordEntity = wordItem,
                            onSpeak = { speakWithFeedback(wordItem.word) },
                            onToggleMastered = { onToggleWordMastered(wordItem) },
                            onEdit = { wordToEdit = wordItem },
                            onDelete = { wordToDelete = wordItem },
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog && topic != null) {
        TopicEditorDialog(
            isRename = true,
            initialTitle = topic.title,
            initialIntervalDays = topic.reviewIntervalDays,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle, _ ->
                showRenameDialog = false
                onRenameTopic(newTitle)
            },
        )
    }

    if (showDeleteTopicDialog && topic != null) {
        ConfirmDeleteDialog(
            title = tr(R.string.dialog_delete_topic_title),
            message = tr(R.string.dialog_delete_topic_message, topic.title),
            onDismiss = { showDeleteTopicDialog = false },
            onConfirm = {
                showDeleteTopicDialog = false
                onDeleteTopic()
            },
        )
    }

    if (showTimerDialog && topic != null) {
        ReviewTimerDialog(
            currentIntervalDays = topic.reviewIntervalDays,
            onDismiss = { showTimerDialog = false },
            onSaveInterval = { days ->
                showTimerDialog = false
                onUpdateReviewTimer(days)
                if (days > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
        )
    }

    if (showAddWordDialog) {
        AddEditWordDialog(
            existingWord = null,
            onSpeak = { text -> speakWithFeedback(text) },
            onDismiss = { showAddWordDialog = false },
            onSave = { word, phonetics, translation, example ->
                showAddWordDialog = false
                onAddWord(word, phonetics, translation, example)
            },
        )
    }

    wordToEdit?.let { editing ->
        AddEditWordDialog(
            existingWord = editing,
            onSpeak = { text -> speakWithFeedback(text) },
            onDismiss = { wordToEdit = null },
            onSave = { word, phonetics, translation, example ->
                wordToEdit = null
                onUpdateWord(
                    editing.copy(
                        word = word,
                        phonetics = phonetics,
                        translation = translation,
                        exampleSentence = example,
                    )
                )
            },
        )
    }

    wordToDelete?.let { target ->
        ConfirmDeleteDialog(
            title = tr(R.string.dialog_delete_word_title),
            message = tr(R.string.dialog_delete_word_message, target.word),
            onDismiss = { wordToDelete = null },
            onConfirm = {
                wordToDelete = null
                onDeleteWord(target.id)
            },
        )
    }

    if (showQuizSetupDialog) {
        QuizSetupDialog(
            totalWords = uiState.allWords.size,
            initialPrioritizeUnmastered = uiState.prioritizeUnmastered,
            onDismiss = { showQuizSetupDialog = false },
            onStartQuiz = { count, prioritize ->
                showQuizSetupDialog = false
                onSetPrioritizeUnmastered(prioritize)
                onStartQuiz(count, prioritize)
            },
        )
    }
}

@Composable
private fun TopicDueAlertCard(topic: TopicEntity) {
    val intervalText = if (topic.reviewIntervalDays == 1) {
        tr(R.string.timer_every_day)
    } else {
        tr(R.string.timer_every_days, topic.reviewIntervalDays)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("topic_due_banner"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr(R.string.topic_due_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = tr(R.string.topic_due_banner_subtitle, intervalText),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReviewTimerConfigCard(
    topic: TopicEntity,
    nowMillis: Long,
    onConfigureClick: () -> Unit,
) {
    val intervalSummary = when {
        topic.reviewIntervalDays <= 0 -> tr(R.string.timer_status_off)
        topic.reviewIntervalDays == 1 -> tr(R.string.timer_every_day)
        else -> tr(R.string.timer_every_days, topic.reviewIntervalDays)
    }
    val countdown = formatCountdownLabel(topic, nowMillis)

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${tr(R.string.review_timer_label)}: $intervalSummary",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (topic.reviewIntervalDays > 0) {
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTopicDue(topic, nowMillis)) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            OutlinedButton(
                onClick = onConfigureClick,
                modifier = Modifier.testTag("configure_timer_button"),
            ) {
                Text(tr(R.string.action_configure_timer))
            }
        }
    }
}

@Composable
private fun WordListCard(
    wordEntity: WordEntity,
    onSpeak: () -> Unit,
    onToggleMastered: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_card_${wordEntity.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LtrContent {
                        Text(
                            text = wordEntity.word,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (wordEntity.phonetics.isNotBlank()) {
                            Text(
                                text = wordEntity.phonetics,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                FilledTonalIconButton(
                    onClick = onSpeak,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.testTag("word_tts_button_${wordEntity.id}"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = tr(R.string.cd_speak_word),
                    )
                }
            }

            if (wordEntity.translation.isNotBlank()) {
                Text(
                    text = wordEntity.translation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (wordEntity.exampleSentence.isNotBlank()) {
                LtrContent {
                    Text(
                        text = "\"${wordEntity.exampleSentence}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    onClick = onToggleMastered,
                    shape = RoundedCornerShape(50),
                    color = if (wordEntity.isMastered) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.testTag("word_mastered_toggle_${wordEntity.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = if (wordEntity.isMastered) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (wordEntity.isMastered) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = tr(R.string.badge_mastered),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (wordEntity.isMastered) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("word_edit_button_${wordEntity.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = tr(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("word_delete_button_${wordEntity.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = tr(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWordsPlaceholder() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Style,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
            Text(
                text = tr(R.string.empty_words_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = tr(R.string.empty_words_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
