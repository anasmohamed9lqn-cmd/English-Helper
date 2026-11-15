package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.WordEntity
import com.example.ui.components.LtrContent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAppResources
import com.example.ui.theme.tr
import com.example.ui.viewmodel.FlashcardQuizUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardQuizScreen(
    uiState: FlashcardQuizUiState,
    onClose: () -> Unit,
    onFlipCard: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleMastered: () -> Unit,
    onRestart: () -> Unit,
    onSpeakText: (String) -> Boolean,
) {
    val appRes = LocalAppResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun speak(text: String) {
        if (!onSpeakText(text)) {
            scope.launch {
                snackbarHostState.showSnackbar(appRes.getString(R.string.msg_tts_unavailable))
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
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("quiz_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = tr(R.string.cd_navigate_back),
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = uiState.topicTitle.ifBlank { tr(R.string.quiz_title) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (uiState.cards.isNotEmpty() && !uiState.isCompleted) {
                            Text(
                                text = tr(
                                    R.string.quiz_card_counter,
                                    uiState.currentIndex + 1,
                                    uiState.cards.size,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                uiState.isCompleted -> {
                    QuizCompletedSummary(
                        uiState = uiState,
                        onBackToTopic = onClose,
                        onRestart = onRestart,
                    )
                }
                else -> {
                    val currentCard = uiState.currentCard
                    if (currentCard != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 600.dp)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            LinearProgressIndicator(
                                progress = {
                                    (uiState.currentIndex + 1).toFloat() / uiState.cards.size.toFloat()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                FlippableFlashcard(
                                    word = currentCard,
                                    isFlipped = uiState.isFlipped,
                                    onFlip = onFlipCard,
                                    onSpeakWord = { speak(currentCard.word) },
                                    onSpeakExample = { speak(currentCard.exampleSentence) },
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Mark as Mastered / Reviewed toggle
                                FilledTonalButton(
                                    onClick = onToggleMastered,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (currentCard.isMastered) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        contentColor = if (currentCard.isMastered) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("quiz_mastered_button"),
                                ) {
                                    Icon(
                                        imageVector = if (currentCard.isMastered) {
                                            Icons.Rounded.CheckCircle
                                        } else {
                                            Icons.Rounded.RadioButtonUnchecked
                                        },
                                        contentDescription = null,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (currentCard.isMastered) {
                                            tr(R.string.btn_mastered_done)
                                        } else {
                                            tr(R.string.btn_mark_mastered)
                                        }
                                    )
                                }

                                // Previous / Next Navigation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = onPrevious,
                                        enabled = uiState.currentIndex > 0,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .testTag("quiz_previous_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = null,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(tr(R.string.btn_previous))
                                    }

                                    val isLast = uiState.currentIndex == uiState.cards.lastIndex
                                    Button(
                                        onClick = onNext,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .testTag("quiz_next_button"),
                                    ) {
                                        Text(
                                            text = if (isLast) tr(R.string.btn_finish)
                                            else tr(R.string.btn_next)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (isLast) {
                                                Icons.Rounded.DoneAll
                                            } else {
                                                Icons.AutoMirrored.Rounded.ArrowForward
                                            },
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlippableFlashcard(
    word: WordEntity,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSpeakWord: () -> Unit,
    onSpeakExample: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "flashcard_flip",
    )

    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFlip,
            )
            .testTag("flashcard_surface"),
    ) {
        if (rotation <= 90f) {
            // FRONT FACE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            )
                        )
                    )
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (word.isMastered) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = tr(R.string.badge_mastered),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        Icon(
                            imageVector = Icons.Rounded.Flip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LtrContent {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            if (word.phonetics.isNotBlank()) {
                                Text(
                                    text = word.phonetics,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        FilledTonalButton(
                            onClick = onSpeakWord,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("quiz_speak_word_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = tr(R.string.cd_speak_word),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tr(R.string.btn_listen_pronunciation))
                        }
                    }

                    Text(
                        text = tr(R.string.quiz_tap_to_reveal),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // BACK FACE (mirrored back so text reads cleanly)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                            )
                        )
                    )
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            LtrContent {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = word.word,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (word.phonetics.isNotBlank()) {
                                        Text(
                                            text = word.phonetics,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = JetBrainsMonoFamily
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = onSpeakWord) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = tr(R.string.cd_speak_word),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = tr(R.string.quiz_label_translation),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = word.translation.ifBlank { tr(R.string.quiz_no_translation) },
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (word.translation.isNotBlank()) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                },
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = tr(R.string.quiz_label_example),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (word.exampleSentence.isNotBlank()) {
                                LtrContent {
                                    Text(
                                        text = "\"${word.exampleSentence}\"",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontStyle = FontStyle.Italic
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSpeakExample,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr(R.string.btn_speak_example))
                                }
                            } else {
                                Text(
                                    text = tr(R.string.quiz_no_example),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }

                    Text(
                        text = tr(R.string.quiz_tap_to_hide),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizCompletedSummary(
    uiState: FlashcardQuizUiState,
    onBackToTopic: () -> Unit,
    onRestart: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .widthIn(max = 520.dp)
            .padding(24.dp)
            .testTag("quiz_completed_summary"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DoneAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp),
                )
            }

            Text(
                text = tr(R.string.quiz_complete_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = tr(
                    R.string.quiz_complete_subtitle,
                    uiState.cards.size,
                    uiState.topicTitle,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ) {
                Text(
                    text = tr(
                        R.string.quiz_complete_mastered_stat,
                        uiState.masteredInSessionCount,
                        uiState.cards.size,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }

            if (uiState.timerWasReset && uiState.reviewIntervalDays > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = tr(R.string.quiz_complete_timer_reset, uiState.reviewIntervalDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onBackToTopic,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("quiz_back_to_topic_button"),
            ) {
                Text(tr(R.string.btn_back_to_topic))
            }

            OutlinedButton(
                onClick = onRestart,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("quiz_restart_button"),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr(R.string.btn_restart_quiz))
            }
        }
    }
}
