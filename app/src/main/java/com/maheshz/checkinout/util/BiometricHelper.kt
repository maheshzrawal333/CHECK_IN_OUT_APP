package com.maheshz.checkinout.util

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature

object BiometricHelper {

    // 1. Fetches the Private Key and initializes the Signature engine
    private fun createSignature(): Signature? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            // NOTE: "EmployeeKeyAlias" must match the name you used when generating the key during registration!
            val privateKey = keyStore.getKey("EmployeeKeyAlias", null) as? PrivateKey

            privateKey?.let {
                Signature.getInstance("SHA256withECDSA").apply {
                    initSign(it)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // 2. Launches the UI prompt and returns the result via a callback lambda
    fun authenticateAndTransmit(
        activity: FragmentActivity,
        employeeId: String,
        onSuccess: (String, ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val signature = createSignature()

        if (signature == null) {
            onError("Cryptographic key not found. Please re-register your device.")
            return
        }

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    try {
                        val unlockedSignature = result.cryptoObject?.signature
                        val payload = "CHECK_IN:$employeeId:${System.currentTimeMillis()}"

                        unlockedSignature?.update(payload.toByteArray(Charsets.UTF_8))
                        val signedBytes = unlockedSignature?.sign()

                        if (signedBytes != null) {
                            onSuccess(payload, signedBytes)
                        } else {
                            onError("Failed to generate digital signature.")
                        }
                    } catch (e: Exception) {
                        onError("Security exception during signing.")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Employee Check-In")
            .setSubtitle("Verify identity to transmit signal")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(signature))
    }
}