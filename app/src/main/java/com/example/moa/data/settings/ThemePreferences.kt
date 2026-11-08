
package com.example.moa.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(
    name = "moa_settings"
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class ThemePreferences(
    private val context: Context
) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> =
        context.themeDataStore.data.map { preferences ->
            val saved = preferences[themeKey]

            ThemeMode.entries.firstOrNull {
                it.name == saved
            } ?: ThemeMode.SYSTEM
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[themeKey] = mode.name
        }
    }
}
