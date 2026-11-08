
package com.example.moa.ui.transaction

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.MoaApplication
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionEditUiState(
    val transaction: TransactionEntity? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val categoryId: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val memo: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class TransactionEditViewModel(
    application: MoaApplication
) : AndroidViewModel(application) {

    private val transactionRepository =
        application.transactionRepository

    private val categoryRepository =
        application.categoryRepository

    private val _uiState = MutableStateFlow(TransactionEditUiState())
    val uiState: StateFlow<TransactionEditUiState> = _uiState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null
    private var categoriesJob: kotlinx.coroutines.Job? = null

    fun loadTransaction(id: Long) {
        loadJob?.cancel()
        categoriesJob?.cancel()

        loadJob = viewModelScope.launch {
            _uiState.value = TransactionEditUiState(isLoading = true)

            try {
                val transaction = transactionRepository.getById(id)
                    ?: throw IllegalStateException("거래를 찾을 수 없습니다.")

                _uiState.value = TransactionEditUiState(
                    transaction = transaction,
                    type = transaction.type,
                    amount = transaction.amount.toString(),
                    date = transaction.date,
                    categoryId = transaction.categoryId,
                    memo = transaction.memo.orEmpty()
                )

                observeCategories(transaction.type)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    private fun observeCategories(type: TransactionType) {
        categoriesJob?.cancel()

        categoriesJob = viewModelScope.launch {
            categoryRepository.getByType(type).collect { list ->
                _uiState.update { state ->
                    state.copy(
                        categories = list,
                        categoryId = if (list.any { it.id == state.categoryId }) {
                            state.categoryId
                        } else {
                            list.firstOrNull()?.id
                        }
                    )
                }
            }
        }
    }

    fun setType(type: TransactionType) {
        _uiState.update {
            it.copy(
                type = type,
                categoryId = null,
                categories = emptyList(),
                error = null
            )
        }
        observeCategories(type)
    }

    fun setAmount(value: String) {
        if (value.all(Char::isDigit)) {
            _uiState.update { it.copy(amount = value, error = null) }
        }
    }

    fun setDate(value: LocalDate) {
        _uiState.update { it.copy(date = value) }
    }

    fun setMemo(value: String) {
        _uiState.update { it.copy(memo = value) }
    }

    fun setCategory(id: Long) {
        if (_uiState.value.categories.any { it.id == id }) {
            _uiState.update { it.copy(categoryId = id) }
        }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return

        val original = state.transaction ?: return
        val amount = state.amount.toLongOrNull()

        if (amount == null || amount <= 0L) {
            _uiState.update {
                it.copy(error = "올바른 금액을 입력해 주세요.")
            }
            return
        }

        val category = state.categories.find { it.id == state.categoryId }
        if (category == null || category.type != state.type) {
            _uiState.update {
                it.copy(error = "카테고리를 선택해 주세요.")
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                transactionRepository.update(
                    original.copy(
                        type = state.type,
                        amount = amount,
                        date = state.date,
                        categoryId = category.id,
                        memo = state.memo.trim().ifBlank { null },
                        updatedAt = System.currentTimeMillis()
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "수정 실패: ${e.message}")
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return

        val transaction = state.transaction ?: return
        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                transactionRepository.delete(transaction)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "삭제 실패: ${e.message}")
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
