package com.maheshz.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.maheshz.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getRecordsForTimeRange(startTime: Long, endTime: Long): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}
