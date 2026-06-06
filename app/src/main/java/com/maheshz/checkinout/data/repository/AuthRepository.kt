package com.maheshz.checkinout.data.repository

import com.maheshz.checkinout.data.remote.ApiService
import com.maheshz.checkinout.util.DataStoreManager
// 🌟 REQUIRED IMPORTS ADDED
import retrofit2.Response

class AuthRepository(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) {


    suspend fun logout() {
        try { apiService.logout() } catch (e: Exception) { }
        dataStoreManager.clear()
    }

    suspend fun deleteAccount() {
        try { apiService.deleteAccount() } catch(e: Exception) { }
        dataStoreManager.clear()
    }

    suspend fun activateDeviceWithCode(code: String, pubKey: String, deviceFp: String): Response<Map<String, String>> {
        val payload = mapOf(
            "activationCode" to code,
            "fpPublicKey" to pubKey,
            "deviceFingerprint" to deviceFp
        )
        return apiService.activateDeviceWithCode(payload) // Assuming your ApiService method is named this
    }
}