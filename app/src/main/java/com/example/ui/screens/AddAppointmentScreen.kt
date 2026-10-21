package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.model.RepeatRule
import com.example.ui.MawaeeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAppointmentScreen(
    navController: NavController,
    viewModel: MawaeeViewModel
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val currentSelectedProfileId by viewModel.selectedProfileId.collectAsState()

    var title by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    var selectedProfileId by remember(profiles, currentSelectedProfileId) {
        mutableLongStateOf(
            currentSelectedProfileId ?: profiles.firstOrNull()?.id ?: 1L
        )
    }

    var selectedDateTime by remember {
        mutableLongStateOf(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
    }

    var selectedRepeat by remember {
        mutableStateOf(RepeatRule.NONE.name.lowercase())
    }

    val selectedOffsets = remember {
        mutableStateListOf(1440, 60)
    }

    val calendar = remember(selectedDateTime) {
        Calendar.getInstance().apply { timeInMillis = selectedDateTime }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale("ar")) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale("ar")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "إضافة موعد جديد",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("nav_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile selector
            if (profiles.isNotEmpty()) {
                Text(
                    "الملف الشخصي:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    profiles.forEach { p ->
                        FilterChip(
                            selected = selectedProfileId == p.id,
                            onClick = { selectedProfileId = p.id },
                            label = { Text(p.name) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("عنوان الموعد / الكشف الطبي *") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("يرجى كتابة عنوان للموعد") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_appointment_title_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Doctor
            OutlinedTextField(
                value = doctor,
                onValueChange = { doctor = it },
                label = { Text("اسم الطبيب المعالج / العيادة") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_appointment_doctor_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Date & Time pickers
            Text(
                "تاريخ وتوقيت الموعد:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("التاريخ", style = MaterialTheme.typography.labelSmall)
                            Text(
                                dateFormat.format(calendar.time),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
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
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("الوقت", style = MaterialTheme.typography.labelSmall)
                            Text(
                                timeFormat.format(calendar.time),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("الموقع / اسم المستشفى أو العنوان") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_appointment_location_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Reminder Offsets
            Text(
                "التنبيهات المسبقة للموعد:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
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

            // Repeat rule
            Text(
                "تكرار الموعد:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
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
                label = { Text("ملاحظات إضافية (أدوية موصوفة، تحاليل مطلوبة...)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_appointment_notes_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val offsetsStr = selectedOffsets.ifEmpty { listOf(60) }.joinToString(",")
                    viewModel.saveAppointment(
                        profileId = selectedProfileId,
                        title = title,
                        doctor = doctor,
                        dateTime = selectedDateTime,
                        locationText = location,
                        notes = notes,
                        repeat = selectedRepeat,
                        reminderOffsets = offsetsStr
                    ) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_appointment_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "تأكيد وحفظ الموعد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
