
package com.example.moa.ui.transaction

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moa.data.local.entity.TransactionType
import java.time.LocalDate

@Composable
fun TransactionEditScreen(
    viewModel: TransactionEditViewModel,
    transactionId: Long,
    onBack: () -> Unit,
    onFinished: (LocalDate) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var categoryExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("거래 삭제") },
            text = { Text("이 거래 내역을 삭제할까요? 삭제한 거래는 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val date = state.transaction?.date ?: state.date
                        viewModel.delete {
                            onFinished(date)
                        }
                    }
                ) {
                    Text(
                        text = "삭제",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("취소")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("〈 돌아가기")
        }

        Text(
            text = "거래 상세 및 수정",
            style = MaterialTheme.typography.headlineMedium
        )

        if (state.isLoading) {
            CircularProgressIndicator()
        } else if (state.transaction == null) {
            Text(state.error ?: "거래를 찾을 수 없습니다.")
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = {
                        viewModel.setType(TransactionType.EXPENSE)
                    },
                    label = { Text("지출") }
                )

                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = {
                        viewModel.setType(TransactionType.INCOME)
                    },
                    label = { Text("수입") }
                )
            }

            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::setAmount,
                label = { Text("금액") },
                suffix = { Text("원") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            viewModel.setDate(
                                LocalDate.of(year, month + 1, day)
                            )
                        },
                        state.date.year,
                        state.date.monthValue - 1,
                        state.date.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("날짜: ${state.date}")
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { categoryExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.categories.isNotEmpty()
                ) {
                    val selected = state.categories.find {
                        it.id == state.categoryId
                    }
                    Text(selected?.name ?: "카테고리 선택")
                }

                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.setCategory(category.id)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.memo,
                onValueChange = viewModel::setMemo,
                label = { Text("메모 (선택)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val editedDate = state.date
                    viewModel.save {
                        onFinished(editedDate)
                    }
                },
                enabled = !state.isSaving &&
                        state.categories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "처리 중..." else "수정 저장")
            }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("거래 삭제")
            }
        }
    }
}
