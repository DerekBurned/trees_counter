package com.example.trees_houses_counter.domain.model

import com.google.android.gms.maps.model.LatLng

data class Report(
    val id: String = "",
    val type: ReportType,
    val coordinates: List<LatLng>, // 4 corner points
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val data: ReportData
)

enum class ReportType {
    TREES, HOUSE
}

sealed class ReportData {
    data class TreesData(val quantity: Int) : ReportData()
    data class HouseData(val height: Double) : ReportData()
}

data class User(
    val id: String,
    val phoneNumber: String
)
