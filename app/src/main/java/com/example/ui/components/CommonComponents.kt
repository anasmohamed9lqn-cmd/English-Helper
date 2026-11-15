package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TopicEntity
import com.example.data.local.WordEntity
import com.example.ui.theme.tr

@Composable
fun LtrContent(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        content()
    }
}

@Composable
fun formatCountdownLabel(topic: TopicEntity, nowMillis: Long): String {
    val nextAt = topic.nextReviewAt
    if (topic.reviewIntervalDays <= 0 || nextAt == null) {
        return tr(R.string.timer_status_off)
    }
    val remaining = nextAt - nowMillis
    if (remaining <= 0L) {
        return tr(R.string.timer_status_due)
    }
    val totalMinutes = (remaining / 60_000L).coerceAtLeast(1L)
    val days = totalMinutes / (60L * 24L)
    val hours = (totalMinutes % (60L * 24L)) / 60L
    val mins = totalMinutes % 60L
    return when {
        days > 0L -> tr(R.string.timer_status_in_days, days.toInt(), hours.toInt())
        hours > 0L -> tr(R.string.timer_status_in_hours, hours.toInt(), mins.toInt())
        else -> tr(R.string.timer_status_in_mins, mins.toInt().coerceAtLeast(1))
    }
}

fun isTopicDue(topic: TopicEntity, nowMillis: Long): Boolean {
    val nextAt = topic.nextReviewAt ?: return false
    return topic.reviewIntervalDays > 0 && nextAt <= nowMillis
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicEditorDialog(
    isRename: Boolean,
    initialTitle: String = "",
    initialIntervalDays: Int = 3,
    onDismiss: () -> Unit,
    onConfirm: (title: String, intervalDays: Int) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var showError by remember { mutableStateOf(false) }
    val presets = listOf(0, 1, 2, 3, 7)
    var selectedPreset by remember {
        mutableIntStateOf(if (initialIntervalDays in presets) initialIntervalDays else -1)
    }
    var customDaysText by remember {
        mutableStateOf(
            if (initialIntervalDays !in presets && initialIntervalDays > 0) {
                initialIntervalDays.toString()
            } else {
                ""
            }
        )
    }
    var showCustomError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRename) tr(R.string.dialog_rename_topic_title)
                else tr(R.string.dialog_new_topic_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (showError) showError = false
                    },
                    label = { Text(tr(R.string.field_topic_title)) },
                    placeholder = { Text(tr(R.string.field_topic_title_hint)) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(tr(R.string.error_topic_title_required)) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_title_input"),
                )

                if (!isRename) {
                    Text(
                        text = tr(R.string.label_review_schedule),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val options = listOf(
                            0 to tr(R.string.timer_opt_off),
                            1 to tr(R.string.timer_opt_1d),
                            2 to tr(R.string.timer_opt_2d),
                            3 to tr(R.string.timer_opt_3d),
                            7 to tr(R.string.timer_opt_7d),
                            -1 to tr(R.string.timer_opt_custom),
                        )
                        options.forEach { (value, label) ->
                            FilterChip(
                                selected = selectedPreset == value,
                                onClick = {
                                    selectedPreset = value
                                    showCustomError = false
                                },
                                label = { Text(label) },
                                leadingIcon = if (selectedPreset == value) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                            )
                        }
                    }

                    if (selectedPreset == -1) {
                        OutlinedTextField(
                            value = customDaysText,
                            onValueChange = { raw ->
                                customDaysText = raw.filter { it.isDigit() }.take(3)
                                showCustomError = false
                            },
                            label = { Text(tr(R.string.field_custom_days)) },
                            isError = showCustomError,
                            supportingText = if (showCustomError) {
                                { Text(tr(R.string.error_invalid_days)) }
                            } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        showError = true
                        return@Button
                    }
                    val interval = if (isRename) {
                        initialIntervalDays
                    } else if (selectedPreset >= 0) {
                        selectedPreset
                    } else {
                        val parsed = customDaysText.toIntOrNull()
                        if (parsed == null || parsed !in 1..365) {
                            showCustomError = true
                            return@Button
                        }
                        parsed
                    }
                    onConfirm(trimmedTitle, interval)
                },
                modifier = Modifier.testTag("confirm_topic_button"),
            ) {
                Text(if (isRename) tr(R.string.action_save) else tr(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewTimerDialog(
    currentIntervalDays: Int,
    onDismiss: () -> Unit,
    onSaveInterval: (Int) -> Unit,
) {
    val presets = listOf(0, 1, 2, 3, 7)
    var selectedPreset by remember {
        mutableIntStateOf(if (currentIntervalDays in presets) currentIntervalDays else -1)
    }
    var customDaysText by remember {
        mutableStateOf(
            if (currentIntervalDays !in presets && currentIntervalDays > 0) {
                currentIntervalDays.toString()
            } else {
                "5"
            }
        )
    }
    var showCustomError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = tr(R.string.dialog_timer_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr(R.string.dialog_timer_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val options = listOf(
                        0 to tr(R.string.timer_opt_off),
                        1 to tr(R.string.timer_opt_1d),
                        2 to tr(R.string.timer_opt_2d),
                        3 to tr(R.string.timer_opt_3d),
                        7 to tr(R.string.timer_opt_7d),
                        -1 to tr(R.string.timer_opt_custom),
                    )
                    options.forEach { (value, label) ->
                        FilterChip(
                            selected = selectedPreset == value,
                            onClick = {
                                selectedPreset = value
                                showCustomError = false
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("timer_chip_$value"),
                        )
                    }
                }
                if (selectedPreset == -1) {
                    OutlinedTextField(
                        value = customDaysText,
                        onValueChange = { raw ->
                            customDaysText = raw.filter { it.isDigit() }.take(3)
                            showCustomError = false
                        },
                        label = { Text(tr(R.string.field_custom_days)) },
                        isError = showCustomError,
                        supportingText = if (showCustomError) {
                            { Text(tr(R.string.error_invalid_days)) }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_days_input"),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDays = if (selectedPreset >= 0) {
                        selectedPreset
                    } else {
                        val parsed = customDaysText.toIntOrNull()
                        if (parsed == null || parsed !in 1..365) {
                            showCustomError = true
                            return@Button
                        }
                        parsed
                    }
                    onSaveInterval(finalDays)
                },
                modifier = Modifier.testTag("save_timer_button"),
            ) {
                Text(tr(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun AddEditWordDialog(
    existingWord: WordEntity? = null,
    onSpeak: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (word: String, phonetics: String, translation: String, example: String) -> Unit,
) {
    var word by remember { mutableStateOf(existingWord?.word.orEmpty()) }
    var phonetics by remember { mutableStateOf(existingWord?.phonetics.orEmpty()) }
    var translation by remember { mutableStateOf(existingWord?.translation.orEmpty()) }
    var example by remember { mutableStateOf(existingWord?.exampleSentence.orEmpty()) }
    var showWordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingWord == null) tr(R.string.dialog_add_word_title)
                else tr(R.string.dialog_edit_word_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LtrContent {
                    OutlinedTextField(
                        value = word,
                        onValueChange = {
                            word = it
                            if (showWordError) showWordError = false
                        },
                        label = { Text(tr(R.string.field_word_required)) },
                        placeholder = { Text(tr(R.string.field_word_hint)) },
                        isError = showWordError,
                        supportingText = if (showWordError) {
                            { Text(tr(R.string.error_word_required)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("word_input"),
                    )
                }

                OutlinedButton(
                    onClick = { onSpeak(word) },
                    enabled = word.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_modal_tts_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = tr(R.string.cd_speak_word),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr(R.string.btn_listen_pronunciation))
                }

                LtrContent {
                    OutlinedTextField(
                        value = phonetics,
                        onValueChange = { phonetics = it },
                        label = { Text(tr(R.string.field_phonetics_optional)) },
                        placeholder = { Text(tr(R.string.field_phonetics_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phonetics_input"),
                    )
                }

                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text(tr(R.string.field_translation_optional)) },
                    placeholder = { Text(tr(R.string.field_translation_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("translation_input"),
                )

                LtrContent {
                    OutlinedTextField(
                        value = example,
                        onValueChange = { example = it },
                        label = { Text(tr(R.string.field_example_optional)) },
                        placeholder = { Text(tr(R.string.field_example_hint)) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("example_input"),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = word.trim()
                    if (trimmed.isEmpty()) {
                        showWordError = true
                        return@Button
                    }
                    onSave(trimmed, phonetics.trim(), translation.trim(), example.trim())
                },
                modifier = Modifier.testTag("save_word_button"),
            ) {
                Text(tr(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizSetupDialog(
    totalWords: Int,
    initialPrioritizeUnmastered: Boolean,
    onDismiss: () -> Unit,
    onStartQuiz: (count: Int, prioritizeUnmastered: Boolean) -> Unit,
) {
    var selectedCount by remember { mutableIntStateOf(0) } // 0 = All
    var prioritizeUnmastered by remember { mutableStateOf(initialPrioritizeUnmastered) }

    val presets = buildList {
        add(0) // All
        listOf(5, 10, 15, 20).forEach { count ->
            if (count < totalWords) add(count)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Style,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = tr(R.string.dialog_quiz_setup_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = tr(R.string.dialog_quiz_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { count ->
                        val label = if (count == 0) {
                            tr(R.string.quiz_count_all, totalWords)
                        } else {
                            count.toString()
                        }
                        FilterChip(
                            selected = selectedCount == count,
                            onClick = { selectedCount = count },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("quiz_count_chip_$count"),
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
                    Text(
                        text = tr(R.string.quiz_prioritize_unmastered),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                    )
                    Switch(
                        checked = prioritizeUnmastered,
                        onCheckedChange = { prioritizeUnmastered = it },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartQuiz(selectedCount, prioritizeUnmastered) },
                modifier = Modifier.testTag("begin_quiz_session_button"),
            ) {
                Text(tr(R.string.btn_begin_session))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.testTag("confirm_delete_button"),
            ) {
                Text(tr(R.string.action_confirm_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr(R.string.action_cancel))
            }
        },
    )
}
