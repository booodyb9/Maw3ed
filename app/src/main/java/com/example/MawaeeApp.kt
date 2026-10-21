package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.service.AlarmScheduler
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MawaeeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        CoroutineScope(Dispatchers.IO).launch {
            // Warm up database and ensure default profile
            val db = AppDatabase.getInstance(this@MawaeeApp)
            if (db.profileDao().count() == 0) {
                db.profileDao().insert(
                    com.example.data.model.ProfileEntity(name = "أنا", relation = "self")
                )
            }
            // Reschedule active alarms & WorkManager medication schedules
            AlarmScheduler.rescheduleAll(this@MawaeeApp)
            com.example.service.MedicationWorkScheduler.scheduleAllMedications(this@MawaeeApp)
        }
    }
}
