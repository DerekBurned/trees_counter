package com.example.trees_houses_counter.data.remote.dto

data class ReportDto(
    val id: String = "",
    val type: String = "",
    val coordinates: List<CoordinateDto> = emptyList(),
    val userId: String = "",
    val timestamp: Long = 0L,
    val quantity: Int? = null,
    val height: Double? = null
)

data class CoordinateDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
