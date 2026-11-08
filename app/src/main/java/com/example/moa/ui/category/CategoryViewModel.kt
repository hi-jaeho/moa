
package com.example.moa.ui.category

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.MoaApplication
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val categories: List<CategoryEntity> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null
)

class CategoryViewModel(
    application: MoaApplication
) : AndroidViewModel(application) {

    private val repository = application.categoryRepository

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .map { it.type }
                .distinctUntilChanged()
                .flatMapLatest { repository.getByType(it) }
                .collect { categories ->
                    _uiState.update {
                        it.copy(categories = categories)
                    }
                }
        }
    }

    fun setType(type: TransactionType) {
        _uiState.update {
            it.copy(type = type, message = null)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (!validateName(trimmed)) return

        val type = _uiState.value.type
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val existing = repository.getByType(type).first()

                if (existing.any { it.name == trimmed }) {
                    showMessage("이미 존재하는 카테고리입니다.")
                    return@launch
                }

                repository.insert(
                    CategoryEntity(
                        name = trimmed,
                        type = type,
                        sortOrder = (existing.maxOfOrNull {
                            it.sortOrder
                        } ?: 0) + 1
                    )
                )

                showMessage("카테고리를 추가했습니다.")
            } catch (e: Exception) {
                showMessage("추가 실패: ${e.message}")
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun renameCategory(
        category: CategoryEntity,
        newName: String
    ) {
        val trimmed = newName.trim()
        if (!validateName(trimmed)) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val existing = repository.getByType(category.type).first()

                if (existing.any {
                        it.id != category.id && it.name == trimmed
                    }) {
                    showMessage("이미 존재하는 카테고리입니다.")
                    return@launch
                }

                repository.update(category.copy(name = trimmed))
                showMessage("카테고리를 수정했습니다.")
            } catch (e: Exception) {
                showMessage("수정 실패: ${e.message}")
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                repository.delete(category)
                showMessage("카테고리를 삭제했습니다.")
            } catch (e: Exception) {
                showMessage(
                    "삭제할 수 없습니다. 거래에서 사용 중인 카테고리인지 확인해 주세요."
                )
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun validateName(name: String): Boolean {
        if (name.isBlank()) {
            showMessage("카테고리 이름을 입력해 주세요.")
            return false
        }

        if (name.length > 20) {
            showMessage("카테고리 이름은 20자 이하로 입력해 주세요.")
            return false
        }

        return true
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
