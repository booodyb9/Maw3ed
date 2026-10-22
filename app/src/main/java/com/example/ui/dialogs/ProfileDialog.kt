package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ProfileEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileDialog(
    profile: ProfileEntity? = null,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, relation: String) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var relation by remember { mutableStateOf(profile?.relation ?: "الأب") }
    var nameError by remember { mutableStateOf(false) }

    val presetRelations = listOf(
        "أنا", "الأب", "الأم", "الزوج", "الزوجة", "الابن", "الابنة", "أخرى"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (profile != null) "تعديل بيانات الملف" else "إضافة فرد جديد",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("الاسم * (مثال: الوالد، سارة، أحمد)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("يرجى إدخال الاسم") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("صلة القرابة:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetRelations.forEach { rel ->
                        FilterChip(
                            selected = relation == rel,
                            onClick = { relation = rel },
                            label = { Text(rel) }
                        )
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
                    onSave(profile?.id ?: 0L, name.trim(), relation)
                },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(if (profile != null) "حفظ التعديل" else "إضافة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
