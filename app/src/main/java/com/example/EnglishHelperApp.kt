package com.example

import android.app.Application
import com.example.data.backup.BackupManager
import com.example.data.local.VocabDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.VocabRepository
import com.example.notifications.ReviewAlarmScheduler

class EnglishHelperApp : Application() {
    val database: VocabDatabase by lazy { VocabDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val alarmScheduler: ReviewAlarmScheduler by lazy { ReviewAlarmScheduler(this) }
    val backupManager: BackupManager by lazy { BackupManager(database) }
    val repository: VocabRepository by lazy {
        VocabRepository(
            dao = database.vocabDao(),
            preferencesRepository = preferencesRepository,
            backupManager = backupManager,
            alarmScheduler = alarmScheduler,
        )
    }

    override fun onCreate() {
        super.onCreate()
        alarmScheduler.ensureNotificationChannel()
    }
}
