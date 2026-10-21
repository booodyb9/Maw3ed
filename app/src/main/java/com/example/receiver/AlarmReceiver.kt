package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: NotificationHelper.CHANNEL_APPOINTMENTS
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "تنبيه موعد طبي"
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: "حان موعدك الطبي المحدد"

        NotificationHelper.createNotificationChannels(context)
        NotificationHelper.showNotification(context, notificationId, channelId, title, content)
    }
}
