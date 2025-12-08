package com.example.trees_houses_counter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.trees_houses_counter.data.local.converters.Converters

@Entity(tableName = "reports")
@TypeConverters(Converters::class)
data class ReportEntity(
    @PrimaryKey val id: String,
    val type: String, // TREES or HOUSE
    val coordinates: String, // JSON string of coordinates
    val userId: String,
    val timestamp: Long,
    val quantity: Int?, // For trees
    val height: Double? // For house
)
