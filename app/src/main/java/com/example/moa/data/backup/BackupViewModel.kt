
package com.example.moa.ui.backup

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.MoaApplication
import com.example.moa.data.backup.BackupData
import com.example.moa.data.backup.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val pendingRestore: BackupData? = null
)

class BackupViewModel(
    application: MoaApplication
) : AndroidViewModel(application) {

    private val manager = BackupManager(application.database)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun createBackup(
        onReady: (String) -> Unit
    ) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, message = null) }

        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    manager.exportJson()
                }
                onReady(json)
            } catch (e: Exception) {
                showMessage("백업 생성 실패: ${e.message}")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun prepareRestore(json: String) {
        if (_uiState.value.isBusy) return
        _uiState.update {
            it.copy(
                isBusy = true,
                message = null,
                pendingRestore = null
            )
        }

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.Default) {
                    manager.parseAndValidate(json)
                }

                _uiState.update {
                    it.copy(pendingRestore = data)
                }
            } catch (e: Exception) {
                showMessage("백업 파일 검증 실패: ${e.message}")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun confirmRestore() {
        if (_uiState.value.isBusy) return
        val data = _uiState.value.pendingRestore ?: return

        _uiState.update {
            it.copy(
                isBusy = true,
                pendingRestore = null,
                message = null
            )
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    manager.restore(data)
                }
                showMessage("복원이 완료되었습니다.")
            } catch (e: Exception) {
                showMessage("복원 실패: ${e.message}")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun cancelRestore() {
        _uiState.update { it.copy(pendingRestore = null) }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
