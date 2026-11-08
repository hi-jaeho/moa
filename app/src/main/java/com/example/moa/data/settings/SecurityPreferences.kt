
package com.example.moa.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securitySettingsDataStore by preferencesDataStore(
    name = "moa_security_settings"
)

data class SecuritySettings(
    val lockEnabled: Boolean = true,
    val biometricEnabled: Boolean = false
)

class SecurityPreferences(
    private val context: Context
) {
    private val lockKey = booleanPreferencesKey("lock_enabled")
    private val biometricKey = booleanPreferencesKey("biometric_enabled")

    val settings: Flow<SecuritySettings> =
        context.securitySettingsDataStore.data.map { prefs ->
            SecuritySettings(
                lockEnabled = prefs[lockKey] ?: true,
                biometricEnabled = prefs[biometricKey] ?: false
            )
        }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.securitySettingsDataStore.edit { prefs ->
            prefs[lockKey] = enabled
            if (!enabled) prefs[biometricKey] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securitySettingsDataStore.edit { prefs ->
            prefs[biometricKey] = enabled
        }
    }
}
