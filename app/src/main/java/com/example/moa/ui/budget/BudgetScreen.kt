
package com.example.moa.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    var editingCategoryId by remember {
        mutableStateOf<Long?>(null)
    }
    var amountInput by remember {
        mutableStateOf("")
    }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.KOREA)
    }

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy년 M월")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "예산 관리",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = viewModel::previousMonth) {
                Text("〈 이전")
            }

            Text(
                text = state.yearMonth.format(monthFormatter),
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(onClick = viewModel::nextMonth) {
                Text("다음 〉")
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("설정된 예산 합계")

                Text(
                    text = "${numberFormat.format(state.totalBudget)}원",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = {
                        state.usageRate.coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))


                Text(
                    text = "예산 내 사용: ${numberFormat.format(state.budgetedSpent)}원"
                )

                Text(
                    text = "이번 달 총지출: ${numberFormat.format(state.totalSpent)}원"
                )


                Text(
                    text = "잔여: ${numberFormat.format(state.totalRemaining)}원",
                    color = if (state.totalRemaining < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "카테고리별 예산",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        if (state.categories.isEmpty()) {
            Text(
                text = "등록된 예산이 없습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.categories,
                    key = { it.categoryId }
                ) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = category.categoryName,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = if (category.budgetAmount == null) {
                                    "예산 미설정"
                                } else {
                                    "예산: ${numberFormat.format(category.budgetAmount)}원"
                                }
                            )

                            Text(
                                text = "지출: ${numberFormat.format(category.spentAmount)}원"
                            )

                            Spacer(Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = {
                                    category.usageRate.coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                OutlinedButton(
                                    onClick = {
                                        editingCategoryId = category.categoryId
                                        amountInput = category.budgetAmount?.toString() ?: ""
                                    }
                                ) {
                                    Text(
                                        if (category.budgetAmount == null) {
                                            "예산 설정"
                                        } else {
                                            "수정"
                                        }
                                    )
                                }
                                if (category.budgetAmount != null) {
                                    TextButton(
                                        onClick = {
                                            viewModel.deleteBudget(category.categoryId)
                                        }
                                    ) {
                                        Text("삭제")
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    if (editingCategoryId != null) {
        AlertDialog(
            onDismissRequest = {
                editingCategoryId = null
            },
            title = {
                Text("예산 설정")
            },
            text = {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it.filter(Char::isDigit)
                    },
                    label = { Text("예산 금액") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = amountInput.toLongOrNull() != null,
                    onClick = {
                        val categoryId = editingCategoryId
                        val amount = amountInput.toLongOrNull()

                        if (categoryId != null && amount != null) {
                            viewModel.saveBudget(categoryId, amount)
                            editingCategoryId = null
                        }
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingCategoryId = null
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }
}
