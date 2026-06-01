package com.maheshz.checkinout.data.repository

import com.maheshz.checkinout.data.remote.ApiService
import com.maheshz.checkinout.data.remote.RegisterRequest
import com.maheshz.checkinout.data.remote.RegisterResponse
import com.maheshz.checkinout.data.remote.SimplifiedRegisterRequest
import com.maheshz.checkinout.util.DataStoreManager
// 🌟 REQUIRED IMPORTS ADDED
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.Response

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
        val response = apiService.registerSimplified(
            SimplifiedRegisterRequest(orgCode, fullName, email, fpPublicKey, deviceFingerprint)
        )

        if (!response.isSuccessful) {
            val errBody = response.errorBody()?.string() ?: ""
            if (errBody.contains("Organization Code does not exist")) throw Exception("Organization Code does not exist.")
            if (errBody.contains("This email is already registered")) throw Exception("Email already used for this organization.")
            // 🌟 CORRECTED: Matches the updated server error string mapping
            if (errBody.contains("Request limit exceeded")) throw Exception("Request limit exceeded: Max 3 requests per device within 24 hours.")
            throw Exception("Registration failed. Please try again.")
        }

        val resp = response.body()!!
        dataStoreManager.savePendingData(
            empId = resp["employee_id"] ?: "",
            empCode = resp["employee_code"] ?: "",
            orgCode = orgCode,
            name = fullName
        )
        return "PENDING_APPROVAL"
    }

    suspend fun checkStatus(): String {
        // 🌟 FIXED: Extracted flow states asynchronously without passing an incorrect filter block
        val empCode = dataStoreManager.employeeCodeFlow.firstOrNull() ?: return "REJECTED"
        val orgCode = dataStoreManager.orgCodeFlow.firstOrNull() ?: return "REJECTED"
        val fp = dataStoreManager.getDeviceFingerprint()

        val response = apiService.checkStatus(empCode, orgCode, fp)
        if (response.isSuccessful) {
            val body = response.body()
            val status = body?.get("status") ?: "PENDING"

            if (status == "APPROVED") {
                val access = body?.get("access_token") ?: ""
                val refresh = body?.get("refresh_token") ?: ""
                dataStoreManager.saveTokens(access, refresh)
            } else if (status == "REJECTED") {
                dataStoreManager.clear() // Admin rejected, wipe pending data to return to login
            }
            return status
        }
        return "ERROR"
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