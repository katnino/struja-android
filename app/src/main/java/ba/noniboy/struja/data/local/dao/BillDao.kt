package ba.noniboy.struja.data.local.dao

import androidx.room.*
import ba.noniboy.struja.data.local.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE meterId = :meterId ORDER BY createdAt DESC")
    suspend fun getAllForMeter(meterId: String): List<BillEntity>

    @Query("SELECT * FROM bills WHERE meterId = :meterId ORDER BY createdAt DESC")
    fun getAllForMeterFlow(meterId: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun get(id: String): BillEntity?

    @Insert
    suspend fun insert(bill: BillEntity)

    @Query("DELETE FROM bills WHERE meterId = :meterId")
    suspend fun deleteAllForMeter(meterId: String)
}
