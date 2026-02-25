package com.example.trees_houses_counter.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.trees_houses_counter.data.local.dao.ReportDao
import com.example.trees_houses_counter.data.local.entity.ReportEntity

@Database(
    entities = [ReportEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}
