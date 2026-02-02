package com.example.trees_houses_counter.presentation.map

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.trees_houses_counter.domain.model.Report
import com.example.trees_houses_counter.domain.model.ReportData
import com.example.trees_houses_counter.domain.model.ReportType
import com.example.trees_houses_counter.domain.model.TreeEntry
import com.example.trees_houses_counter.domain.model.TreeSubType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
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
                    onTypeSelected = viewModel::onTypeSelected,
                    onQuantityChanged = viewModel::onQuantityChanged,
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
    reports: List<Report>,
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
        reports.forEach { report ->
            // Since only TREES type exists, directly assign colors
            val strokeColor = Color.Green
            val fillColor = Color.Green.copy(alpha = 0.3f)

            Polygon(
                points = report.coordinates,
                strokeColor = strokeColor,
                fillColor = fillColor,
                strokeWidth = 4f
            )

            val center = report.coordinates.averageCenter()
            val treesData = report.data as ReportData.TreesData

            Marker(
                state = rememberMarkerState(position = center),
                title = "${report.type.name} Report",
                snippet = "Trees: ${treesData.quantity}",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )
        }

        if (selectedArea.size == 4) {
            Polygon(
                points = selectedArea,
                strokeColor = Color.Red,
                fillColor = Color.Red.copy(alpha = 0.3f),
                strokeWidth = 3f
            )

            val centerMarkerState = rememberUpdatedMarkerState(position = selectedArea.averageCenter())
            val cornerMarkerStates = selectedArea.mapIndexed { index, pos ->
                rememberUpdatedMarkerState(position = pos) to index
            }

            CenterDragMarker(markerState = centerMarkerState, onCenterDragged = onPolygonDrag)

            cornerMarkerStates.forEach { (state, index) ->
                DraggableCornerMarker(
                    markerState = state,
                    title = "Corner ${index + 1}",
                    onPositionChange = { newPos -> onCornerDrag(index, newPos) }
                )
            }
        }
    }
}

@Composable
fun rememberUpdatedMarkerState(position: LatLng): MarkerState {
    return remember { MarkerState(position = position) }.apply { this.position = position }
}

private fun List<LatLng>.averageCenter(): LatLng {
    val lat = map { it.latitude }.average()
    val lng = map { it.longitude }.average()
    return LatLng(lat, lng)
}

@Composable
fun CenterDragMarker(
    markerState: MarkerState,
    onCenterDragged: (LatLng) -> Unit
) {
    LaunchedEffect(markerState) {
        snapshotFlow { markerState.position }
            .distinctUntilChanged()
            .collect { newCenter -> onCenterDragged(newCenter) }
    }

    Marker(
        state = markerState,
        title = "Drag to Move Entire Area",
        draggable = true,
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
    )
}

@Composable
fun DraggableCornerMarker(
    markerState: MarkerState,
    title: String,
    onPositionChange: (LatLng) -> Unit
) {
    LaunchedEffect(markerState) {
        snapshotFlow { markerState.position }
            .distinctUntilChanged()
            .collect { newPosition -> onPositionChange(newPosition) }
    }

    Marker(
        state = markerState,
        title = title,
        draggable = true,
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    )
}


@Composable
fun ReportDialog2(
    treeEntries: List<TreeEntry>,
    onAddEntry: () -> Unit,
    onDeleteEntry: (Int) -> Unit,
    onTypeChange: (Int, TreeSubType) -> Unit,
    onQuantityChange: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean
) {
    val isSubmitEnabled = !isSubmitting && treeEntries.any { it.quantity > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Tree Report") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tree Types:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(qabottom = 8.dp)
                )

                treeEntries.forEachIndexed { index, entry ->
                    TreeEntryCard(
                        entry = entry,
                        entryNumber = index + 1,
                        onTypeChange = { newType -> onTypeChange(index, newType) },
                        onQuantityChange = { newQuantity -> onQuantityChange(index, newQuantity) },
                        onDelete = { onDeleteEntry(index) },
                        canDelete = treeEntries.size > 1
                    )

                    if (index < treeEntries.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAddEntry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Tree Type")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = isSubmitEnabled
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
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

@Composable
fun TreeEntryCard(
    entry: TreeEntry,
    entryNumber: Int,
    onTypeChange: (TreeSubType) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entry #$entryNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = entry.treeType == TreeSubType.DECIDUOUS,
                    onClick = { onTypeChange(TreeSubType.DECIDUOUS) },
                    label = { Text("Liściaste") }
                )
                FilterChip(
                    selected = entry.treeType == TreeSubType.CONIFEROUS,
                    onClick = { onTypeChange(TreeSubType.CONIFEROUS) },
                    label = { Text("Iglaste") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = entry.quantity.takeIf { it > 0 }?.toString() ?: "",
                onValueChange = { onQuantityChange(it.toIntOrNull() ?: 0) },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportDialog2Preview() {
    MaterialTheme {
        ReportDialog2(
            treeEntries = listOf(
                TreeEntry(id = 0, TreeSubType.DECIDUOUS, 10),
                TreeEntry(id = 1, TreeSubType.CONIFEROUS, 5)
            ),
            onAddEntry = {},
            onDeleteEntry = {},
            onTypeChange = { _, _ -> },
            onQuantityChange = { _, _ -> },
            onSubmit = {},
            onDismiss = {},
            isSubmitting = false
        )
    }
}


@Composable
fun ReportDialog(
    selectedType: ReportType,
    quantity: Int,
    onTypeSelected: (ReportType) -> Unit,
    onQuantityChanged: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean
) {
    val isTreesSelected = selectedType == ReportType.TREES
    val isSubmitEnabled = !isSubmitting && isTreesSelected && quantity > 0
    val quantityValue = quantity.takeIf { it > 0 }?.toString() ?: ""

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
                        selected = isTreesSelected,
                        onClick = { onTypeSelected(ReportType.TREES) },
                        label = { Text("Trees") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Row{
                        FilterChip(
                            selected = isTreesSelected,
                            onClick = { onTypeSelected(ReportType.TREES) },
                            label = { Text("Liszczacte") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = isTreesSelected,
                            onClick = { onTypeSelected(ReportType.TREES) },
                            label = { Text("Iglaste") }
                        )

                    }
                    OutlinedTextField(
                        value = quantityValue,
                        onValueChange = { onQuantityChanged(it.toIntOrNull() ?: 0) },
                        label = { Text("Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = isSubmitEnabled
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

@Preview(showBackground = true)
@Composable
fun ReportDialogPreview() {
    MaterialTheme {
        ReportDialog(
            selectedType = ReportType.TREES,
            quantity = 10,
            isSubmitting = false,
            onTypeSelected = {},
            onQuantityChanged = {},
            onSubmit = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Report Dialog - Submitting")
@Composable
fun ReportDialogSubmittingPreview() {
    MaterialTheme {
        ReportDialog(
            selectedType = ReportType.TREES,
            quantity = 25,
            isSubmitting = true,
            onTypeSelected = {},
            onQuantityChanged = {},
            onSubmit = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Report Dialog - Empty")
@Composable
fun ReportDialogEmptyPreview() {
    MaterialTheme {
        ReportDialog(
            selectedType = ReportType.TREES,
            quantity = 0,
            isSubmitting = false,
            onTypeSelected = {},
            onQuantityChanged = {},
            onSubmit = {},
            onDismiss = {}
        )
    }
}
