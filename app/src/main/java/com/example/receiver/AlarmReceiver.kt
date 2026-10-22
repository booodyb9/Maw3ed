package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.service.AlarmScheduler
import com.example.service.AlarmSoundPlayer
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_OWNER_TYPE = "extra_owner_type" // "appointment" or "medication"
        const val EXTRA_OWNER_ID = "extra_owner_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MawaeeIlaj:AlarmWakeLock"
        )
        wakeLock.acquire(15_000L) // 15 seconds wake lock

        try {
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: NotificationHelper.CHANNEL_APPOINTMENTS
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "تنبيه موعد طبي"
            val content = intent.getStringExtra(EXTRA_CONTENT) ?: "حان موعدك الطبي المحدد"
            val ownerType = intent.getStringExtra(EXTRA_OWNER_TYPE)
            val ownerId = intent.getLongExtra(EXTRA_OWNER_ID, 0L)

            // Start ringing sound & vibration
            AlarmSoundPlayer.play(context)

            // Show alarm notification and full screen intent
            NotificationHelper.createNotificationChannels(context)
            NotificationHelper.showNotification(context, notificationId, channelId, title, content)

            // If this is a repeating appointment or medication, reschedule next occurrence
            if (ownerId > 0L) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AlarmScheduler.rescheduleOwner(context.applicationContext, ownerType, ownerId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
