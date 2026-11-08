
package com.example.moa.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64
import java.security.MessageDigest

private val Context.pinDataStore by preferencesDataStore(
    name = "moa_security"
)

class PinPreferences(
    private val context: Context
) {
    private val saltKey = stringPreferencesKey("pin_salt")
    private val hashKey = stringPreferencesKey("pin_hash")

    private val iterations = 210_000
    private val random = SecureRandom()

    suspend fun hasPin(): Boolean {
        val prefs = context.pinDataStore.data.first()
        return prefs[saltKey] != null && prefs[hashKey] != null
    }

    suspend fun savePin(pin: String) {
        require(pin.matches(Regex("\\d{6}"))) {
            "PIN은 숫자 6자리여야 합니다."
        }

        val salt = ByteArray(16)
        random.nextBytes(salt)

        val hash = hashPin(pin, salt)

        context.pinDataStore.edit { prefs ->
            prefs[saltKey] = encode(salt)
            prefs[hashKey] = encode(hash)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        if (!pin.matches(Regex("\\d{6}"))) return false

        val prefs = context.pinDataStore.data.first()
        val salt = prefs[saltKey] ?: return false
        val savedHash = prefs[hashKey] ?: return false

        return try {
            val actualHash = hashPin(pin, decode(salt))
            MessageDigest.isEqual(actualHash, decode(savedHash))
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    suspend fun removePin() {
        context.pinDataStore.edit { prefs ->
            prefs.remove(saltKey)
            prefs.remove(hashKey)
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            iterations,
            256
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)
}
