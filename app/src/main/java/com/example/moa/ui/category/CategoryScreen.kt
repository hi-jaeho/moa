
package com.example.moa.ui.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.TransactionType

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember {
        mutableStateOf<CategoryEntity?>(null)
    }
    var deletingCategory by remember {
        mutableStateOf<CategoryEntity?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "카테고리 관리",
            style = MaterialTheme.typography.headlineMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

        Button(
            onClick = { showAddDialog = true },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ 새 카테고리")
        }

        state.message?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.categories, key = { it.id }) { category ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            category.name,
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 10.dp)
                        )

                        TextButton(
                            onClick = {
                                editingCategory = category
                            },
                            enabled = !state.isSaving
                        ) {
                            Text("수정")
                        }

                        TextButton(
                            onClick = {
                                deletingCategory = category
                            },
                            enabled = !state.isSaving
                        ) {
                            Text("삭제")
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryNameDialog(
            title = "카테고리 추가",
            initialName = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name)
                showAddDialog = false
            }
        )
    }

    editingCategory?.let { category ->
        CategoryNameDialog(
            title = "카테고리 수정",
            initialName = category.name,
            onDismiss = { editingCategory = null },
            onConfirm = { name ->
                viewModel.renameCategory(category, name)
                editingCategory = null
            }
        )
    }

    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = {
                deletingCategory = null
            },
            title = { Text("카테고리 삭제") },
            text = {
                Text(
                    "'${category.name}' 카테고리를 삭제할까요?\n" +
                            "기존 거래에서 사용 중인 카테고리는 삭제할 수 없습니다."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        deletingCategory = null
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingCategory = null
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) {
        mutableStateOf(initialName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("카테고리 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() &&
                        name.trim().length <= 20
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
