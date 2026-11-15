package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import com.example.R
import com.example.data.local.TopicEntity
import java.util.Locale

class ReviewAlarmScheduler(private val context: Context) {

    fun ensureNotificationChannel(languageCode: String = "en") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val res = localizedResources(languageCode)
            val channel = NotificationChannel(
                CHANNEL_ID,
                res.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = res.getString(R.string.notif_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleForTopic(topic: TopicEntity) {
        val triggerAt = topic.nextReviewAt
        if (topic.reviewIntervalDays <= 0 || triggerAt == null) {
            cancelForTopic(topic.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReviewAlarmReceiver::class.java).apply {
            action = ACTION_REVIEW_DUE
            putExtra(EXTRA_TOPIC_ID, topic.id)
            putExtra(EXTRA_TOPIC_TITLE, topic.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            topic.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val safeTrigger = maxOf(triggerAt, System.currentTimeMillis() + 5_000L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
        }
    }

    fun cancelForTopic(topicId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReviewAlarmReceiver::class.java).apply {
            action = ACTION_REVIEW_DUE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            topicId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun localizedResources(languageCode: String) = run {
        val locale = Locale.forLanguageTag(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config).resources
    }

    companion object {
        const val CHANNEL_ID = "vocab_review_reminders"
        const val ACTION_REVIEW_DUE = "com.example.action.REVIEW_DUE"
        const val EXTRA_TOPIC_ID = "extra_topic_id"
        const val EXTRA_TOPIC_TITLE = "extra_topic_title"
    }
}
