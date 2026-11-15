package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {
    @Query(
        """
        SELECT topics.*,
               COUNT(words.id) AS word_count,
               COALESCE(SUM(CASE WHEN words.is_mastered = 1 THEN 1 ELSE 0 END), 0) AS mastered_count
        FROM topics
        LEFT JOIN words ON topics.id = words.topic_id
        GROUP BY topics.id
        ORDER BY
            CASE WHEN topics.next_review_at IS NOT NULL AND topics.next_review_at <= :now THEN 0 ELSE 1 END,
            topics.created_at DESC
        """
    )
    fun observeTopicsWithStats(now: Long): Flow<List<TopicWithStats>>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    fun observeTopicById(topicId: Long): Flow<TopicEntity?>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: Long): TopicEntity?

    @Query("SELECT * FROM topics ORDER BY created_at ASC")
    suspend fun getAllTopicsOnce(): List<TopicEntity>

    @Query("SELECT title FROM topics")
    suspend fun getAllTopicTitles(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("UPDATE topics SET title = :newTitle WHERE id = :topicId")
    suspend fun renameTopic(topicId: Long, newTitle: String)

    @Query(
        """
        UPDATE topics
        SET review_interval_days = :intervalDays,
            last_reviewed_at = :lastReviewedAt,
            next_review_at = :nextReviewAt
        WHERE id = :topicId
        """
    )
    suspend fun updateTopicTimer(
        topicId: Long,
        intervalDays: Int,
        lastReviewedAt: Long,
        nextReviewAt: Long?,
    )

    @Query("DELETE FROM topics WHERE id = :topicId")
    suspend fun deleteTopicById(topicId: Long)

    @Query("SELECT * FROM words WHERE topic_id = :topicId ORDER BY created_at DESC")
    fun observeWordsForTopic(topicId: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE topic_id = :topicId ORDER BY created_at ASC")
    suspend fun getWordsForTopicOnce(topicId: Long): List<WordEntity>

    @Query("SELECT * FROM words ORDER BY created_at ASC")
    suspend fun getAllWordsOnce(): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    fun observeTotalWordCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWord(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWords(words: List<WordEntity>)

    @Update
    suspend fun updateWord(word: WordEntity)

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteWordById(wordId: Long)

    @Query("UPDATE words SET is_mastered = :isMastered, last_reviewed_at = :reviewedAt WHERE id = :wordId")
    suspend fun setWordMastered(wordId: Long, isMastered: Boolean, reviewedAt: Long)
}
