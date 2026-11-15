package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class TopicWithStats(
    @Embedded val topic: TopicEntity,
    @ColumnInfo(name = "word_count")
    val wordCount: Int,
    @ColumnInfo(name = "mastered_count")
    val masteredCount: Int,
)
