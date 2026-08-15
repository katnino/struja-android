package ba.noniboy.struja.data.local.dao

import androidx.room.*
import ba.noniboy.struja.data.local.entity.UserSettingsEntity

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: String): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: UserSettingsEntity)

    @Update
    suspend fun update(settings: UserSettingsEntity)

    @Query("DELETE FROM user_settings WHERE userId = :userId")
    suspend fun delete(userId: String)
}
