package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.TopicEntity
import com.example.data.local.VocabDatabase
import com.example.data.local.WordEntity
import com.example.data.preferences.AppPreferences
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExportResult(
    val json: String,
    val topicCount: Int,
    val wordCount: Int,
)

data class ImportResult(
    val importedTopics: List<TopicEntity>,
    val importedTopicCount: Int,
    val importedWordCount: Int,
    val restoredSettings: BackupSettings,
)

class BackupManager(
    private val database: VocabDatabase,
) {
    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(BackupPayload::class.java).indent("  ")

    suspend fun exportToJson(settings: AppPreferences): ExportResult = withContext(Dispatchers.IO) {
        val dao = database.vocabDao()
        val topics = dao.getAllTopicsOnce()
        val allWords = dao.getAllWordsOnce()
        val wordsByTopic = allWords.groupBy { it.topicId }

        val backupTopics = topics.map { topic ->
            val childWords = wordsByTopic[topic.id].orEmpty().map { w ->
                BackupWord(
                    word = w.word,
                    phonetics = w.phonetics,
                    translation = w.translation,
                    exampleSentence = w.exampleSentence,
                    isMastered = w.isMastered,
                    lastReviewedAt = w.lastReviewedAt,
                    createdAt = w.createdAt,
                )
            }
            BackupTopic(
                title = topic.title,
                createdAt = topic.createdAt,
                reviewIntervalDays = topic.reviewIntervalDays,
                lastReviewedAt = topic.lastReviewedAt,
                nextReviewAt = topic.nextReviewAt,
                words = childWords,
            )
        }

        val payload = BackupPayload(
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            settings = BackupSettings(
                languageCode = settings.languageCode,
                prioritizeUnmastered = settings.prioritizeUnmastered,
            ),
            topics = backupTopics,
        )

        ExportResult(
            json = adapter.toJson(payload),
            topicCount = topics.size,
            wordCount = allWords.size,
        )
    }

    suspend fun importFromJson(rawJson: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = rawJson.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("Empty JSON backup")
            }
            val payload = adapter.fromJson(trimmed)
                ?: throw IllegalArgumentException("Invalid JSON backup payload")

            val dao = database.vocabDao()
            val insertedTopics = mutableListOf<TopicEntity>()
            var totalWordsImported = 0

            database.withTransaction {
                val existingTitles = dao.getAllTopicTitles().toMutableSet()

                for (backupTopic in payload.topics) {
                    val resolvedTitle = resolveUniqueTopicTitle(backupTopic.title, existingTitles)
                    val interval = backupTopic.reviewIntervalDays.coerceIn(0, 365)
                    val now = System.currentTimeMillis()
                    val lastReviewed = backupTopic.lastReviewedAt.takeIf { it > 0L } ?: now
                    val nextReview = when {
                        interval <= 0 -> null
                        backupTopic.nextReviewAt != null -> backupTopic.nextReviewAt
                        else -> lastReviewed + interval * MILLIS_PER_DAY
                    }

                    val topicToInsert = TopicEntity(
                        title = resolvedTitle,
                        createdAt = backupTopic.createdAt.takeIf { it > 0L } ?: now,
                        reviewIntervalDays = interval,
                        lastReviewedAt = lastReviewed,
                        nextReviewAt = nextReview,
                    )
                    val newTopicId = dao.insertTopic(topicToInsert)
                    val savedTopic = topicToInsert.copy(id = newTopicId)
                    insertedTopics.add(savedTopic)

                    val childWords = backupTopic.words.mapNotNull { bw ->
                        val wordText = bw.word.trim()
                        if (wordText.isEmpty()) {
                            null
                        } else {
                            WordEntity(
                                topicId = newTopicId,
                                word = wordText,
                                phonetics = bw.phonetics.trim(),
                                translation = bw.translation.trim(),
                                exampleSentence = bw.exampleSentence.trim(),
                                isMastered = bw.isMastered,
                                lastReviewedAt = bw.lastReviewedAt,
                                createdAt = bw.createdAt.takeIf { it > 0L } ?: now,
                            )
                        }
                    }

                    if (childWords.isNotEmpty()) {
                        dao.insertWords(childWords)
                        totalWordsImported += childWords.size
                    }
                }
            }

            ImportResult(
                importedTopics = insertedTopics,
                importedTopicCount = insertedTopics.size,
                importedWordCount = totalWordsImported,
                restoredSettings = payload.settings,
            )
        }
    }

    companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        fun resolveUniqueTopicTitle(
            rawTitle: String,
            existingTitles: MutableSet<String>,
        ): String {
            val base = rawTitle.trim().ifEmpty { "Imported Topic" }
            if (!existingTitles.contains(base)) {
                existingTitles.add(base)
                return base
            }
            var suffix = 1
            while (true) {
                val candidate = "$base $suffix"
                if (!existingTitles.contains(candidate)) {
                    existingTitles.add(candidate)
                    return candidate
                }
                suffix++
            }
        }
    }
}
