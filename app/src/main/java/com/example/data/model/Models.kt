package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RepeatRule {
    NONE, DAILY, WEEKLY, MONTHLY;

    fun displayName(): String = when (this) {
        NONE -> "بدون تكرار"
        DAILY -> "يوميًا"
        WEEKLY -> "أسبوعيًا"
        MONTHLY -> "شهريًا"
    }

    companion object {
        fun fromString(value: String?): RepeatRule {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

enum class AttachmentKind {
    REPORT, TEST, PRESCRIPTION, OTHER;

    fun displayName(): String = when (this) {
        REPORT -> "تقرير طبي"
        TEST -> "تحليل / أشعة"
        PRESCRIPTION -> "روشتة علاج"
        OTHER -> "مرفق آخر"
    }

    companion object {
        fun fromString(value: String?): AttachmentKind {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class OwnerType {
    APPOINTMENT, MEDICATION
}

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relation: String = "self"
)

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profile_id", "date_time"])]
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    val title: String,
    val doctor: String? = null,
    @ColumnInfo(name = "date_time")
    val dateTime: Long,
    @ColumnInfo(name = "location_text")
    val locationText: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val notes: String? = null,
    val repeat: String = "none",
    @ColumnInfo(name = "reminder_offsets")
    val reminderOffsets: String = "1440,60" // minutes before appointment
) {
    val hasLocation: Boolean
        get() = (lat != null && lng != null) || (!locationText.isNullOrBlank())

    fun parsedReminderOffsets(): List<Int> {
        return reminderOffsets.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(1440, 60) }
    }
}

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profile_id"])]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    val name: String,
    val dose: String? = null,
    val times: String, // comma-separated e.g. "08:00,14:00,20:00"
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null
) {
    fun parsedTimes(): List<String> {
        return times.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Entity(
    tableName = "attachments",
    indices = [Index(value = ["owner_type", "owner_id"])]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "owner_type")
    val ownerType: String, // "appointment" or "medication"
    @ColumnInfo(name = "owner_id")
    val ownerId: Long,
    val path: String,
    val kind: String = "other",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
