package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppointmentEntity
import com.example.data.model.AttachmentEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id ASC")
    fun getAllProfilesFlow(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY id ASC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE profile_id = :profileId ORDER BY date_time ASC")
    fun getAppointmentsForProfileFlow(profileId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments ORDER BY date_time ASC")
    fun getAllAppointmentsFlow(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments ORDER BY date_time ASC")
    suspend fun getAllAppointments(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    suspend fun getAppointmentById(id: Long): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: AppointmentEntity): Long

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE profile_id = :profileId ORDER BY name ASC")
    fun getMedicationsForProfileFlow(profileId: Long): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedicationsFlow(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY name ASC")
    suspend fun getAllMedications(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE owner_type = :ownerType AND owner_id = :ownerId ORDER BY created_at DESC")
    fun getAttachmentsForOwnerFlow(ownerType: String, ownerId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE owner_type = :ownerType AND owner_id = :ownerId ORDER BY created_at DESC")
    suspend fun getAttachmentsForOwner(ownerType: String, ownerId: Long): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE owner_type = :ownerType AND owner_id = :ownerId")
    suspend fun deleteForOwner(ownerType: String, ownerId: Long)
}
