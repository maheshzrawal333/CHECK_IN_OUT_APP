package com.maheshz.checkinout.data.repository

import com.maheshz.checkinout.data.local.AttendanceDao
import com.maheshz.checkinout.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val dao: AttendanceDao) {
    fun getAllRecords(): Flow<List<AttendanceRecord>> = dao.getAllRecords()
    suspend fun insertRecord(record: AttendanceRecord) = dao.insertRecord(record)
}
