package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["topic_id"])],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "topic_id")
    val topicId: Long,
    val word: String,
    val phonetics: String = "",
    val translation: String = "",
    @ColumnInfo(name = "example_sentence")
    val exampleSentence: String = "",
    @ColumnInfo(name = "is_mastered")
    val isMastered: Boolean = false,
    @ColumnInfo(name = "last_reviewed_at")
    val lastReviewedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
