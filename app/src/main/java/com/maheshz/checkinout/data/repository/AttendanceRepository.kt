package com.maheshz.checkinout.data.repository

import com.maheshz.checkinout.data.local.AttendanceDao
import com.maheshz.checkinout.data.remote.ApiService // 🌟 ADDED IMPORT
import com.maheshz.checkinout.data.remote.ScanStatusResponse
import com.maheshz.checkinout.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

class AttendanceRepository(
    private val dao: AttendanceDao,
    private val apiService: ApiService // 🌟 FIXED: Added apiService to the constructor!
) {
    fun getAllRecords(): Flow<List<AttendanceRecord>> = dao.getAllRecords()

    suspend fun insertRecord(record: AttendanceRecord) = dao.insertRecord(record)

    // 🌟 This will now work perfectly
    suspend fun checkLatestScanStatus(empCode: String): Response<ScanStatusResponse> {
        return apiService.checkLatestScanStatus(empCode)
    }
}