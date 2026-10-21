package com.example.ui.dialogs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.AppointmentEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.RepeatRule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentDialog(
    appointment: AppointmentEntity? = null,
    profiles: List<ProfileEntity>,
    initialProfileId: Long?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        profileId: Long,
        title: String,
        doctor: String?,
        dateTime: Long,
        location: String?,
        notes: String?,
        repeat: String,
        reminderOffsets: String
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(appointment?.title ?: "") }
    var doctor by remember { mutableStateOf(appointment?.doctor ?: "") }
    var location by remember { mutableStateOf(appointment?.locationText ?: "") }
    var notes by remember { mutableStateOf(appointment?.notes ?: "") }
    var titleError by remember { mutableStateOf(false) }

    var selectedProfileId by remember {
        mutableLongStateOf(
            appointment?.profileId
                ?: initialProfileId
                ?: profiles.firstOrNull()?.id
                ?: 1L
        )
    }

    var selectedDateTime by remember {
        mutableLongStateOf(
            appointment?.dateTime ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
        )
    }

    var selectedRepeat by remember {
        mutableStateOf(appointment?.repeat ?: RepeatRule.NONE.name.lowercase())
    }

    val selectedOffsets = remember {
        mutableStateListOf<Int>().apply {
            addAll(appointment?.parsedReminderOffsets() ?: listOf(1440, 60))
        }
    }

    val calendar = remember(selectedDateTime) {
        Calendar.getInstance().apply { timeInMillis = selectedDateTime }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale("ar")) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale("ar")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (appointment == null) "إضافة موعد جديد" else "تعديل الموعد",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile selector if multiple profiles exist
                if (profiles.isNotEmpty()) {
                    Text("الملف الشخصي:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        profiles.forEach { p ->
                            FilterChip(
                                selected = selectedProfileId == p.id,
                                onClick = { selectedProfileId = p.id },
                                label = { Text(p.name) }
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("عنوان الموعد / الكشف *") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("يرجى إدخال عنوان الموعد") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Doctor Input
                OutlinedTextField(
                    value = doctor,
                    onValueChange = { doctor = it },
                    label = { Text("اسم الطبيب / العيادة") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Date & Time pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        selectedDateTime = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("التاريخ", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    dateFormat.format(calendar.time),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        selectedDateTime = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("الوقت", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    timeFormat.format(calendar.time),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Location Input
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("المكان / المستشفى") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Reminder offsets
                Text(
                    "التنبيهات المسبقة:",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val availableOffsets = listOf(
                        1440 to "قبل يوم",
                        120 to "قبل ساعتين",
                        60 to "قبل ساعة",
                        30 to "قبل ٣٠ دقيقة",
                        0 to "في نفس الوقت"
                    )
                    availableOffsets.forEach { (minutes, label) ->
                        val isSelected = selectedOffsets.contains(minutes)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedOffsets.remove(minutes)
                                else selectedOffsets.add(minutes)
                            },
                            label = { Text(label) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.NotificationsActive, contentDescription = null) }
                            } else null
                        )
                    }
                }

                // Repeat Rule
                Text("التكرار:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatRule.entries.forEach { r ->
                        FilterChip(
                            selected = selectedRepeat.equals(r.name, ignoreCase = true),
                            onClick = { selectedRepeat = r.name.lowercase() },
                            label = { Text(r.displayName()) }
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val offsetsStr = selectedOffsets.ifEmpty { listOf(60) }.joinToString(",")
                    onSave(
                        appointment?.id ?: 0L,
                        selectedProfileId,
                        title,
                        doctor,
                        selectedDateTime,
                        location,
                        notes,
                        selectedRepeat,
                        offsetsStr
                    )
                }
            ) {
                Text("حفظ الموعد")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
