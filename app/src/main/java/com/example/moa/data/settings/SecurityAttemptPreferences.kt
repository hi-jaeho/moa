
package com.example.moa.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityAttemptDataStore by preferencesDataStore(
    name = "moa_security_attempts"
)

data class SecurityAttemptState(
    val failedAttempts: Int = 0,
    val lockoutUntil: Long = 0L
)

class SecurityAttemptPreferences(
    private val context: Context
) {
    private val failedAttemptsKey =
        intPreferencesKey("failed_attempts")

    private val lockoutUntilKey =
        longPreferencesKey("lockout_until")

    val state: Flow<SecurityAttemptState> =
        context.securityAttemptDataStore.data.map { preferences ->
            SecurityAttemptState(
                failedAttempts = preferences[failedAttemptsKey] ?: 0,
                lockoutUntil = preferences[lockoutUntilKey] ?: 0L
            )
        }

    suspend fun getState(): SecurityAttemptState {
        return state.first()
    }

    suspend fun recordFailure(
        now: Long = System.currentTimeMillis()
    ): SecurityAttemptState {
        var result = SecurityAttemptState()

        context.securityAttemptDataStore.edit { preferences ->
            val currentUntil = preferences[lockoutUntilKey] ?: 0L
            val currentFailures = preferences[failedAttemptsKey] ?: 0

            if (now < currentUntil) {
                result = SecurityAttemptState(
                    failedAttempts = currentFailures,
                    lockoutUntil = currentUntil
                )
                return@edit
            }

            val failures = currentFailures + 1

            if (failures >= 5) {
                val until = now + 30_000L

                preferences[failedAttemptsKey] = 0
                preferences[lockoutUntilKey] = until

                result = SecurityAttemptState(
                    failedAttempts = 0,
                    lockoutUntil = until
                )
            } else {
                preferences[failedAttemptsKey] = failures
                preferences[lockoutUntilKey] = 0L

                result = SecurityAttemptState(
                    failedAttempts = failures,
                    lockoutUntil = 0L
                )
            }
        }

        return result
    }

    suspend fun reset() {
        context.securityAttemptDataStore.edit { preferences ->
            preferences[failedAttemptsKey] = 0
            preferences[lockoutUntilKey] = 0L
        }
    }
}
