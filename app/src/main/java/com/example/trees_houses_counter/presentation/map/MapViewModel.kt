package com.example.trees_houses_counter.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trees_houses_counter.domain.model.Report
import com.example.trees_houses_counter.domain.model.ReportData
import com.example.trees_houses_counter.domain.model.ReportType
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
        // Only allow new selection if not currently editing
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
                isAreaSelected = false
            )
        }
    }

    fun cancelAreaSelection() {
        _mapState.update {
            it.copy(
                selectedArea = emptyList(),
                isAreaSelected = false,
                showDialog = false
            )
        }
    }

    fun onTypeSelected(type: ReportType) {
        _mapState.update { it.copy(selectedType = type) }
    }

    fun onQuantityChanged(quantity: Int) {
        _mapState.update { it.copy(quantity = quantity) }
    }

    fun onHeightChanged(height: Double) {
        _mapState.update { it.copy(height = height) }
    }

    fun submitReport() {
        val state = _mapState.value
        val userId = authRepository.currentUserId ?: return

        if (state.selectedArea.size != 4) return

        val reportData = when (state.selectedType) {
            ReportType.TREES -> ReportData.TreesData(state.quantity)
            ReportType.HOUSE -> ReportData.HouseData(state.height)
        }

        val report = Report(
            id = UUID.randomUUID().toString(),
            type = state.selectedType,
            coordinates = state.selectedArea,  // Exact shape saved ✅
            userId = userId,
            data = reportData
        )

        viewModelScope.launch {
            _mapState.update { it.copy(isSubmitting = true) }
            submitReportUseCase(report)
                .onSuccess {
                    _mapState.value = MapState()  // Reset form
                    loadUserReports()  // Refresh reports list ✅
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
                isAreaSelected = false
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
    val selectedType: ReportType = ReportType.TREES,
    val quantity: Int = 0,
    val height: Double = 0.0,
    val isSubmitting: Boolean = false,
    val error: String? = null
)
