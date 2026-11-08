
package com.example.moa.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
fun PinScreen(
    status: PinStatus,
    error: String?,
    onSubmit: (String) -> Unit,
    onBiometricClick: () -> Unit,
    biometricAvailable: Boolean,
    lockoutUntil: Long,
) {
    var pin by remember(status, error) {
        mutableStateOf("")
    }

    val title = when (status) {
        PinStatus.SETUP -> "PIN 설정"
        PinStatus.CONFIRM -> "PIN 확인"
        PinStatus.LOCKED -> "Moa 잠금 해제"
        else -> "Moa"
    }

    val description = when (status) {
        PinStatus.SETUP -> "사용할 6자리 PIN을 입력하세요."
        PinStatus.CONFIRM -> "PIN을 한 번 더 입력하세요."
        PinStatus.LOCKED -> "PIN을 입력해 가계부를 열어주세요."
        else -> ""
    }


    var remainingSeconds by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(lockoutUntil) {
        while (true) {
            val remaining =
                lockoutUntil - System.currentTimeMillis()

            remainingSeconds = if (remaining > 0) {
                (remaining + 999L) / 1000L
            } else {
                0L
            }

            if (remainingSeconds == 0L) break

            delay(1000L)
        }
    }

    val isLockedOut = remainingSeconds > 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { input ->
                pin = input.filter { it.isDigit() }.take(6)
            },
            label = { Text("6자리 PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            singleLine = true,
            enabled = !isLockedOut
        )

        if (status == PinStatus.LOCKED && biometricAvailable) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBiometricClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("지문으로 잠금 해제")
            }
        }


        if (isLockedOut) {
            Text(
                text = "${remainingSeconds}초 후 다시 시도하세요.",
                color = MaterialTheme.colorScheme.error
            )
        } else if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }


        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSubmit(pin)
                pin = ""
            },
            enabled = !isLockedOut && pin.length == 6,
            modifier = Modifier.fillMaxWidth()

        ) {
            Text(
                when (status) {
                    PinStatus.SETUP -> "다음"
                    PinStatus.CONFIRM -> "PIN 저장"
                    else -> "잠금 해제"
                }
            )
        }
    }
}
