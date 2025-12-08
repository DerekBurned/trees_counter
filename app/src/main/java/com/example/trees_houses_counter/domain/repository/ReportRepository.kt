package com.example.trees_houses_counter.domain.repository

import com.example.trees_houses_counter.domain.model.Report
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    suspend fun submitReport(report: Report): Result<Unit>
    fun getUserReports(userId: String): Flow<List<Report>>
    suspend fun getReportById(reportId: String): Report?
}
