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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import com.example.data.model.MedicationEntity
import com.example.data.model.ProfileEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationDialog(
    medication: MedicationEntity? = null,
    profiles: List<ProfileEntity>,
    initialProfileId: Long?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        profileId: Long,
        name: String,
        dose: String?,
        times: List<String>,
        startDate: Long,
        endDate: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(medication?.name ?: "") }
    var dose by remember { mutableStateOf(medication?.dose ?: "") }
    var nameError by remember { mutableStateOf(false) }

    var selectedProfileId by remember {
        mutableLongStateOf(
            medication?.profileId
                ?: initialProfileId
                ?: profiles.firstOrNull()?.id
                ?: 1L
        )
    }

    val timesList = remember {
        mutableStateListOf<String>().apply {
            addAll(medication?.parsedTimes() ?: listOf("08:00", "20:00"))
        }
    }

    var startDate by remember {
        mutableLongStateOf(medication?.startDate ?: System.currentTimeMillis())
    }

    var endDate by remember {
        mutableStateOf<Long?>(medication?.endDate)
    }

    val startCal = remember(startDate) {
        Calendar.getInstance().apply { timeInMillis = startDate }
    }

    val endCal = remember(endDate) {
        Calendar.getInstance().apply {
            if (endDate != null) timeInMillis = endDate!!
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale("ar")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (medication == null) "إضافة دواء جديد" else "تعديل الدواء",
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
                // Profile selection
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

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("اسم الدواء / العلاج *") },
                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("يرجى كتابة اسم الدواء") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Dose
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("الجرعة (مثال: قرص بعد الإفطار، 5 مل)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Daily Dose Times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مواعيد الجرعات يوميًا:", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                    if (!timesList.contains(formatted)) {
                                        timesList.add(formatted)
                                        timesList.sort()
                                    }
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة موعد")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    timesList.forEach { timeStr ->
                        InputChip(
                            selected = true,
                            onClick = {},
                            label = { Text(timeStr) },
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { timesList.remove(timeStr) }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف الموعد")
                                }
                            }
                        )
                    }
                }

                // Dates (Start and End)
                Text("مدة العلاج:", style = MaterialTheme.typography.labelLarge)
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
                                        startCal.set(Calendar.YEAR, year)
                                        startCal.set(Calendar.MONTH, month)
                                        startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        startDate = startCal.timeInMillis
                                    },
                                    startCal.get(Calendar.YEAR),
                                    startCal.get(Calendar.MONTH),
                                    startCal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تاريخ البدء", style = MaterialTheme.typography.labelSmall)
                            Text(
                                dateFormat.format(startCal.time),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val c = endCal
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        c.set(Calendar.YEAR, year)
                                        c.set(Calendar.MONTH, month)
                                        c.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        endDate = c.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تاريخ الانتهاء", style = MaterialTheme.typography.labelSmall)
                            Text(
                                if (endDate != null) dateFormat.format(endCal.time) else "مستمر",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (endDate != null) {
                    OutlinedButton(
                        onClick = { endDate = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("جعل العلاج مستمرًا (بدون تاريخ انتهاء)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val finalTimes = if (timesList.isEmpty()) listOf("08:00") else timesList.toList()
                    onSave(
                        medication?.id ?: 0L,
                        selectedProfileId,
                        name,
                        dose,
                        finalTimes,
                        startDate,
                        endDate
                    )
                }
            ) {
                Text("حفظ الدواء")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
