package com.example.service

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.data.model.MedicationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

object MedicationWorkScheduler {

    fun scheduleMedication(context: Context, medication: MedicationEntity, profileName: String) {
        val workManager = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()

        if (medication.endDate != null && medication.endDate < now) {
            cancelMedication(context, medication.id)
            return
        }

        val isEveryOtherDay = medication.isEveryOtherDay()
        val isWeekly = medication.isWeekly()

        val startCal = Calendar.getInstance().apply {
            timeInMillis = medication.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val times = medication.parsedTimes()
        for ((index, timeStr) in times.withIndex()) {
            val parts = timeStr.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (targetCal.timeInMillis <= now) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (isEveryOtherDay) {
                while (true) {
                    val daysDiff = ((targetCal.timeInMillis - startCal.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
                    if (daysDiff >= 0 && daysDiff % 2 == 0) {
                        break
                    }
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else if (isWeekly) {
                val startDayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
                while (targetCal.get(Calendar.DAY_OF_WEEK) != startDayOfWeek) {
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val delayMillis = (targetCal.timeInMillis - now).coerceAtLeast(0L)

            val inputData = Data.Builder()
                .putLong(MedicationReminderWorker.KEY_MED_ID, medication.id)
                .putString(MedicationReminderWorker.KEY_MED_NAME, medication.name)
                .putString(MedicationReminderWorker.KEY_DOSE, medication.dose)
                .putString(MedicationReminderWorker.KEY_PROFILE_NAME, profileName)
                .putString(MedicationReminderWorker.KEY_TIME_SLOT, timeStr)
                .build()

            val intervalHours = when {
                isEveryOtherDay -> 48L
                isWeekly -> 168L
                else -> 24L
            }

            val periodicWork = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
                intervalHours, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("medication_${medication.id}")
                .build()

            val uniqueWorkName = "med_work_${medication.id}_$index"
            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWork
            )
        }
    }

    fun cancelMedication(context: Context, medicationId: Long) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("medication_$medicationId")
    }

    suspend fun scheduleAllMedications(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val profiles = db.profileDao().getAllProfiles().associateBy { it.id }
        val medications = db.medicationDao().getAllMedications()
        for (med in medications) {
            val pName = profiles[med.profileId]?.name ?: "المريض"
            scheduleMedication(context, med, pName)
        }
    }
}
