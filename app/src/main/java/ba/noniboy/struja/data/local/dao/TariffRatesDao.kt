package ba.noniboy.struja.data.local.dao

import androidx.room.*
import ba.noniboy.struja.data.local.entity.TariffRatesEntity

@Dao
interface TariffRatesDao {
    @Query("SELECT * FROM tariff_rates WHERE id = 1 LIMIT 1")
    suspend fun get(): TariffRatesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rates: TariffRatesEntity)

    @Update
    suspend fun update(rates: TariffRatesEntity)
}
