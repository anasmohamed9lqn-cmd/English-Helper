package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "review_interval_days")
    val reviewIntervalDays: Int = 0,
    @ColumnInfo(name = "last_reviewed_at")
    val lastReviewedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Long? = null,
)
