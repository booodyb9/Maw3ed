package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.ProfileEntity
import com.example.ui.MawaeeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: MawaeeViewModel,
    profiles: List<ProfileEntity>,
    onAddAppointment: () -> Unit
) {
    val appointments by viewModel.appointments.collectAsState()
    val medications by viewModel.medications.collectAsState()

    val currentCalendar = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(currentCalendar.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(currentCalendar.get(Calendar.MONTH)) } // 0-based
    var selectedDay by remember { mutableIntStateOf(currentCalendar.get(Calendar.DAY_OF_MONTH)) }

    val monthCalendar = remember(displayedYear, displayedMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthTitleFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("ar")) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale("ar")) }

    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    // In Java Calendar, SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
    // For Arabic calendar starting with Saturday:
    // Saturday = 0, Sunday = 1, Monday = 2, Tuesday = 3, Wednesday = 4, Thursday = 5, Friday = 6
    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK)
    val startOffset = when (firstDayOfWeek) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 0
    }

    // Map days in month to list of appointments (taking into account repeating rules)
    val dayAppointmentsMap = remember(appointments, displayedYear, displayedMonth, daysInMonth) {
        val map = mutableMapOf<Int, MutableList<AppointmentEntity>>()
        for (day in 1..daysInMonth) {
            val currentCheckDay = Calendar.getInstance().apply {
                set(displayedYear, displayedMonth, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            for (a in appointments) {
                val apptStartDay = Calendar.getInstance().apply {
                    timeInMillis = a.dateTime
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (currentCheckDay.timeInMillis < apptStartDay.timeInMillis) continue

                val occurs = when (a.repeatRule()) {
                    com.example.data.model.RepeatRule.NONE -> {
                        val apptCal = Calendar.getInstance().apply { timeInMillis = a.dateTime }
                        apptCal.get(Calendar.YEAR) == displayedYear &&
                                apptCal.get(Calendar.MONTH) == displayedMonth &&
                                apptCal.get(Calendar.DAY_OF_MONTH) == day
                    }
                    com.example.data.model.RepeatRule.DAILY -> true
                    com.example.data.model.RepeatRule.EVERY_OTHER_DAY -> {
                        val daysDiff = ((currentCheckDay.timeInMillis - apptStartDay.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
                        daysDiff >= 0 && daysDiff % 2 == 0
                    }
                    com.example.data.model.RepeatRule.WEEKLY -> {
                        val apptCal = Calendar.getInstance().apply { timeInMillis = a.dateTime }
                        currentCheckDay.get(Calendar.DAY_OF_WEEK) == apptCal.get(Calendar.DAY_OF_WEEK)
                    }
                    com.example.data.model.RepeatRule.MONTHLY -> {
                        val apptCal = Calendar.getInstance().apply { timeInMillis = a.dateTime }
                        day == apptCal.get(Calendar.DAY_OF_MONTH)
                    }
                }
                if (occurs) {
                    map.getOrPut(day) { mutableListOf() }.add(a)
                }
            }
        }
        map
    }

    // Selected day appointments and active medications
    val selectedDayAppointments = dayAppointmentsMap[selectedDay] ?: emptyList()

    val selectedDayMedications = remember(medications, displayedYear, displayedMonth, selectedDay) {
        val currentCheckDay = Calendar.getInstance().apply {
            set(displayedYear, displayedMonth, selectedDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val checkMillis = currentCheckDay.timeInMillis
        medications.filter { med ->
            val startDay = Calendar.getInstance().apply {
                timeInMillis = med.startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (checkMillis < startDay.timeInMillis) return@filter false
            if (med.endDate != null && checkMillis > med.endDate) return@filter false

            when (med.frequencyEnum()) {
                com.example.data.model.MedicationFrequency.DAILY -> true
                com.example.data.model.MedicationFrequency.EVERY_OTHER_DAY -> {
                    val daysDiff = ((checkMillis - startDay.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
                    daysDiff >= 0 && daysDiff % 2 == 0
                }
                com.example.data.model.MedicationFrequency.WEEKLY -> {
                    currentCheckDay.get(Calendar.DAY_OF_WEEK) == startDay.get(Calendar.DAY_OF_WEEK)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Header and Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayedMonth == 0) {
                                    displayedMonth = 11
                                    displayedYear -= 1
                                } else {
                                    displayedMonth -= 1
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "الشهر السابق"
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthTitleFormat.format(monthCalendar.time),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "الشهر التالي"
                            )
                        }
                    }

                    // Quick Jump to Today
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                val today = Calendar.getInstance()
                                displayedYear = today.get(Calendar.YEAR)
                                displayedMonth = today.get(Calendar.MONTH)
                                selectedDay = today.get(Calendar.DAY_OF_MONTH)
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("اليوم", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Weekday headers (Arabic, starting Saturday)
                    val weekDays = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekDays.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Calendar Days Grid
                    val totalSlots = startOffset + daysInMonth
                    val rowsCount = (totalSlots + 6) / 7

                    val todayCal = Calendar.getInstance()
                    val isCurrentMonthAndYear = (todayCal.get(Calendar.YEAR) == displayedYear &&
                            todayCal.get(Calendar.MONTH) == displayedMonth)
                    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rowsCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0 until 7) {
                                    val slotIndex = r * 7 + c
                                    val dayNumber = slotIndex - startOffset + 1
                                    if (dayNumber in 1..daysInMonth) {
                                        val isToday = isCurrentMonthAndYear && (dayNumber == todayDay)
                                        val isSelected = dayNumber == selectedDay
                                        val apptsCount = dayAppointmentsMap[dayNumber]?.size ?: 0

                                        CalendarDayCell(
                                            dayNumber = dayNumber,
                                            isToday = isToday,
                                            isSelected = isSelected,
                                            hasEvents = apptsCount > 0,
                                            eventsCount = apptsCount,
                                            onClick = { selectedDay = dayNumber },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Details Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جدول يوم $selectedDay ${monthTitleFormat.format(monthCalendar.time)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = onAddAppointment,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("موعد جديد")
                }
            }
        }

        // Appointments for selected day
        if (selectedDayAppointments.isEmpty() && selectedDayMedications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "لا توجد مواعيد علاج أو كشوفات مسجلة في هذا اليوم",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Appointments section
            if (selectedDayAppointments.isNotEmpty()) {
                item {
                    Text(
                        "مواعيد الكشف والزيارات الطبية:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(selectedDayAppointments, key = { "appt_${it.id}" }) { a ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    a.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    timeFormat.format(a.dateTime),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!a.doctor.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        a.doctor,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (!a.locationText.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        a.locationText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Medications section
            if (selectedDayMedications.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "الأدوية والجرعات المقررة:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(selectedDayMedications, key = { "med_${it.id}" }) { med ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Medication,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    med.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!med.dose.isNullOrBlank()) {
                                    Text(
                                        med.dose,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    med.parsedTimes().joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasEvents: Boolean,
    eventsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )

            if (hasEvents) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}
