package ba.noniboy.struja.data.repository

import ba.noniboy.struja.data.local.dao.UserSettingsDao
import ba.noniboy.struja.data.local.entity.UserSettingsEntity
import ba.noniboy.struja.data.security.CryptoManager
import javax.inject.Inject

/**
 * Repository for user settings, including encrypted AI API key storage.
 */
class UserSettingsRepository @Inject constructor(
    private val dao: UserSettingsDao,
    private val cryptoManager: CryptoManager
) {
    suspend fun get(userId: String): UserSettingsEntity? = dao.get(userId)

    suspend fun getDecryptedApiKey(userId: String): String? {
        val settings = dao.get(userId) ?: return null
        val encrypted = settings.aiApiKeyEncrypted ?: return null
        return cryptoManager.decrypt(encrypted)
    }

    suspend fun saveApiKey(userId: String, apiKey: String?) {
        val existing = dao.get(userId)
        val encrypted = if (apiKey != null && apiKey.isNotBlank()) {
            cryptoManager.encrypt(apiKey.trim())
        } else {
            null
        }
        val settings = if (existing != null) {
            existing.copy(
                aiApiKeyEncrypted = encrypted,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            UserSettingsEntity(
                userId = userId,
                aiApiKeyEncrypted = encrypted
            )
        }
        if (existing != null) {
            dao.update(settings)
        } else {
            dao.insert(settings)
        }
    }

    /**
     * Store the API key only if it's not empty.
     * Returns the decrypted value if stored, null otherwise.
     */
    suspend fun saveAndVerifyApiKey(userId: String, apiKey: String): Result<String?> {
        return try {
            saveApiKey(userId, apiKey)
            val decrypted = getDecryptedApiKey(userId)
            Result.success(decrypted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
