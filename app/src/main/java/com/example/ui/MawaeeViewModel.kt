package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.AppointmentEntity
import com.example.data.model.AttachmentEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.ProfileEntity
import com.example.service.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    APPOINTMENTS("المواعيد"),
    MEDICATIONS("الأدوية"),
    CALENDAR("التقويم"),
    PROFILES("الملفات"),
    STATS("نظرة عامة")
}

@OptIn(ExperimentalCoroutinesApi::class)
class MawaeeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val profileDao = db.profileDao()
    private val appointmentDao = db.appointmentDao()
    private val medicationDao = db.medicationDao()
    private val attachmentDao = db.attachmentDao()

    val profiles: StateFlow<List<ProfileEntity>> = profileDao.getAllProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.APPOINTMENTS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun selectProfile(profileId: Long?) {
        _selectedProfileId.value = profileId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filtered appointments by selected profile and query
    val appointments: StateFlow<List<AppointmentEntity>> = combine(
        _selectedProfileId.flatMapLatest { id ->
            if (id == null) appointmentDao.getAllAppointmentsFlow()
            else appointmentDao.getAppointmentsForProfileFlow(id)
        },
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.title.contains(query, ignoreCase = true) ||
                    (it.doctor?.contains(query, ignoreCase = true) == true) ||
                    (it.locationText?.contains(query, ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered medications by selected profile and query
    val medications: StateFlow<List<MedicationEntity>> = combine(
        _selectedProfileId.flatMapLatest { id ->
            if (id == null) medicationDao.getAllMedicationsFlow()
            else medicationDao.getMedicationsForProfileFlow(id)
        },
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
                    (it.dose?.contains(query, ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveProfile(id: Long = 0, name: String, relation: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = ProfileEntity(id = id, name = name.trim(), relation = relation.trim())
            val savedId = profileDao.insert(entity)
            if (_selectedProfileId.value == null || id == 0L) {
                _selectedProfileId.value = if (id == 0L) savedId else id
            }
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cancel alarms for all appointments of this profile
            val appts = appointmentDao.getAppointmentsForProfile(profile.id)
            for (a in appts) {
                AlarmScheduler.cancelAppointment(getApplication(), a.id)
                attachmentDao.deleteForOwner("appointment", a.id)
            }

            // Cancel alarms and work for all medications of this profile
            val meds = medicationDao.getMedicationsForProfile(profile.id)
            for (m in meds) {
                AlarmScheduler.cancelMedication(getApplication(), m.id)
                com.example.service.MedicationWorkScheduler.cancelMedication(getApplication(), m.id)
                attachmentDao.deleteForOwner("medication", m.id)
            }

            // Delete profile
            profileDao.delete(profile)

            // If no profiles left, ensure default "أنا" profile exists
            if (profileDao.count() == 0) {
                val newId = profileDao.insert(ProfileEntity(name = "أنا", relation = "self"))
                _selectedProfileId.value = newId
            } else if (_selectedProfileId.value == profile.id) {
                _selectedProfileId.value = null
            }
        }
    }

    fun saveAppointment(
        id: Long = 0,
        profileId: Long,
        title: String,
        doctor: String?,
        dateTime: Long,
        locationText: String?,
        notes: String?,
        repeat: String = "none",
        reminderOffsets: String = "1440,60",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = AppointmentEntity(
                id = id,
                profileId = profileId,
                title = title.trim(),
                doctor = doctor?.trim()?.ifEmpty { null },
                dateTime = dateTime,
                locationText = locationText?.trim()?.ifEmpty { null },
                notes = notes?.trim()?.ifEmpty { null },
                repeat = repeat,
                reminderOffsets = reminderOffsets
            )
            val newId = appointmentDao.insert(entity)
            val savedEntity = if (id == 0L) entity.copy(id = newId) else entity
            val pName = profileDao.getProfileById(profileId)?.name ?: "المستخدم"
            AlarmScheduler.scheduleAppointment(getApplication(), savedEntity, pName)
            viewModelScope.launch(Dispatchers.Main) { onComplete() }
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancelAppointment(getApplication(), appointment.id)
            attachmentDao.deleteForOwner("appointment", appointment.id)
            appointmentDao.deleteById(appointment.id)
        }
    }

    fun saveMedication(
        id: Long = 0,
        profileId: Long,
        name: String,
        dose: String?,
        times: List<String>,
        startDate: Long,
        endDate: Long?,
        frequency: String = "daily",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val timesString = times.joinToString(",")
            val entity = MedicationEntity(
                id = id,
                profileId = profileId,
                name = name.trim(),
                dose = dose?.trim()?.ifEmpty { null },
                times = timesString,
                startDate = startDate,
                endDate = endDate,
                frequency = frequency
            )
            val newId = medicationDao.insert(entity)
            val savedEntity = if (id == 0L) entity.copy(id = newId) else entity
            val pName = profileDao.getProfileById(profileId)?.name ?: "المستخدم"
            AlarmScheduler.scheduleMedication(getApplication(), savedEntity, pName)
            com.example.service.MedicationWorkScheduler.scheduleMedication(getApplication(), savedEntity, pName)
            viewModelScope.launch(Dispatchers.Main) { onComplete() }
        }
    }

    fun deleteMedication(medication: MedicationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            AlarmScheduler.cancelMedication(getApplication(), medication.id)
            com.example.service.MedicationWorkScheduler.cancelMedication(getApplication(), medication.id)
            attachmentDao.deleteForOwner("medication", medication.id)
            medicationDao.deleteById(medication.id)
        }
    }

    fun getAttachments(ownerType: String, ownerId: Long): Flow<List<AttachmentEntity>> {
        return attachmentDao.getAttachmentsForOwnerFlow(ownerType, ownerId)
    }

    fun saveAttachment(ownerType: String, ownerId: Long, path: String, kind: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = AttachmentEntity(
                ownerType = ownerType,
                ownerId = ownerId,
                path = path,
                kind = kind
            )
            attachmentDao.insert(entity)
        }
    }

    fun deleteAttachment(attachment: AttachmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            attachmentDao.delete(attachment)
        }
    }
}
