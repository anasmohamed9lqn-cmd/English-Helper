package com.example.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.VocabDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class ReviewAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val topicId = intent.getLongExtra(ReviewAlarmScheduler.EXTRA_TOPIC_ID, -1L)
        if (topicId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VocabDatabase.getInstance(context)
                val topic = db.vocabDao().getTopicById(topicId)
                if (topic != null && topic.reviewIntervalDays > 0) {
                    val prefs = UserPreferencesRepository(context).getSnapshot()
                    val scheduler = ReviewAlarmScheduler(context)
                    scheduler.ensureNotificationChannel(prefs.languageCode)

                    val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }

                    if (canNotify) {
                        val locale = Locale.forLanguageTag(prefs.languageCode)
                        val config = Configuration(context.resources.configuration)
                        config.setLocale(locale)
                        val res = context.createConfigurationContext(config).resources

                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(ReviewAlarmScheduler.EXTRA_TOPIC_ID, topic.id)
                        }
                        val contentIntent = PendingIntent.getActivity(
                            context,
                            topic.id.toInt(),
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )

                        val notification = NotificationCompat.Builder(
                            context,
                            ReviewAlarmScheduler.CHANNEL_ID,
                        )
                            .setSmallIcon(R.drawable.ic_launcher_art)
                            .setContentTitle(res.getString(R.string.notif_due_title, topic.title))
                            .setContentText(res.getString(R.string.notif_due_body))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .setContentIntent(contentIntent)
                            .build()

                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(topic.id.toInt(), notification)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
