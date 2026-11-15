package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.VocabDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VocabDatabase.getInstance(context)
                val scheduler = ReviewAlarmScheduler(context)
                val topics = db.vocabDao().getAllTopicsOnce()
                topics.forEach { topic ->
                    if (topic.reviewIntervalDays > 0 && topic.nextReviewAt != null) {
                        scheduler.scheduleForTopic(topic)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
