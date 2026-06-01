package com.maheshz.checkinout.biometric

import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature

object FingerprintSigner {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun getSignatureObject(employeeCode: String): Signature? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val alias = "proximity_key_$employeeCode"
            val privateKey = keyStore.getKey(alias, null) as? PrivateKey ?: return null
            
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            signature
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
