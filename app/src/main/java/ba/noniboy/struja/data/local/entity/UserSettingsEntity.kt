package ba.noniboy.struja.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User settings — stores the encrypted AI API key.
 * Uses Android Keystore for encryption/decryption.
 */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val userId: String,
    val aiApiKeyEncrypted: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
