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
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MedicationEntity
import com.example.data.model.ProfileEntity
import com.example.ui.MawaeeViewModel
import com.example.ui.dialogs.AttachmentDialog
import com.example.ui.dialogs.MedicationDialog
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MedicationsScreen(
    viewModel: MawaeeViewModel,
    profiles: List<ProfileEntity>,
    onAddClick: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val profilesMap = remember(profiles) { profiles.associateBy { it.id } }

    var medToEdit by remember { mutableStateOf<MedicationEntity?>(null) }
    var medToDelete by remember { mutableStateOf<MedicationEntity?>(null) }
    var attachmentTarget by remember { mutableStateOf<MedicationEntity?>(null) }

    if (medications.isEmpty()) {
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
                    Icons.Default.Medication,
                    contentDescription = null,
                    modifier = Modifier.height(72.dp).width(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Text(
                    "لا توجد أدوية مسجلة حاليًا",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "سجّل أدويتك وجرعاتها اليومية ومواعيدها لتذكيرك بها بانتظام",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("empty_add_medication_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة دواء جديد")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("medications_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(medications, key = { it.id }) { item ->
                MedicationCard(
                    medication = item,
                    profileName = profilesMap[item.profileId]?.name ?: "المستخدم",
                    onEdit = { medToEdit = item },
                    onDelete = { medToDelete = item },
                    onAttachments = { attachmentTarget = item }
                )
            }
        }
    }

    if (medToEdit != null) {
        MedicationDialog(
            medication = medToEdit,
            profiles = profiles,
            initialProfileId = medToEdit?.profileId,
            onDismiss = { medToEdit = null },
            onSave = { id, profileId, name, dose, times, startDate, endDate ->
                viewModel.saveMedication(
                    id = id,
                    profileId = profileId,
                    name = name,
                    dose = dose,
                    times = times,
                    startDate = startDate,
                    endDate = endDate
                )
                medToEdit = null
            }
        )
    }

    if (medToDelete != null) {
        AlertDialog(
            onDismissRequest = { medToDelete = null },
            title = { Text("حذف الدواء") },
            text = { Text("هل أنت متأكد من حذف علاج \"${medToDelete?.name}\"؟") },
            confirmButton = {
                Button(
                    onClick = {
                        medToDelete?.let { viewModel.deleteMedication(it) }
                        medToDelete = null
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { medToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (attachmentTarget != null) {
        AttachmentDialog(
            ownerType = "medication",
            ownerId = attachmentTarget!!.id,
            ownerTitle = attachmentTarget!!.name,
            viewModel = viewModel,
            onDismiss = { attachmentTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationCard(
    medication: MedicationEntity,
    profileName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAttachments: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale("ar")) }
    val now = System.currentTimeMillis()
    val isExpired = medication.endDate != null && medication.endDate < now

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("medication_card_${medication.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Profile & action buttons
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

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Medication Name
            Text(
                text = medication.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Dose
            if (!medication.dose.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = medication.dose,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(8.dp))

            // Daily dose times chips
            Text(
                "مواعيد الجرعات يوميًا:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                medication.parsedTimes().forEach { time ->
                    AssistChip(
                        onClick = {},
                        label = { Text(time, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.height(14.dp).width(14.dp)
                            )
                        }
                    )
                }
            }

            // Duration and Attachment footer
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.height(14.dp).width(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    val durationText = if (medication.endDate != null) {
                        "حتى ${dateFormat.format(medication.endDate)}"
                    } else {
                        "علاج مستمر"
                    }
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                FilledTonalIconButton(
                    onClick = onAttachments,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Attachment, contentDescription = "روشتات ومرفقات الدواء")
                }
            }
        }
    }
}
