package com.example.trees_houses_counter.domain.model

import com.google.android.gms.maps.model.LatLng

data class Report(
    val id: String = "",
    val type: ReportType,
    val coordinates: List<LatLng>,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val data: ReportData
)

enum class ReportType {
    TREES
}

sealed class ReportData {
    data class TreesData(val quantity: Int) : ReportData()
}

data class User(
    val id: String,
    val phoneNumber: String
)
data class TreeEntry(
    val id: Int,
    val treeType: TreeSubType = TreeSubType.DECIDUOUS,
    val quantity: Int = 0
)

enum class TreeSubType {
    DECIDUOUS,  // Liściaste
    CONIFEROUS  // Iglaste
}
