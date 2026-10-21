package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class MedicationReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MED_ID = "med_id"
        const val KEY_MED_NAME = "med_name"
        const val KEY_DOSE = "dose"
        const val KEY_PROFILE_NAME = "profile_name"
        const val KEY_TIME_SLOT = "time_slot"
    }

    override suspend fun doWork(): Result {
        val medId = inputData.getLong(KEY_MED_ID, 0L)
        val medName = inputData.getString(KEY_MED_NAME) ?: "الدواء"
        val dose = inputData.getString(KEY_DOSE)
        val profileName = inputData.getString(KEY_PROFILE_NAME) ?: "المريض"
        val timeSlot = inputData.getString(KEY_TIME_SLOT) ?: ""

        val doseText = if (!dose.isNullOrBlank()) " — $dose" else ""
        val title = "تذكير بموعد دواء: $medName"
        val content = "حان موعد جرعة $medName$doseText لمريض ($profileName)"

        // Ensure channels exist and show notification
        NotificationHelper.createNotificationChannels(context)
        val notificationId = (100_000 + medId * 10 + (timeSlot.hashCode() % 10)).toInt()
        NotificationHelper.showNotification(
            context = context,
            notificationId = notificationId,
            channelId = NotificationHelper.CHANNEL_MEDICATIONS,
            title = title,
            content = content
        )

        return Result.success()
    }
}
