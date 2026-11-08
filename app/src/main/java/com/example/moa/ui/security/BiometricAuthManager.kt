
package com.example.moa.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(
    private val activity: FragmentActivity
) {
    private val biometricManager = BiometricManager.from(activity)

    fun isAvailable(): Boolean {
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAvailable()) {
            onError("사용 가능한 생체인증이 없습니다.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Moa 잠금 해제")
            .setSubtitle("생체인증으로 가계부에 접근합니다.")
            .setNegativeButtonText("PIN으로 입력")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
