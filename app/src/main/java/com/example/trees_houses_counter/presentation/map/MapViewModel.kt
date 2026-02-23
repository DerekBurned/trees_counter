package com.example.trees_houses_counter.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trees_houses_counter.domain.model.Report
import com.example.trees_houses_counter.domain.model.TreeEntry
import com.example.trees_houses_counter.domain.model.TreeSubType
import com.example.trees_houses_counter.domain.repository.AuthRepository
import com.example.trees_houses_counter.domain.usecase.GetUserReportsUseCase
import com.example.trees_houses_counter.domain.usecase.SubmitReportUseCase
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val submitReportUseCase: SubmitReportUseCase,
    private val getUserReportsUseCase: GetUserReportsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    init {
        loadUserReports()
    }

    private fun loadUserReports() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            getUserReportsUseCase(userId).collect { reports ->
                _reports.value = reports
            }
        }
    }

    fun onMapLongClick(position: LatLng) {
        if (_mapState.value.isAreaSelected) return

        val corners = calculateRectangleCorners(position)
        _mapState.update {
            it.copy(
                selectedArea = corners,
                showDialog = false,
                isAreaSelected = true
            )
        }
    }

    fun onCornerDrag(index: Int, newPosition: LatLng) {
        _mapState.update { state ->
            val updatedCorners = state.selectedArea.toMutableList()
            updatedCorners[index] = newPosition
            state.copy(selectedArea = updatedCorners)
        }
    }

    fun onPolygonDrag(newCenter: LatLng) {
        _mapState.update { current ->
            if (current.selectedArea.size != 4) return@update current

            val oldCenterLat = current.selectedArea.map { it.latitude }.average()
            val oldCenterLng = current.selectedArea.map { it.longitude }.average()
            val deltaLat = newCenter.latitude - oldCenterLat
            val deltaLng = newCenter.longitude - oldCenterLng

            val newArea = current.selectedArea.map {
                LatLng(it.latitude + deltaLat, it.longitude + deltaLng)
            }
            current.copy(selectedArea = newArea)
        }
    }

    fun confirmAreaSelection() {
        _mapState.update {
            it.copy(
                showDialog = true,
                isAreaSelected = true
            )
        }
    }

    fun cancelAreaSelection() {
        _mapState.update {
            it.copy(
                selectedArea = emptyList(),
                isAreaSelected = false,
                showDialog = false,
                treeEntries = listOf(TreeEntry(id = 0, TreeSubType.DECIDUOUS, 0))
            )
        }
    }

    fun onAddTreeEntry(): () -> Unit = {
        val currentEntries = _mapState.value.treeEntries
        val selectedTypes = currentEntries.map { it.treeType }.toSet()
        val availableTypes = TreeSubType.entries.filter { it !in selectedTypes }

        if (availableTypes.isNotEmpty()) {
            val newEntry = TreeEntry(
                id = currentEntries.size,
                treeType = availableTypes.first(),
                quantity = 0
            )
            _mapState.update {
                it.copy(treeEntries = it.treeEntries + newEntry)
            }
        }
    }

    fun onDeleteTreeEntry(index: Int) {
        _mapState.update { state ->
            val updatedEntries = state.treeEntries.toMutableList().apply {
                removeAt(index)
            }.mapIndexed { i, entry -> entry.copy(id = i) }
            state.copy(treeEntries = updatedEntries)
        }
    }

    fun onTreeTypeChange(index: Int, newType: TreeSubType) {
        _mapState.update { state ->
            val updatedEntries = state.treeEntries.toMutableList().apply {
                this[index] = this[index].copy(treeType = newType)
            }
            state.copy(treeEntries = updatedEntries)
        }
    }

    fun onTreeQuantityChange(index: Int, quantity: Int) {
        _mapState.update { state ->
            val updatedEntries = state.treeEntries.toMutableList().apply {
                this[index] = this[index].copy(quantity = quantity)
            }
            state.copy(treeEntries = updatedEntries)
        }
    }

    fun submitReport() {
        val state = _mapState.value
        val userId = authRepository.currentUserId ?: return

        if (state.selectedArea.size != 4) return
        if (state.treeEntries.isEmpty() || state.treeEntries.all { it.quantity == 0 }) return

        val validEntries = state.treeEntries.filter { it.quantity > 0 }
        val totalQuantity = validEntries.sumOf { it.quantity }

        // Look how much cleaner this is now!
        val report = Report(
            id = UUID.randomUUID().toString(),
            coordinates = state.selectedArea,
            userId = userId,
            totalQuantity = totalQuantity,
            entries = validEntries
        )

        viewModelScope.launch {
            _mapState.update { it.copy(isSubmitting = true) }
            submitReportUseCase(report)
                .onSuccess {
                    _mapState.value = MapState(
                        treeEntries = listOf(TreeEntry(id = 0, TreeSubType.DECIDUOUS, 0))
                    )
                    loadUserReports()
                }
                .onFailure { error ->
                    _mapState.update {
                        it.copy(
                            isSubmitting = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun dismissDialog() {
        _mapState.update {
            it.copy(
                showDialog = false,
                selectedArea = emptyList(),
                isAreaSelected = false,
                treeEntries = listOf(TreeEntry(id = 0, TreeSubType.DECIDUOUS, 0))
            )
        }
    }

    private fun calculateRectangleCorners(center: LatLng, sizeMeters: Double = 50.0): List<LatLng> {
        val latOffset = sizeMeters / 111320.0
        val lngOffset = sizeMeters / (111320.0 * Math.cos(Math.toRadians(center.latitude)))

        return listOf(
            LatLng(center.latitude + latOffset, center.longitude - lngOffset), // Top-left
            LatLng(center.latitude + latOffset, center.longitude + lngOffset), // Top-right
            LatLng(center.latitude - latOffset, center.longitude + lngOffset), // Bottom-right
            LatLng(center.latitude - latOffset, center.longitude - lngOffset)  // Bottom-left
        )
    }
}

data class MapState(
    val selectedArea: List<LatLng> = emptyList(),
    val showDialog: Boolean = false,
    val isAreaSelected: Boolean = false,
    val treeEntries: List<TreeEntry> = listOf(TreeEntry(id = 0, TreeSubType.DECIDUOUS, 0)),
    val isSubmitting: Boolean = false,
    val error: String? = null
)