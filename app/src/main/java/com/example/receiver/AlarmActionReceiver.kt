package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.AlarmSoundPlayer
import com.example.service.NotificationHelper

class AlarmActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DISMISS = "com.example.action.DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.example.action.SNOOZE_ALARM"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "تنبيه موعد طبي"
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: "حان الموعد المحدد"
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: NotificationHelper.CHANNEL_APPOINTMENTS

        // Stop the alarm sound and vibration immediately
        AlarmSoundPlayer.stop()

        // Dismiss notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        if (intent.action == ACTION_SNOOZE) {
            // Schedule snooze after 10 minutes
            val snoozeMillis = System.currentTimeMillis() + (10 * 60 * 1000L)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(AlarmReceiver.EXTRA_CHANNEL_ID, channelId)
                putExtra(AlarmReceiver.EXTRA_TITLE, "غفوة: $title")
                putExtra(AlarmReceiver.EXTRA_CONTENT, content)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 500,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val showIntent = Intent(context, com.example.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                0,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(snoozeMillis, showPendingIntent),
                    pendingIntent
                )
            } catch (_: Exception) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeMillis,
                    pendingIntent
                )
            }
        }
    }
}
