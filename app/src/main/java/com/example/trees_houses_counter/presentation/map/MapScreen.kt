package com.example.trees_houses_counter.presentation.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.trees_houses_counter.domain.model.ReportType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onSignOut: () -> Unit
) {
    val mapState by viewModel.mapState.collectAsState()
    val reports by viewModel.reports.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Map") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.ExitToApp, "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MapContent(
                reports = reports,
                selectedArea = mapState.selectedArea,
                isAreaSelected = mapState.isAreaSelected,
                onMapLongClick = viewModel::onMapLongClick,
                onCornerDrag = viewModel::onCornerDrag,
                onPolygonDrag = viewModel::onPolygonDrag
            )

            if (mapState.isAreaSelected && mapState.selectedArea.isNotEmpty()) {
                AreaConfirmationButtons(
                    onConfirm = viewModel::confirmAreaSelection,
                    onCancel = viewModel::cancelAreaSelection,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            if (mapState.showDialog) {
                ReportDialog(
                    selectedType = mapState.selectedType,
                    quantity = mapState.quantity,
                    height = mapState.height,
                    onTypeSelected = viewModel::onTypeSelected,
                    onQuantityChanged = viewModel::onQuantityChanged,
                    onHeightChanged = viewModel::onHeightChanged,
                    onSubmit = viewModel::submitReport,
                    onDismiss = viewModel::dismissDialog,
                    isSubmitting = mapState.isSubmitting
                )
            }
        }
    }
}

@Composable
fun AreaConfirmationButtons(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Drag center marker to move • Drag corners to resize",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
fun MapContent(
    reports: List<com.example.trees_houses_counter.domain.model.Report>,
    selectedArea: List<LatLng>,
    isAreaSelected: Boolean,
    onMapLongClick: (LatLng) -> Unit,
    onCornerDrag: (Int, LatLng) -> Unit,
    onPolygonDrag: (LatLng) -> Unit
) {
    val defaultPosition = LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(zoomControlsEnabled = false),
        onMapLongClick = onMapLongClick
    ) {
        // Draw existing reports
        reports.forEach { report ->
            Polygon(
                points = report.coordinates,
                strokeColor = Color.Blue,
                fillColor = Color.Blue.copy(alpha = 0.3f),
                strokeWidth = 2f
            )
        }

        // Draw selected area
        if (selectedArea.size == 4) {
            Polygon(
                points = selectedArea,
                strokeColor = Color.Red,
                fillColor = Color.Red.copy(alpha = 0.3f),
                strokeWidth = 3f
            )

            // Calculate center of polygon for the drag marker
            val centerLat = selectedArea.map { it.latitude }.average()
            val centerLng = selectedArea.map { it.longitude }.average()
            val centerPosition = LatLng(centerLat, centerLng)

            // Add center marker for dragging the whole polygon
            CenterDragMarker(
                initialPosition = centerPosition,
                onPositionChange = { newCenter ->
                    // Calculate the delta between old and new center
                    val deltaLat = newCenter.latitude - centerLat
                    val deltaLng = newCenter.longitude - centerLng
                    onPolygonDrag(LatLng(deltaLat, deltaLng))
                }
            )

            // Draw draggable corner markers
            selectedArea.forEachIndexed { index, position ->
                DraggableCornerMarker(
                    position = position,
                    title = "Corner ${index + 1}",
                    onPositionChange = { newPosition ->
                        onCornerDrag(index, newPosition)
                    }
                )
            }
        }
    }
}

@Composable
fun CenterDragMarker(
    initialPosition: LatLng,
    onPositionChange: (LatLng) -> Unit
) {
    var previousPosition by remember(initialPosition) { mutableStateOf(initialPosition) }
    val markerState = remember(initialPosition) {
        MarkerState(position = initialPosition)
    }

    LaunchedEffect(markerState) {
        snapshotFlow { markerState.position }
            .distinctUntilChanged()
            .collect { newPosition ->
                if (newPosition != previousPosition) {
                    onPositionChange(newPosition)
                    previousPosition = newPosition
                }
            }
    }

    Marker(
        state = markerState,
        title = "Drag to Move",
        draggable = true,
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
    )
}

@Composable
fun DraggableCornerMarker(
    position: LatLng,
    title: String,
    onPositionChange: (LatLng) -> Unit
) {
    val markerState = remember(position) {
        MarkerState(position = position)
    }

    LaunchedEffect(markerState) {
        snapshotFlow { markerState.position }
            .distinctUntilChanged()
            .collect { newPosition ->
                onPositionChange(newPosition)
            }
    }

    Marker(
        state = markerState,
        title = title,
        draggable = true,
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    )
}

@Composable
fun ReportDialog(
    selectedType: ReportType,
    quantity: Int,
    height: Double,
    onTypeSelected: (ReportType) -> Unit,
    onQuantityChanged: (Int) -> Unit,
    onHeightChanged: (Double) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Report") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Type:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == ReportType.TREES,
                        onClick = { onTypeSelected(ReportType.TREES) },
                        label = { Text("Trees") }
                    )
                    FilterChip(
                        selected = selectedType == ReportType.HOUSE,
                        onClick = { onTypeSelected(ReportType.HOUSE) },
                        label = { Text("House") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedType) {
                    ReportType.TREES -> {
                        OutlinedTextField(
                            value = if (quantity > 0) quantity.toString() else "",
                            onValueChange = { onQuantityChanged(it.toIntOrNull() ?: 0) },
                            label = { Text("Quantity") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ReportType.HOUSE -> {
                        OutlinedTextField(
                            value = if (height > 0) height.toString() else "",
                            onValueChange = { onHeightChanged(it.toDoubleOrNull() ?: 0.0) },
                            label = { Text("Height (meters)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting &&
                        ((selectedType == ReportType.TREES && quantity > 0) ||
                                (selectedType == ReportType.HOUSE && height > 0))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
