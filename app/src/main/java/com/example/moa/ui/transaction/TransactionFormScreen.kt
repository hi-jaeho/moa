
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
import com.example.moa.ui.common.ThousandsSeparatorVisualTransformation
import java.time.LocalDate
@Composable
fun TransactionFormScreen(
    viewModel: TransactionViewModel
) {
    val context = LocalContext.current

    // 등록 화면에 새로 진입할 때 이전 메시지 제거
    LaunchedEffect(Unit) {
        viewModel.clearMessage()
    }

    val type by viewModel.type.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val date by viewModel.date.collectAsState()
    val memo by viewModel.memo.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val message by viewModel.message.collectAsState()

    var categoryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            text = "거래 등록",
            style = MaterialTheme.typography.headlineMedium
        )

        // 수입 / 지출 선택
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = type == TransactionType.EXPENSE,
                onClick = {
                    viewModel.setType(TransactionType.EXPENSE)
                },
                label = { Text("지출") }
            )

            FilterChip(
                selected = type == TransactionType.INCOME,
                onClick = {
                    viewModel.setType(TransactionType.INCOME)
                },
                label = { Text("수입") }
            )
        }

        // 금액
        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                viewModel.setAmount(input.filter { it.isDigit() })
            },
            label = { Text("금액") },
            suffix = { Text("원") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            visualTransformation = ThousandsSeparatorVisualTransformation,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )


        // 날짜
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        viewModel.setDate(
                            LocalDate.of(year, month + 1, day)
                        )
                    },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("날짜: $date")
        }

        // 카테고리
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { categoryExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = categories.isNotEmpty()
            ) {
                val selected = categories.find {
                    it.id == selectedCategoryId
                }

                Text(selected?.name ?: "카테고리 선택")
            }

            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = {
                    categoryExpanded = false
                }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            viewModel.selectCategory(category.id)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // 메모
        OutlinedTextField(
            value = memo,
            onValueChange = viewModel::setMemo,
            label = { Text("메모 (선택)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        // 저장
        Button(
            onClick = viewModel::saveTransaction,
            enabled = !isSaving && categories.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "저장 중..." else "저장하기")
        }

        // 저장 결과
        message?.let { currentMessage ->
            Text(
                text = currentMessage,
                color = if (currentMessage == "저장되었습니다.") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}
