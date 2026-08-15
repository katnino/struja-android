package ba.noniboy.struja.data.local.dao

import androidx.room.*
import ba.noniboy.struja.data.local.entity.MeterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {
    @Query("SELECT * FROM meters WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAll(userId: String): List<MeterEntity>

    @Query("SELECT * FROM meters WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllFlow(userId: String): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE id = :id")
    suspend fun get(id: String): MeterEntity?

    @Insert
    suspend fun insert(meter: MeterEntity)

    @Update
    suspend fun update(meter: MeterEntity)

    @Delete
    suspend fun delete(meter: MeterEntity)
}
