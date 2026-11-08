
package com.example.moa.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.data.settings.ThemeMode
import com.example.moa.data.settings.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val preferences = ThemePreferences(application)

    val themeMode = preferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }
}
