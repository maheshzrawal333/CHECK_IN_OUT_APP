package com.maheshz.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey val id: String,
    val type: String, // "IN" or "OUT"
    val timestamp: Long,
    val verifiedBy: String
)
