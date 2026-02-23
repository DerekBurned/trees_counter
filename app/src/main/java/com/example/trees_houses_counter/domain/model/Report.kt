package com.example.trees_houses_counter.domain.model

import com.google.android.gms.maps.model.LatLng

data class Report(
    val id: String,
    val coordinates: List<LatLng>,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalQuantity: Int,
    val entries: List<TreeEntry>
)

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