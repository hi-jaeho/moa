
package com.example.moa.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var pendingJson by remember { mutableStateOf<String?>(null) }
    var fileBusy by remember { mutableStateOf(false) }

    val createFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingJson
        pendingJson = null

        if (uri != null && json != null) {
            scope.launch {
                fileBusy = true
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver
                            .openOutputStream(uri)
                            ?: error("파일을 열 수 없습니다.")

                        stream.bufferedWriter(Charsets.UTF_8).use {
                            it.write(json)
                        }
                    }
                    viewModel.showMessage("백업 파일을 저장했습니다.")
                } catch (e: Exception) {
                    viewModel.showMessage(
                        "파일 저장 실패: ${e.message}"
                    )
                } finally {
                    fileBusy = false
                }
            }
        }
    }

    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                fileBusy = true
                try {
                    val json = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver
                            .openInputStream(uri)
                            ?: error("파일을 열 수 없습니다.")

                        stream.bufferedReader(Charsets.UTF_8).use {
                            it.readText()
                        }
                    }

                    viewModel.prepareRestore(json)
                } catch (e: Exception) {
                    viewModel.showMessage(
                        "파일 읽기 실패: ${e.message}"
                    )
                } finally {
                    fileBusy = false
                }
            }
        }
    }

    state.pendingRestore?.let { data ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("데이터 복원 확인") },
            text = {
                Text(
                    "카테고리 ${data.categories.size}개와 " +
                            "거래 ${data.transactions.size}개, " +
                            "고정비 ${data.fixedExpenses.size}개를 복원합니다.\n\n" +
                            "현재 저장된 모든 거래와 카테고리가 " +
                            "백업 파일의 데이터로 교체됩니다.\n\n" +
                            "계속하시겠습니까?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmRestore
                ) {
                    Text("복원 실행")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelRestore
                ) {
                    Text("취소")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "백업 및 복원",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Moa의 거래 내역, 카테고리, 보유금액과 고정비 설정을 " +
                "JSON 파일로 보관할 수 있습니다."
        )

        Button(
            onClick = {
                viewModel.createBackup { json ->
                    pendingJson = json

                    val date = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    )

                    createFile.launch("moa_backup_$date.json")
                }
            },
            enabled = !state.isBusy && !fileBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("JSON 백업 파일 저장")
        }

        OutlinedButton(
            onClick = {
                openFile.launch(arrayOf("application/json", "text/plain"))
            },
            enabled = !state.isBusy && !fileBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("JSON 백업 파일 복원")
        }

        if (state.isBusy || fileBusy) {
            CircularProgressIndicator()
        }

        state.message?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        Text(
            "복원 시 기존 데이터는 삭제되고 백업 파일의 데이터로 교체됩니다. " +
                    "복원 전에 현재 데이터를 별도로 백업하는 것을 권장합니다.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
