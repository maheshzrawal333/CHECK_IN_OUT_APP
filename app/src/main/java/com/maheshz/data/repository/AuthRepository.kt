package com.maheshz.data.repository

import com.maheshz.data.remote.*
import com.maheshz.util.DataStoreManager

class AuthRepository(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) {
    suspend fun register(
        orgCode: String,
        empCode: String,
        fullName: String,
        email: String,
        phone: String,
        department: String,
        designation: String,
        fpPublicKey: String
    ): RegisterResponse {
        val resp = apiService.register(
            RegisterRequest(
                orgCode, empCode, fullName, email, phone, department, designation, fpPublicKey
            )
        )
        dataStoreManager.saveAuthData(
            resp.employee_id,
            resp.org_id,
            empCode,
            orgCode,
            fullName,
            resp.jwt_access_token,
            resp.jwt_refresh_token
        )
        return resp
    }

    suspend fun logout() {
        try { apiService.logout() } catch (e: Exception) { }
        dataStoreManager.clear()
    }
    
    suspend fun deleteAccount() {
        try { apiService.deleteAccount() } catch(e: Exception) { }
        dataStoreManager.clear()
    }

    suspend fun registerSimplified(
        orgCode: String, fullName: String, email: String, fpPublicKey: String, deviceFingerprint: String
    ): String {
        val resp = apiService.registerSimplified(
            SimplifiedRegisterRequest(orgCode, fullName, email, fpPublicKey, deviceFingerprint)
        )

        // Save structural metadata without tokens yet
        dataStoreManager.savePendingData(
            empId = resp.employee_id,
            empCode = resp.employee_code,
            orgCode = orgCode,
            name = fullName
        )
        return "PENDING_APPROVAL"
    }
}
