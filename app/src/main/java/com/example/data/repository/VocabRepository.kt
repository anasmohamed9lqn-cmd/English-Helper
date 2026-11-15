package com.example.data.repository

import com.example.data.backup.BackupManager
import com.example.data.backup.ExportResult
import com.example.data.backup.ImportResult
import com.example.data.local.TopicEntity
import com.example.data.local.TopicWithStats
import com.example.data.local.VocabDao
import com.example.data.local.WordEntity
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.notifications.ReviewAlarmScheduler
import kotlinx.coroutines.flow.Flow

class VocabRepository(
    private val dao: VocabDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val backupManager: BackupManager,
    private val alarmScheduler: ReviewAlarmScheduler,
) {
    val preferencesFlow: Flow<AppPreferences> = preferencesRepository.preferencesFlow

    fun observeTopicsWithStats(now: Long): Flow<List<TopicWithStats>> =
        dao.observeTopicsWithStats(now)

    fun observeTotalWordCount(): Flow<Int> = dao.observeTotalWordCount()

    fun observeTopicById(topicId: Long): Flow<TopicEntity?> = dao.observeTopicById(topicId)

    fun observeWordsForTopic(topicId: Long): Flow<List<WordEntity>> =
        dao.observeWordsForTopic(topicId)

    suspend fun getWordsForTopicOnce(topicId: Long): List<WordEntity> =
        dao.getWordsForTopicOnce(topicId)

    suspend fun toggleLanguage() {
        preferencesRepository.toggleLanguage()
    }

    suspend fun setLanguageCode(code: String) {
        preferencesRepository.setLanguageCode(code)
    }

    suspend fun setPrioritizeUnmastered(enabled: Boolean) {
        preferencesRepository.setPrioritizeUnmastered(enabled)
    }

    suspend fun createTopic(title: String, reviewIntervalDays: Int): Long {
        val now = System.currentTimeMillis()
        val interval = reviewIntervalDays.coerceIn(0, 365)
        val nextReview = if (interval > 0) now + interval * BackupManager.MILLIS_PER_DAY else null
        val entity = TopicEntity(
            title = title.trim(),
            createdAt = now,
            reviewIntervalDays = interval,
            lastReviewedAt = now,
            nextReviewAt = nextReview,
        )
        val id = dao.insertTopic(entity)
        val saved = entity.copy(id = id)
        if (interval > 0) {
            alarmScheduler.scheduleForTopic(saved)
        }
        return id
    }

    suspend fun renameTopic(topicId: Long, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        dao.renameTopic(topicId, clean)
        dao.getTopicById(topicId)?.let { updated ->
            if (updated.reviewIntervalDays > 0) {
                alarmScheduler.scheduleForTopic(updated)
            }
        }
    }

    suspend fun deleteTopic(topicId: Long) {
        alarmScheduler.cancelForTopic(topicId)
        dao.deleteTopicById(topicId)
    }

    suspend fun updateTopicReviewTimer(topicId: Long, intervalDays: Int) {
        val cleanInterval = intervalDays.coerceIn(0, 365)
        val now = System.currentTimeMillis()
        val nextReviewAt = if (cleanInterval > 0) {
            now + cleanInterval * BackupManager.MILLIS_PER_DAY
        } else {
            null
        }
        dao.updateTopicTimer(
            topicId = topicId,
            intervalDays = cleanInterval,
            lastReviewedAt = now,
            nextReviewAt = nextReviewAt,
        )
        val updated = dao.getTopicById(topicId)
        if (updated != null && cleanInterval > 0) {
            alarmScheduler.scheduleForTopic(updated)
        } else {
            alarmScheduler.cancelForTopic(topicId)
        }
    }

    suspend fun resetTopicReviewCycleAfterQuiz(topicId: Long) {
        val topic = dao.getTopicById(topicId) ?: return
        val now = System.currentTimeMillis()
        val nextReviewAt = if (topic.reviewIntervalDays > 0) {
            now + topic.reviewIntervalDays * BackupManager.MILLIS_PER_DAY
        } else {
            null
        }
        dao.updateTopicTimer(
            topicId = topicId,
            intervalDays = topic.reviewIntervalDays,
            lastReviewedAt = now,
            nextReviewAt = nextReviewAt,
        )
        if (topic.reviewIntervalDays > 0) {
            alarmScheduler.scheduleForTopic(
                topic.copy(lastReviewedAt = now, nextReviewAt = nextReviewAt)
            )
        }
    }

    suspend fun addWord(
        topicId: Long,
        word: String,
        phonetics: String,
        translation: String,
        exampleSentence: String,
    ): Long {
        val entity = WordEntity(
            topicId = topicId,
            word = word.trim(),
            phonetics = phonetics.trim(),
            translation = translation.trim(),
            exampleSentence = exampleSentence.trim(),
        )
        return dao.insertWord(entity)
    }

    suspend fun updateWord(word: WordEntity) {
        dao.updateWord(
            word.copy(
                word = word.word.trim(),
                phonetics = word.phonetics.trim(),
                translation = word.translation.trim(),
                exampleSentence = word.exampleSentence.trim(),
            )
        )
    }

    suspend fun deleteWord(wordId: Long) {
        dao.deleteWordById(wordId)
    }

    suspend fun setWordMastered(wordId: Long, isMastered: Boolean) {
        dao.setWordMastered(wordId, isMastered, System.currentTimeMillis())
    }

    suspend fun exportBackup(): ExportResult {
        val prefs = preferencesRepository.getSnapshot()
        return backupManager.exportToJson(prefs)
    }

    suspend fun importBackup(rawJson: String): Result<ImportResult> {
        val result = backupManager.importFromJson(rawJson)
        result.onSuccess { imported ->
            imported.importedTopics.forEach { topic ->
                if (topic.reviewIntervalDays > 0 && topic.nextReviewAt != null) {
                    alarmScheduler.scheduleForTopic(topic)
                }
            }
        }
        return result
    }
}
