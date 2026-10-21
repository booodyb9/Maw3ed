package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.model.AppointmentEntity
import com.example.data.model.MedicationEntity
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {
    private const val MAX_SLOTS = 10
    private const val MED_ID_OFFSET = 100_000

    private val timeFormat = SimpleDateFormat("h:mm a", Locale("ar"))
    private val fullDateFormat = SimpleDateFormat("EEEE d MMMM، h:mm a", Locale("ar"))

    private fun getAlarmManager(context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (canScheduleExact(alarmManager)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
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

    suspend fun scheduleAppointment(context: Context, appointment: AppointmentEntity, profileName: String) {
        cancelAppointment(context, appointment.id)
        val alarmManager = getAlarmManager(context)
        val now = System.currentTimeMillis()
        val offsets = appointment.parsedReminderOffsets().take(MAX_SLOTS)

        for (i in offsets.indices) {
            val offsetMinutes = offsets[i]
            val triggerTime = appointment.dateTime - (offsetMinutes * 60 * 1000L)
            if (triggerTime <= now) continue

            val doctorInfo = if (!appointment.doctor.isNullOrBlank()) " مع ${appointment.doctor}" else ""
            val timeText = fullDateFormat.format(appointment.dateTime)
            val title = "موعد $profileName ${offsetLabel(offsetMinutes)}"
            val content = "${appointment.title}$doctorInfo — $timeText"

            val requestCode = (appointment.id * 10 + i).toInt()
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
                putExtra(AlarmReceiver.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_APPOINTMENTS)
                putExtra(AlarmReceiver.EXTRA_TITLE, title)
                putExtra(AlarmReceiver.EXTRA_CONTENT, content)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setAlarm(alarmManager, triggerTime, pendingIntent)
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

        val times = medication.parsedTimes().take(MAX_SLOTS)
        for (i in times.indices) {
            val timeStr = times[i]
            val parts = timeStr.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val startCal = Calendar.getInstance().apply { timeInMillis = medication.startDate }
            if (calendar.before(startCal)) {
                calendar.timeInMillis = startCal.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
            }

            val doseInfo = if (!medication.dose.isNullOrBlank()) " — ${medication.dose}" else ""
            val title = "موعد دواء $profileName"
            val content = "${medication.name}$doseInfo"

            val requestCode = (MED_ID_OFFSET + medication.id * 10 + i).toInt()
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
                putExtra(AlarmReceiver.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_MEDICATIONS)
                putExtra(AlarmReceiver.EXTRA_TITLE, title)
                putExtra(AlarmReceiver.EXTRA_CONTENT, content)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
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
