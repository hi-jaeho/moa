
package com.example.moa.ui.transaction

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.MoaApplication
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

class TransactionViewModel(
    application: MoaApplication
) : AndroidViewModel(application) {

    private val transactionRepository =
        application.transactionRepository

    private val categoryRepository =
        application.categoryRepository

    private val _type =
        MutableStateFlow(TransactionType.EXPENSE)
    val type: StateFlow<TransactionType> = _type.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _memo = MutableStateFlow("")
    val memo: StateFlow<String> = _memo.asStateFlow()

    private val _categories =
        MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> =
        _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> =
        _selectedCategoryId.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            type.collectLatest { currentType ->
                categoryRepository.getByType(currentType)
                    .collect { list ->
                        _categories.value = list

                        if (list.none { it.id == _selectedCategoryId.value }) {
                            _selectedCategoryId.value = list.firstOrNull()?.id
                        }
                    }
            }
        }
    }

    fun setType(value: TransactionType) {
        _type.value = value
        _selectedCategoryId.value = null
        _categories.value = emptyList()
    }

    fun setAmount(value: String) {
        if (value.all { it.isDigit() }) {
            _amount.value = value
            _message.value = null
        }
    }

    fun setDate(value: LocalDate) {
        _date.value = value
    }

    fun setMemo(value: String) {
        _memo.value = value
    }

    fun selectCategory(id: Long) {
        if (_categories.value.any { it.id == id }) {
            _selectedCategoryId.value = id
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun saveTransaction() {
        if (_isSaving.value) return

        val amountValue = _amount.value.toLongOrNull()

        if (amountValue == null || amountValue <= 0L) {
            _message.value = "올바른 금액을 입력해 주세요."
            return
        }

        val categoryId = _selectedCategoryId.value
        val category = _categories.value.find { it.id == categoryId }

        if (categoryId == null || category == null ||
            category.type != _type.value) {
            _message.value = "카테고리를 선택해 주세요."
            return
        }

        _isSaving.value = true

        viewModelScope.launch {
            try {
                transactionRepository.insert(
                    TransactionEntity(
                        type = _type.value,
                        amount = amountValue,
                        date = _date.value,
                        categoryId = categoryId,
                        memo = _memo.value.trim().ifBlank { null }
                    )
                )

                _amount.value = ""
                _memo.value = ""
                _message.value = "저장되었습니다."

            } catch (e: Exception) {
                _message.value = "저장 실패: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
