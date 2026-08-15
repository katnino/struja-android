package ba.noniboy.struja.data.local.dao

import androidx.room.*
import ba.noniboy.struja.data.local.entity.ReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings WHERE meterId = :meterId ORDER BY recordedAt ASC")
    suspend fun getForMeter(meterId: String): List<ReadingEntity>

    @Query("SELECT * FROM readings WHERE meterId = :meterId ORDER BY recordedAt ASC")
    fun getForMeterFlow(meterId: String): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE id = :id")
    suspend fun get(id: String): ReadingEntity?

    @Query("SELECT * FROM readings WHERE meterId = :meterId ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatest(meterId: String): ReadingEntity?

    @Insert
    suspend fun insert(reading: ReadingEntity)

    @Update
    suspend fun update(reading: ReadingEntity)

    @Delete
    suspend fun delete(reading: ReadingEntity)
}
