package com.maheshz.checkinout.biometric

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun generateECKeyPair(employeeCode: String): String {
        val alias = "proximity_key_$employeeCode"
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)

        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            .setInvalidatedByBiometricEnrollment(true)

        keyPairGenerator.initialize(builder.build())
        val keyPair = keyPairGenerator.generateKeyPair()
        
        val publicKeyEncoded = keyPair.public.encoded
        return Base64.encodeToString(publicKeyEncoded, Base64.NO_WRAP)
    }

    fun hasKey(employeeCode: String): Boolean {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.containsAlias("proximity_key_$employeeCode")
    }
}
