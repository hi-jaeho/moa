
package com.example.moa.ui.security

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.data.settings.PinPreferences
import com.example.moa.data.settings.SecurityAttemptPreferences
import com.example.moa.data.settings.SecurityPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.moa.data.settings.SecuritySettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

enum class PinStatus {
    LOADING,
    SETUP,
    CONFIRM,
    LOCKED,
    UNLOCKED
}

class PinViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val preferences = PinPreferences(application)

    private val _status = MutableStateFlow(PinStatus.LOADING)
    val status = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var firstPin: String? = null
    private var busy = false


    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts = _failedAttempts.asStateFlow()

    private val _lockoutUntil = MutableStateFlow(0L)
    val lockoutUntil = _lockoutUntil.asStateFlow()

    private val _securityMessage = MutableStateFlow<String?>(null)
    private val _pinChangeSuccess = MutableStateFlow(false)
    val pinChangeSuccess = _pinChangeSuccess.asStateFlow()

    fun resetPinChangeSuccess() {
        _pinChangeSuccess.value = false
        _securityMessage.value = null
    }
    val securityMessage = _securityMessage.asStateFlow()
    private val attemptPreferences = SecurityAttemptPreferences(application)

    private val securityPreferences = SecurityPreferences(application)
    init {
        // 1. PIN 실패 횟수 및 잠금 제한 상태 관찰
        viewModelScope.launch {
            attemptPreferences.state.collect { state ->
                _failedAttempts.value = state.failedAttempts
                _lockoutUntil.value = state.lockoutUntil
            }
        }

        // 2. PIN 및 보안 설정 초기화
        viewModelScope.launch {
            try {
                val exists = withContext(Dispatchers.IO) {
                    preferences.hasPin()
                }

                val settings = securityPreferences.settings.first()

                _status.value = when {
                    !exists -> PinStatus.SETUP
                    !settings.lockEnabled -> PinStatus.UNLOCKED
                    else -> PinStatus.LOCKED
                }
            } catch (e: Exception) {
                _error.value = "보안 설정을 불러오지 못했습니다."
                _status.value = PinStatus.LOCKED
            }
        }
    }


    fun submitPin(pin: String) {
        if (busy || !pin.matches(Regex("\\d{6}"))) return

        _error.value = null

        when (_status.value) {
            PinStatus.SETUP -> {
                firstPin = pin
                _status.value = PinStatus.CONFIRM
            }

            PinStatus.CONFIRM -> {
                if (pin != firstPin) {
                    firstPin = null
                    _error.value = "PIN이 일치하지 않습니다."
                    _status.value = PinStatus.SETUP
                    return
                }

                busy = true
                viewModelScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            preferences.savePin(pin)
                        }
                        firstPin = null
                        _status.value = PinStatus.UNLOCKED
                    } catch (e: Exception) {
                        _error.value = "PIN 저장에 실패했습니다."
                    } finally {
                        busy = false
                    }
                }
            }


            PinStatus.LOCKED -> {
                if (busy) return

                busy = true

                viewModelScope.launch {
                    try {
                        val attemptState = attemptPreferences.getState()
                        val now = System.currentTimeMillis()

                        if (now < attemptState.lockoutUntil) {
                            val remainingSeconds =
                                ((attemptState.lockoutUntil - now + 999L) / 1000L)

                            _error.value =
                                "${remainingSeconds}초 후 다시 시도하세요."

                            return@launch
                        }

                        val valid = withContext(Dispatchers.IO) {
                            preferences.verifyPin(pin)
                        }

                        if (valid) {
                            attemptPreferences.reset()

                            _error.value = null
                            _status.value = PinStatus.UNLOCKED
                        } else {
                            val result = attemptPreferences.recordFailure()

                            if (result.lockoutUntil > System.currentTimeMillis()) {
                                _error.value =
                                    "PIN을 5회 잘못 입력했습니다. 30초 후 다시 시도하세요."
                            } else {
                                _error.value =
                                    "PIN이 올바르지 않습니다. (${result.failedAttempts}/5)"
                            }
                        }
                    } catch (e: Exception) {
                        _error.value = "PIN 인증 중 오류가 발생했습니다."
                    } finally {
                        busy = false
                    }
                }
            }
            else -> Unit
        }
    }

    fun lock() {
        if (_status.value == PinStatus.UNLOCKED) {
            _status.value = PinStatus.LOCKED
        }
    }

    fun changePin() {
        if (_status.value == PinStatus.UNLOCKED) {
            firstPin = null
            _error.value = null
            _status.value = PinStatus.SETUP
        }
    }

    fun disablePin() {
        if (_status.value != PinStatus.UNLOCKED || busy) return

        busy = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    preferences.removePin()
                }
                firstPin = null
                _status.value = PinStatus.UNLOCKED
            } catch (e: Exception) {
                _error.value = "잠금 해제 설정에 실패했습니다."
            } finally {
                busy = false
            }
        }
    }

    fun unlockWithBiometric() {
        if (_status.value == PinStatus.LOCKED) {
            _error.value = null
            _status.value = PinStatus.UNLOCKED
        }
    }



    val securitySettings = securityPreferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SecuritySettings()
    )


    fun setLockEnabled(enabled: Boolean, currentPin: String) {
        if (_status.value != PinStatus.UNLOCKED || busy) return

        busy = true
        viewModelScope.launch {
            try {
                val valid = withContext(Dispatchers.IO) {
                    preferences.verifyPin(currentPin)
                }

                if (!valid) {
                    _error.value = "현재 PIN이 올바르지 않습니다."
                    return@launch
                }

                securityPreferences.setLockEnabled(enabled)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "잠금 설정 변경에 실패했습니다."
            } finally {
                busy = false
            }
        }
    }


    fun setBiometricEnabled(
        enabled: Boolean,
        currentPin: String
    ) {
        if (_status.value != PinStatus.UNLOCKED || busy) return

        busy = true

        viewModelScope.launch {
            try {
                val valid = withContext(Dispatchers.IO) {
                    preferences.verifyPin(currentPin)
                }

                if (!valid) {
                    _securityMessage.value = "현재 PIN이 올바르지 않습니다."
                    return@launch
                }

                if (!securitySettings.value.lockEnabled) {
                    _securityMessage.value = "앱 잠금을 먼저 활성화하세요."
                    return@launch
                }

                securityPreferences.setBiometricEnabled(enabled)
                _securityMessage.value = null
            } catch (e: Exception) {
                _securityMessage.value = "생체인증 설정 변경에 실패했습니다."
            } finally {
                busy = false
            }
        }
    }


    fun lockIfEnabled() {
        if (
            _status.value == PinStatus.UNLOCKED &&
            securitySettings.value.lockEnabled
        ) {
            _status.value = PinStatus.LOCKED
        }
    }




    fun updatePin(
        currentPin: String,
        newPin: String,
        confirmPin: String
    ) {
        if (_status.value != PinStatus.UNLOCKED || busy) return

        _pinChangeSuccess.value = false
        _securityMessage.value = null

        if (!newPin.matches(Regex("\\d{6}"))) {
            _securityMessage.value = "새 PIN은 숫자 6자리여야 합니다."
            return
        }

        if (newPin != confirmPin) {
            _securityMessage.value = "새 PIN이 일치하지 않습니다."
            return
        }

        busy = true

        viewModelScope.launch {
            try {
                val valid = withContext(Dispatchers.IO) {
                    preferences.verifyPin(currentPin)
                }

                if (!valid) {
                    _securityMessage.value = "현재 PIN이 올바르지 않습니다."
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    preferences.savePin(newPin)
                }

                _pinChangeSuccess.value = true
                _securityMessage.value = "PIN이 변경되었습니다."
            } catch (e: Exception) {
                _securityMessage.value = "PIN 변경에 실패했습니다."
            } finally {
                busy = false
            }
        }
    }

}
