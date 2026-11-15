package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.TopicWithStats
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.TopicEditorDialog
import com.example.ui.components.formatCountdownLabel
import com.example.ui.components.isTopicDue
import com.example.ui.theme.LocalAppResources
import com.example.ui.theme.tr
import com.example.ui.viewmodel.HomeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onCreateTopic: (title: String, intervalDays: Int) -> Unit,
    onRenameTopic: (topicId: Long, newTitle: String) -> Unit,
    onDeleteTopic: (topicId: Long) -> Unit,
    onExportBackupJson: suspend () -> Pair<String, Pair<Int, Int>>,
    onImportBackupJson: suspend (String) -> Result<Pair<Int, Int>>,
    onOpenTopic: (Long) -> Unit,
) {
    val context = LocalContext.current
    val appRes = LocalAppResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSettingsMenu by remember { mutableStateOf(false) }
    var showCreateTopicDialog by remember { mutableStateOf(false) }
    var topicToRename by remember { mutableStateOf<TopicWithStats?>(null) }
    var topicToDelete by remember { mutableStateOf<TopicWithStats?>(null) }

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

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val (json, counts) = onExportBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open output stream")
                }
                counts
            }.onSuccess { (topicsCount, wordsCount) ->
                snackbarHostState.showSnackbar(
                    appRes.getString(R.string.msg_export_success, topicsCount, wordsCount)
                )
            }.onFailure {
                snackbarHostState.showSnackbar(appRes.getString(R.string.msg_export_failed))
            }
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val rawResult = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        it.readText()
                    } ?: error("Empty file")
                }
            }
            rawResult.fold(
                onSuccess = { jsonText ->
                    val importResult = onImportBackupJson(jsonText)
                    importResult.fold(
                        onSuccess = { (topicsCount, wordsCount) ->
                            snackbarHostState.showSnackbar(
                                appRes.getString(R.string.msg_import_success, topicsCount, wordsCount)
                            )
                        },
                        onFailure = {
                            snackbarHostState.showSnackbar(appRes.getString(R.string.msg_import_invalid))
                        },
                    )
                },
                onFailure = {
                    snackbarHostState.showSnackbar(appRes.getString(R.string.msg_import_invalid))
                },
            )
        }
    }

    fun shareBackupFile() {
        scope.launch {
            runCatching {
                val (json, _) = onExportBackupJson()
                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "backups").apply { mkdirs() }
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    File(dir, "english_helper_backup_$stamp.json").apply {
                        writeText(json, Charsets.UTF_8)
                    }
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            }.onSuccess { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, appRes.getString(R.string.menu_share_json))
                )
            }.onFailure {
                snackbarHostState.showSnackbar(appRes.getString(R.string.msg_export_failed))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Column {
                        Text(
                            text = tr(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = tr(R.string.home_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    // Compact Bilingual EN / عربي Toggle Chip
                    Surface(
                        onClick = onToggleLanguage,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("language_toggle_chip"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = tr(R.string.cd_language_toggle),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (uiState.languageCode == "ar") {
                                    tr(R.string.lang_chip_en)
                                } else {
                                    tr(R.string.lang_chip_ar)
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showSettingsMenu = true },
                            modifier = Modifier.testTag("settings_menu_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = tr(R.string.cd_settings_menu),
                            )
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(tr(R.string.menu_switch_language)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Translate, contentDescription = null)
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    onToggleLanguage()
                                },
                                modifier = Modifier.testTag("menu_item_language"),
                            )
                            DropdownMenuItem(
                                text = { Text(tr(R.string.menu_export_json)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                    exportDocumentLauncher.launch("EnglishHelper_Backup_$stamp.json")
                                },
                                modifier = Modifier.testTag("menu_item_export"),
                            )
                            DropdownMenuItem(
                                text = { Text(tr(R.string.menu_share_json)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Share, contentDescription = null)
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    shareBackupFile()
                                },
                                modifier = Modifier.testTag("menu_item_share"),
                            )
                            DropdownMenuItem(
                                text = { Text(tr(R.string.menu_import_json)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    importDocumentLauncher.launch(
                                        arrayOf("application/json", "text/plain", "*/*")
                                    )
                                },
                                modifier = Modifier.testTag("menu_item_import"),
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPerm) {
                                    DropdownMenuItem(
                                        text = { Text(tr(R.string.menu_enable_notifications)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.NotificationsActive,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            showSettingsMenu = false
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateTopicDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(tr(R.string.fab_new_topic)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("new_topic_fab"),
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Due Review Alert Banner
                item(key = "due_banner") {
                    AnimatedVisibility(
                        visible = uiState.dueTopics.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        DueReviewAlertBanner(
                            dueCount = uiState.dueReviewsCount,
                            onReviewNow = {
                                uiState.dueTopics.firstOrNull()?.let { onOpenTopic(it.topic.id) }
                            },
                        )
                    }
                }

                // Stats Dashboard Header
                item(key = "stats_dashboard") {
                    StatsDashboardCard(
                        totalTopics = uiState.totalTopicsCount,
                        totalWords = uiState.totalWordsCount,
                        dueReviews = uiState.dueReviewsCount,
                    )
                }

                // Search Bar
                item(key = "search_bar") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(tr(R.string.search_topics_hint)) },
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
                            .testTag("topic_search_input"),
                    )
                }

                if (uiState.totalTopicsCount == 0 && !uiState.isLoading) {
                    item(key = "empty_topics") {
                        EmptyTopicsState(onCreateFirst = { showCreateTopicDialog = true })
                    }
                } else if (uiState.topics.isEmpty() && !uiState.isLoading) {
                    item(key = "empty_search") {
                        EmptySearchState()
                    }
                } else {
                    item(key = "topics_header") {
                        Text(
                            text = tr(R.string.section_saved_topics),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    items(
                        items = uiState.topics,
                        key = { it.topic.id },
                    ) { topicWithStats ->
                        TopicDashboardCard(
                            item = topicWithStats,
                            nowMillis = uiState.currentTimeMillis,
                            onClick = { onOpenTopic(topicWithStats.topic.id) },
                            onRename = { topicToRename = topicWithStats },
                            onDelete = { topicToDelete = topicWithStats },
                        )
                    }
                }
            }
        }
    }

    if (showCreateTopicDialog) {
        TopicEditorDialog(
            isRename = false,
            onDismiss = { showCreateTopicDialog = false },
            onConfirm = { title, interval ->
                showCreateTopicDialog = false
                onCreateTopic(title, interval)
            },
        )
    }

    topicToRename?.let { target ->
        TopicEditorDialog(
            isRename = true,
            initialTitle = target.topic.title,
            initialIntervalDays = target.topic.reviewIntervalDays,
            onDismiss = { topicToRename = null },
            onConfirm = { newTitle, _ ->
                topicToRename = null
                onRenameTopic(target.topic.id, newTitle)
            },
        )
    }

    topicToDelete?.let { target ->
        ConfirmDeleteDialog(
            title = tr(R.string.dialog_delete_topic_title),
            message = tr(R.string.dialog_delete_topic_message, target.topic.title),
            onDismiss = { topicToDelete = null },
            onConfirm = {
                topicToDelete = null
                onDeleteTopic(target.topic.id)
            },
        )
    }
}

@Composable
private fun DueReviewAlertBanner(
    dueCount: Int,
    onReviewNow: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_due_banner"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (dueCount == 1) tr(R.string.due_banner_title_single)
                    else tr(R.string.due_banner_title_plural, dueCount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = tr(R.string.due_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FilledTonalButton(
                onClick = onReviewNow,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                modifier = Modifier.testTag("banner_review_now_button"),
            ) {
                Text(tr(R.string.action_review_now))
            }
        }
    }
}

@Composable
private fun StatsDashboardCard(
    totalTopics: Int,
    totalWords: Int,
    dueReviews: Int,
) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_dashboard_card"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                        )
                    )
                )
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatCounterColumn(
                    value = totalTopics.toString(),
                    label = tr(R.string.stat_total_topics),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                StatCounterColumn(
                    value = totalWords.toString(),
                    label = tr(R.string.stat_total_words),
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                StatCounterColumn(
                    value = dueReviews.toString(),
                    label = tr(R.string.stat_due_reviews),
                    accentColor = if (dueReviews > 0) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatCounterColumn(
    value: String,
    label: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopicDashboardCard(
    item: TopicWithStats,
    nowMillis: Long,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isDue = isTopicDue(item.topic, nowMillis)
    val countdownLabel = formatCountdownLabel(item.topic, nowMillis)
    val wordCountLabel = if (item.wordCount == 1) {
        tr(R.string.word_count_single)
    } else {
        tr(R.string.word_count_plural, item.wordCount)
    }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("topic_card_${item.topic.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDue) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderCopy,
                        contentDescription = null,
                        tint = if (isDue) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // User-entered Topic title rendered verbatim (never translated or altered)
                    Text(
                        text = item.topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (item.wordCount > 0) {
                            "$wordCountLabel • ${tr(R.string.mastered_count_label, item.masteredCount, item.wordCount)}"
                        } else {
                            wordCountLabel
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("topic_menu_${item.topic.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = tr(R.string.cd_topic_options),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(tr(R.string.action_rename)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
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
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            if (item.wordCount > 0) {
                LinearProgressIndicator(
                    progress = { (item.masteredCount.toFloat() / item.wordCount.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Timer status chip
            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    isDue -> MaterialTheme.colorScheme.tertiaryContainer
                    item.topic.reviewIntervalDays > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = if (item.topic.reviewIntervalDays > 0) {
                            Icons.Rounded.Alarm
                        } else {
                            Icons.Rounded.AlarmOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = when {
                            isDue -> MaterialTheme.colorScheme.tertiary
                            item.topic.reviewIntervalDays > 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = countdownLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isDue -> MaterialTheme.colorScheme.onTertiaryContainer
                            item.topic.reviewIntervalDays > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTopicsState(onCreateFirst: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_study_empty),
                contentDescription = tr(R.string.empty_topics_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Text(
                text = tr(R.string.empty_topics_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = tr(R.string.empty_topics_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onCreateFirst,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("empty_create_topic_button"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr(R.string.empty_topics_cta))
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Text(
            text = tr(R.string.empty_search_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = tr(R.string.empty_search_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
