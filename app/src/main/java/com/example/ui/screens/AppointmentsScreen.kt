package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppointmentEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.RepeatRule
import com.example.ui.MawaeeViewModel
import com.example.ui.dialogs.AppointmentDialog
import com.example.ui.dialogs.AttachmentDialog
import com.example.ui.utils.MapLauncher
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AppointmentsScreen(
    viewModel: MawaeeViewModel,
    profiles: List<ProfileEntity>,
    onAddClick: () -> Unit
) {
    val appointments by viewModel.appointments.collectAsState()
    val profilesMap = remember(profiles) { profiles.associateBy { it.id } }

    var appointmentToEdit by remember { mutableStateOf<AppointmentEntity?>(null) }
    var appointmentToDelete by remember { mutableStateOf<AppointmentEntity?>(null) }
    var attachmentTarget by remember { mutableStateOf<AppointmentEntity?>(null) }

    if (appointments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.EventNote,
                    contentDescription = null,
                    modifier = Modifier.height(72.dp).width(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Text(
                    "لا توجد مواعيد مسجلة حاليًا",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "اضغط على زر الإضافة لتسجيل موعد كشف أو زيارة طبية جديدة وتفعيل التنبيهات المسبقة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("empty_add_appointment_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة موعد جديد")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("appointments_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appointments, key = { it.id }) { item ->
                AppointmentCard(
                    appointment = item,
                    profileName = profilesMap[item.profileId]?.name ?: "المستخدم",
                    onEdit = { appointmentToEdit = item },
                    onDelete = { appointmentToDelete = item },
                    onAttachments = { attachmentTarget = item }
                )
            }
        }
    }

    // Edit dialog
    if (appointmentToEdit != null) {
        AppointmentDialog(
            appointment = appointmentToEdit,
            profiles = profiles,
            initialProfileId = appointmentToEdit?.profileId,
            onDismiss = { appointmentToEdit = null },
            onSave = { id, profileId, title, doctor, dateTime, location, notes, repeat, offsets ->
                viewModel.saveAppointment(
                    id = id,
                    profileId = profileId,
                    title = title,
                    doctor = doctor,
                    dateTime = dateTime,
                    locationText = location,
                    notes = notes,
                    repeat = repeat,
                    reminderOffsets = offsets
                )
                appointmentToEdit = null
            }
        )
    }

    // Delete confirmation dialog
    if (appointmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { appointmentToDelete = null },
            title = { Text("حذف الموعد") },
            text = { Text("هل أنت متأكد من حذف موعد \"${appointmentToDelete?.title}\"؟ سيتم إلغاء التنبيهات الخاصة به.") },
            confirmButton = {
                Button(
                    onClick = {
                        appointmentToDelete?.let { viewModel.deleteAppointment(it) }
                        appointmentToDelete = null
                    }
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { appointmentToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Attachments dialog
    if (attachmentTarget != null) {
        AttachmentDialog(
            ownerType = "appointment",
            ownerId = attachmentTarget!!.id,
            ownerTitle = attachmentTarget!!.title,
            viewModel = viewModel,
            onDismiss = { attachmentTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentCard(
    appointment: AppointmentEntity,
    profileName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAttachments: () -> Unit
) {
    val context = LocalContext.current
    val fullDateFormat = remember { SimpleDateFormat("EEEE d MMMM yyyy، h:mm a", Locale("ar")) }
    val isPast = appointment.dateTime < System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("appointment_card_${appointment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPast) 1.dp else 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Profile & Repeat header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(profileName, style = MaterialTheme.typography.labelSmall) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val repeatRule = RepeatRule.fromString(appointment.repeat)
                    if (repeatRule != RepeatRule.NONE) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(repeatRule.displayName(), style = MaterialTheme.typography.labelSmall) },
                            icon = { Icon(Icons.Default.Repeat, contentDescription = null) }
                        )
                    }

                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل الموعد")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف الموعد",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                text = appointment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            // Doctor
            if (!appointment.doctor.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.height(16.dp).width(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = appointment.doctor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Date & Time
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.height(16.dp).width(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = fullDateFormat.format(appointment.dateTime),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Location with Map Button
            if (!appointment.locationText.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.height(16.dp).width(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = appointment.locationText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            MapLauncher.openInMaps(
                                context,
                                appointment.lat,
                                appointment.lng,
                                appointment.locationText
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("الخريطة")
                    }
                }
            }

            // Notes
            if (!appointment.notes.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = appointment.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reminders and attachments footer
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.height(16.dp).width(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${appointment.parsedReminderOffsets().size} تنبيهات مجدولة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = onAttachments,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Attachment, contentDescription = "المرفقات والروشتات")
                }
            }
        }
    }
}
