package com.maheshz.checkinout.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.maheshz.checkinout.model.AttendanceRecord

@Database(entities = [AttendanceRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}
