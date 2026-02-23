package com.example.trees_houses_counter.data.repository

import com.example.trees_houses_counter.data.local.dao.ReportDao
import com.example.trees_houses_counter.data.local.entity.ReportEntity
import com.example.trees_houses_counter.data.remote.dto.CoordinateDto
import com.example.trees_houses_counter.data.remote.dto.ReportDto
import com.example.trees_houses_counter.data.remote.dto.TreeEntryDto
import com.example.trees_houses_counter.domain.model.Report
import com.example.trees_houses_counter.domain.model.TreeEntry
import com.example.trees_houses_counter.domain.repository.ReportRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val reportDao: ReportDao,
    private val gson: Gson
) : ReportRepository {

    override suspend fun submitReport(report: Report): Result<Unit> {
        return try {
            val reportDto = report.toDto()
            firestore.collection("reports")
                .document(report.id)
                .set(reportDto)
                .await()

            val reportEntity = report.toEntity()
            reportDao.insertReport(reportEntity)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUserReports(userId: String): Flow<List<Report>> {
        return reportDao.getUserReports(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getReportById(reportId: String): Report? {
        return reportDao.getReportById(reportId)?.toDomain()
    }

    private fun Report.toDto(): ReportDto {
        return ReportDto(
            id = id,
            coordinates = coordinates.map { CoordinateDto(it.latitude, it.longitude) },
            userId = userId,
            timestamp = timestamp,
            totalQuantity = totalQuantity,
            entries = entries.map { TreeEntryDto(it.id, it.treeType.name, it.quantity) }
        )
    }

    private fun Report.toEntity(): ReportEntity {
        val coordPairs = coordinates.map { it.latitude to it.longitude }
        return ReportEntity(
            id = id,
            coordinates = gson.toJson(coordPairs),
            userId = userId,
            timestamp = timestamp,
            totalQuantity = totalQuantity,
            entries = gson.toJson(entries)
        )
    }

    private fun ReportEntity.toDomain(): Report {
        val coordPairs: List<Pair<Double, Double>> = gson.fromJson(
            coordinates,
            object : com.google.gson.reflect.TypeToken<List<Pair<Double, Double>>>() {}.type
        ) ?: emptyList()
        val latLngs = coordPairs.map { LatLng(it.first, it.second) }

        val entriesList: List<TreeEntry> = try {
            gson.fromJson(
                entries,
                object : com.google.gson.reflect.TypeToken<List<TreeEntry>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return Report(
            id = id,
            coordinates = latLngs,
            userId = userId,
            timestamp = timestamp,
            totalQuantity = totalQuantity,
            entries = entriesList
        )
    }
}
