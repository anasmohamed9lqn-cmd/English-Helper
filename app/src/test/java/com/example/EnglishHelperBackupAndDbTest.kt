package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.local.TopicEntity
import com.example.data.local.VocabDatabase
import com.example.data.local.WordEntity
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EnglishHelperBackupAndDbTest {

    private lateinit var database: VocabDatabase
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VocabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = BackupManager(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `export and import appends numbered suffixes for duplicate topics while preserving words`() = runBlocking {
        val dao = database.vocabDao()
        val existingTopicId = dao.insertTopic(
            TopicEntity(
                title = "Travel",
                reviewIntervalDays = 3,
                lastReviewedAt = 1_000L,
                nextReviewAt = 1_000L + 3 * BackupManager.MILLIS_PER_DAY,
            )
        )
        dao.insertWord(
            WordEntity(
                topicId = existingTopicId,
                word = "Itinerary",
                phonetics = "/aɪˈtɪn.ər.er.i/",
                translation = "خط سير الرحلة",
                exampleSentence = "We checked the travel itinerary.",
            )
        )

        // Export current DB to JSON
        val exported = backupManager.exportToJson(AppPreferences(languageCode = "ar"))
        assertEquals(1, exported.topicCount)
        assertEquals(1, exported.wordCount)

        // Import once: "Travel" already exists -> should become "Travel 1"
        val firstImport = backupManager.importFromJson(exported.json).getOrThrow()
        assertEquals(1, firstImport.importedTopicCount)
        assertEquals(1, firstImport.importedWordCount)
        assertEquals("Travel 1", firstImport.importedTopics.first().title)

        // Import again: "Travel" and "Travel 1" exist -> should become "Travel 2"
        val secondImport = backupManager.importFromJson(exported.json).getOrThrow()
        assertEquals("Travel 2", secondImport.importedTopics.first().title)

        val allTopics = dao.getAllTopicsOnce()
        assertEquals(listOf("Travel", "Travel 1", "Travel 2"), allTopics.map { it.title })

        val importedWords = dao.getWordsForTopicOnce(secondImport.importedTopics.first().id)
        assertEquals(1, importedWords.size)
        assertEquals("Itinerary", importedWords.first().word)
        assertEquals("خط سير الرحلة", importedWords.first().translation)
    }

    @Test
    fun `invalid json import fails gracefully without mutating database`() = runBlocking {
        val dao = database.vocabDao()
        dao.insertTopic(TopicEntity(title = "Original"))

        val result = backupManager.importFromJson("{ invalid json")
        assertTrue(result.isFailure)
        assertEquals(1, dao.getAllTopicsOnce().size)
    }
}
