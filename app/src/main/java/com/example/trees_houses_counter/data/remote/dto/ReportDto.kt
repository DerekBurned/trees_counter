package com.example.trees_houses_counter.data.remote.dto

data class ReportDto(
    val id: String = "",
    val coordinates: List<CoordinateDto> = emptyList(),
    val userId: String = "",
    val timestamp: Long = 0L,
    val totalQuantity: Int = 0,
    val entries: List<TreeEntryDto> = emptyList()
)

data class CoordinateDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class TreeEntryDto(
    val id: Int = 0,
    val treeType: String = "",
    val quantity: Int = 0
)