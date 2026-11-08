
package com.example.moa.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.moa.data.settings.SecuritySettings
import com.example.moa.data.settings.ThemeMode

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    securitySettings: SecuritySettings,
    onLockChange: (Boolean, String) -> Unit,
    onBiometricChange: (Boolean, String) -> Unit,
    biometricAvailable: Boolean,
    onPinChange: (String, String, String) -> Unit,
    securityMessage: String?,
    pinChangeSuccess: Boolean,
    onResetPinChangeSuccess: () -> Unit
) {
    // 앱 잠금 설정
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingLockEnabled by remember { mutableStateOf(true) }
    var currentPin by remember { mutableStateOf("") }

    // 생체인증 설정
    var showBiometricPinDialog by remember { mutableStateOf(false) }
    var pendingBiometricEnabled by remember { mutableStateOf(false) }
    var biometricPin by remember { mutableStateOf("") }

    // PIN 변경
    var showChangePinDialog by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    LaunchedEffect(pinChangeSuccess) {
        if (pinChangeSuccess) {
            showChangePinDialog = false
            oldPin = ""
            newPin = ""
            confirmPin = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "설정",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 화면 설정
        Text(
            text = "화면 설정",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.selectableGroup()
        ) {
            ThemeOption(
                title = "시스템 설정에 따르기",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeChange(ThemeMode.SYSTEM) }
            )

            ThemeOption(
                title = "라이트 모드",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeChange(ThemeMode.LIGHT) }
            )

            ThemeOption(
                title = "다크 모드",
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeChange(ThemeMode.DARK) }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // 보안 설정
        Text(
            text = "보안 설정",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 앱 잠금
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("앱 잠금")

            Switch(
                checked = securitySettings.lockEnabled,
                onCheckedChange = { enabled ->
                    pendingLockEnabled = enabled
                    currentPin = ""
                    showPinDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 생체인증
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("생체인증 사용")

            Switch(
                checked = securitySettings.biometricEnabled,
                enabled = securitySettings.lockEnabled &&
                        biometricAvailable,
                onCheckedChange = { enabled ->
                    pendingBiometricEnabled = enabled
                    biometricPin = ""
                    showBiometricPinDialog = true
                }
            )
        }

        if (!biometricAvailable) {
            Text(
                text = "사용 가능한 생체인증이 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PIN 변경 버튼
        OutlinedButton(
            onClick = {
                onResetPinChangeSuccess()
                oldPin = ""
                newPin = ""
                confirmPin = ""
                showChangePinDialog = true
            },
            enabled = securitySettings.lockEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("PIN 변경")
        }

        // 보안 관련 메시지
        if (securityMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = securityMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // 앱 잠금 ON/OFF 확인
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                currentPin = ""
                showPinDialog = false
            },
            title = {
                Text("현재 PIN 확인")
            },
            text = {
                PinInputField(
                    value = currentPin,
                    onValueChange = { currentPin = it },
                    label = "현재 PIN"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = currentPin.length == 6,
                    onClick = {
                        onLockChange(
                            pendingLockEnabled,
                            currentPin
                        )
                        currentPin = ""
                        showPinDialog = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        currentPin = ""
                        showPinDialog = false
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }

    // 생체인증 ON/OFF 확인
    if (showBiometricPinDialog) {
        AlertDialog(
            onDismissRequest = {
                biometricPin = ""
                showBiometricPinDialog = false
            },
            title = {
                Text("생체인증 설정")
            },
            text = {
                Column {
                    Text("설정을 변경하려면 현재 PIN을 입력하세요.")

                    Spacer(modifier = Modifier.height(12.dp))

                    PinInputField(
                        value = biometricPin,
                        onValueChange = { biometricPin = it },
                        label = "현재 PIN"
                    )

                    if (securityMessage != null && !pinChangeSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = securityMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = biometricPin.length == 6,
                    onClick = {
                        onBiometricChange(
                            pendingBiometricEnabled,
                            biometricPin
                        )
                        biometricPin = ""
                        showBiometricPinDialog = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        biometricPin = ""
                        showBiometricPinDialog = false
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }

    // PIN 변경 다이얼로그
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                oldPin = ""
                newPin = ""
                confirmPin = ""
                showChangePinDialog = false
            },
            title = {
                Text("PIN 변경")
            },
            text = {
                Column {
                    PinInputField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = "현재 PIN"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PinInputField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = "새 PIN"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PinInputField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = "새 PIN 확인"
                    )

                    if (
                        newPin.length == 6 &&
                        confirmPin.length == 6 &&
                        newPin != confirmPin
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "새 PIN이 일치하지 않습니다.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = oldPin.length == 6 &&
                            newPin.length == 6 &&
                            confirmPin.length == 6 &&
                            newPin == confirmPin,
                    onClick = {
                        onPinChange(
                            oldPin,
                            newPin,
                            confirmPin
                        )
                    }
                ) {
                    Text("변경")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        oldPin = ""
                        newPin = ""
                        confirmPin = ""
                        showChangePinDialog = false
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(
                it.filter(Char::isDigit).take(6)
            )
        },
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)

        RadioButton(
            selected = selected,
            onClick = null
        )
    }
}
