package ba.noniboy.struja.data.security

import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local encryption manager using Android Keystore.
 *
 * The Android Keystore generates and stores an AES-256 key in a hardware-backed
 * key store when available. The key never leaves the device's secure hardware
 * (or TEE on devices without Secure Element).
 *
 * Encryption format: `v1:<ivHex>:<ciphertextHex>`
 * This matches the web app's crypto.ts format (without authTag since GCM
 * authentication tag is appended to ciphertext by the JCE provider).
 */
class CryptoManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG = "CryptoManager"

        // The key alias used for the single local user
        const val KEY_ALIAS = "struja_master_key"

        private val HEX_CHARS = "0123456789abcdef".toCharArray()
    }

    /**
     * Get or create the AES-256 secret key from the Android Keystore.
     */
    private fun getSecretKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec =
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        return keyStore.getKey(alias, null) as SecretKey
    }

    /**
     * Encrypt a plaintext string.
     * @param plaintext The text to encrypt.
     * @param alias The key alias in the Keystore (default: KEY_ALIAS).
     * @return Encrypted string in format `v1:<ivHex>:<ciphertextHex>`.
     */
    fun encrypt(plaintext: String, alias: String = KEY_ALIAS): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(alias))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        return "v1:${iv.toHex()}:${ciphertext.toHex()}"
    }

    /**
     * Decrypt a string encrypted by [encrypt] or by the web app's crypto.ts.
     *
     * Backward compatibility: strings that don't start with `v1:` are returned as-is
     * (plain text), matching the web app's behavior.
     *
     * @param stored The encrypted string in format `v1:<ivHex>:<ciphertextHex>`.
     * @param alias The key alias in the Keystore.
     * @return The decrypted plaintext.
     */
    fun decrypt(stored: String, alias: String = KEY_ALIAS): String {
        if (!stored.startsWith("v1:")) return stored

        val parts = stored.split(":")
        if (parts.size != 3) return stored

        val iv = parts[1].hexToByteArray()
        val ciphertext = parts[2].hexToByteArray()
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(alias), spec)
        return try {
            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            ""
        }
    }

    // Extension function for ByteArray to hex string
    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (byte in this) {
            sb.append(HEX_CHARS[(byte.toInt() shr 4) and 0x0F])
            sb.append(HEX_CHARS[byte.toInt() and 0x0F])
        }
        return sb.toString()
    }

    // Extension function for hex string to ByteArray
    private fun String.hexToByteArray(): ByteArray {
        val len = this.length
        if (len % 2 != 0) throw IllegalArgumentException("Hex string must have even length")
        val result = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            val high = this[i].digitToInt(16)
            val low = this[i + 1].digitToInt(16)
            result[i / 2] = (high shl 4 or low).toByte()
        }
        return result
    }
}