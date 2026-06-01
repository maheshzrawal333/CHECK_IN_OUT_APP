package com.maheshz.checkinout.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature

object FingerprintSecurityManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    // 1. Generate the strictly bound key during the FIRST time registration
    fun generateStrictBiometricKey(alias: String) {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        // Delete any existing corrupted key first
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true) // Forces biometric prompt
            // 🌟 THE SECURITY LOCK: If ANY finger is added/removed in Android Settings, this key dies.
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()
    }

    // 2. Fetch the signature object for check-in
    fun getStrictSignatureObject(alias: String): SignatureResult {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        try {
            val privateKey = keyStore.getKey(alias, null) as? PrivateKey
                ?: return SignatureResult.Error("Key not found. Please register device.")

            val signature = Signature.getInstance("SHA256withECDSA").apply {
                initSign(privateKey)
            }
            return SignatureResult.Ready(signature)

        } catch (e: KeyPermanentlyInvalidatedException) {
            // 🚨 SECURITY BREACH DETECTED: Someone altered the device fingerprints!
            // We must delete the corrupted key to prevent continuous crashing.
            keyStore.deleteEntry(alias)
            return SignatureResult.Invalidated
        } catch (e: Exception) {
            return SignatureResult.Error(e.message ?: "Unknown security error")
        }
    }

    sealed class SignatureResult {
        data class Ready(val signature: Signature) : SignatureResult()
        object Invalidated : SignatureResult()
        data class Error(val message: String) : SignatureResult()
    }

    // 🌟 ADD THIS: Extracts the base64 public key from the generated Keystore EC KeyPair
    fun getPublicKeyBase64(alias: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val certificate = keyStore.getCertificate(alias) ?: throw Exception("Key not found")
        val publicKey = certificate.publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }
}