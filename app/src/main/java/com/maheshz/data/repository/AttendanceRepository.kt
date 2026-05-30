package com.maheshz.data.repository

import com.maheshz.data.local.AttendanceDao
import com.maheshz.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val dao: AttendanceDao) {
    fun getAllRecords(): Flow<List<AttendanceRecord>> = dao.getAllRecords()
    suspend fun insertRecord(record: AttendanceRecord) = dao.insertRecord(record)
}
