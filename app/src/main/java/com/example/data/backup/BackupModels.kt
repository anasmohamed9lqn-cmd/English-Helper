package com.example.data.backup

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: BackupSettings = BackupSettings(),
    val topics: List<BackupTopic> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BackupSettings(
    val languageCode: String = "en",
    val prioritizeUnmastered: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class BackupTopic(
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val reviewIntervalDays: Int = 0,
    val lastReviewedAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long? = null,
    val words: List<BackupWord> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BackupWord(
    val word: String,
    val phonetics: String = "",
    val translation: String = "",
    val exampleSentence: String = "",
    val isMastered: Boolean = false,
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
