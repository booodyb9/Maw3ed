package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.model.AppointmentEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.RepeatRule
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {
    private const val MAX_SLOTS = 10
    private const val MED_ID_OFFSET = 100_000

    private val fullDateFormat = SimpleDateFormat("EEEE d MMMM، h:mm a", Locale("ar"))

    private fun getAlarmManager(context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun setAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Using setAlarmClock guarantees exact wakeup even in deep Doze mode
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (_: SecurityException) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun offsetLabel(minutes: Int): String {
        return when {
            minutes <= 0 -> "الآن"
            minutes < 60 -> "بعد $minutes دقيقة"
            minutes < 1440 -> "بعد ${minutes / 60} ساعة"
            else -> "بعد ${minutes / 1440} يوم"
        }
    }

    /**
     * Calculates the next occurrence of an appointment based on its repeat rule.
     */
    fun calculateNextOccurrence(baseDateTime: Long, repeatRule: RepeatRule, afterTimeMillis: Long): Long {
        if (repeatRule == RepeatRule.NONE) return baseDateTime
        if (baseDateTime > afterTimeMillis) return baseDateTime

        val cal = Calendar.getInstance().apply { timeInMillis = baseDateTime }
        while (cal.timeInMillis <= afterTimeMillis) {
            when (repeatRule) {
                RepeatRule.NONE -> break
                RepeatRule.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RepeatRule.EVERY_OTHER_DAY -> cal.add(Calendar.DAY_OF_YEAR, 2)
                RepeatRule.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatRule.MONTHLY -> cal.add(Calendar.MONTH, 1)
            }
        }
        return cal.timeInMillis
    }

    suspend fun scheduleAppointment(context: Context, appointment: AppointmentEntity, profileName: String) {
        cancelAppointment(context, appointment.id)
        val alarmManager = getAlarmManager(context)
        val now = System.currentTimeMillis()
        val repeatRule = appointment.repeatRule()

        // Find upcoming appointment occurrence
        val targetOccurrence = calculateNextOccurrence(appointment.dateTime, repeatRule, now)
        val offsets = appointment.parsedReminderOffsets().take(MAX_SLOTS)

        for (i in offsets.indices) {
            val offsetMinutes = offsets[i]
            val triggerTime = targetOccurrence - (offsetMinutes * 60 * 1000L)
            if (triggerTime <= now) continue

            val doctorInfo = if (!appointment.doctor.isNullOrBlank()) " مع ${appointment.doctor}" else ""
            val timeText = fullDateFormat.format(targetOccurrence)
            val title = "موعد $profileName ${offsetLabel(offsetMinutes)}"
            val content = "${appointment.title}$doctorInfo — $timeText"

            val requestCode = (appointment.id * 10 + i).toInt()
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
                putExtra(AlarmReceiver.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_APPOINTMENTS)
                putExtra(AlarmReceiver.EXTRA_TITLE, title)
                putExtra(AlarmReceiver.EXTRA_CONTENT, content)
                putExtra(AlarmReceiver.EXTRA_OWNER_TYPE, "appointment")
                putExtra(AlarmReceiver.EXTRA_OWNER_ID, appointment.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setAlarm(context, alarmManager, triggerTime, pendingIntent)
        }
    }

    fun cancelAppointment(context: Context, appointmentId: Long) {
        val alarmManager = getAlarmManager(context)
        for (i in 0 until MAX_SLOTS) {
            val requestCode = (appointmentId * 10 + i).toInt()
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    suspend fun scheduleMedication(context: Context, medication: MedicationEntity, profileName: String) {
        cancelMedication(context, medication.id)
        val alarmManager = getAlarmManager(context)
        val now = System.currentTimeMillis()

        if (medication.endDate != null && medication.endDate < now) {
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

        val times = medication.parsedTimes().take(MAX_SLOTS)
        for (i in times.indices) {
            val timeStr = times[i]
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

            // Adjust target date according to frequency (every other day or weekly)
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

            if (medication.endDate != null && targetCal.timeInMillis > medication.endDate) {
                continue
            }

            val doseInfo = if (!medication.dose.isNullOrBlank()) " — ${medication.dose}" else ""
            val freqLabel = if (isEveryOtherDay) " (يوم بعد يوم)" else ""
            val title = "موعد دواء $profileName$freqLabel"
            val content = "${medication.name}$doseInfo"

            val requestCode = (MED_ID_OFFSET + medication.id * 10 + i).toInt()
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
                putExtra(AlarmReceiver.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_MEDICATIONS)
                putExtra(AlarmReceiver.EXTRA_TITLE, title)
                putExtra(AlarmReceiver.EXTRA_CONTENT, content)
                putExtra(AlarmReceiver.EXTRA_OWNER_TYPE, "medication")
                putExtra(AlarmReceiver.EXTRA_OWNER_ID, medication.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setAlarm(context, alarmManager, targetCal.timeInMillis, pendingIntent)
        }
    }

    fun cancelMedication(context: Context, medicationId: Long) {
        val alarmManager = getAlarmManager(context)
        for (i in 0 until MAX_SLOTS) {
            val requestCode = (MED_ID_OFFSET + medicationId * 10 + i).toInt()
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    suspend fun rescheduleOwner(context: Context, ownerType: String?, ownerId: Long) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        if (ownerType == "appointment") {
            val appt = db.appointmentDao().getAppointmentById(ownerId)
            if (appt != null) {
                val profileName = db.profileDao().getProfileById(appt.profileId)?.name ?: "المستخدم"
                scheduleAppointment(context, appt, profileName)
            }
        } else if (ownerType == "medication") {
            val med = db.medicationDao().getMedicationById(ownerId)
            if (med != null) {
                val profileName = db.profileDao().getProfileById(med.profileId)?.name ?: "المستخدم"
                scheduleMedication(context, med, profileName)
            }
        }
    }

    suspend fun rescheduleAll(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val profiles = db.profileDao().getAllProfiles().associateBy { it.id }
        val appointments = db.appointmentDao().getAllAppointments()
        val medications = db.medicationDao().getAllMedications()

        for (a in appointments) {
            val pName = profiles[a.profileId]?.name ?: "المستخدم"
            scheduleAppointment(context, a, pName)
        }

        for (m in medications) {
            val pName = profiles[m.profileId]?.name ?: "المستخدم"
            scheduleMedication(context, m, pName)
        }
    }
}
