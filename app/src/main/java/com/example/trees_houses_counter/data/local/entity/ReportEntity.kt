package com.example.trees_houses_counter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val coordinates: String,
    val userId: String,
    val timestamp: Long,
    val totalQuantity: Int,
    val entries: String
)