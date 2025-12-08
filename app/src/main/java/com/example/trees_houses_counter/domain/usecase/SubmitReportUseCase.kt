package com.example.trees_houses_counter.domain.usecase


import com.example.trees_houses_counter.domain.model.Report
import com.example.trees_houses_counter.domain.repository.ReportRepository
import javax.inject.Inject

class SubmitReportUseCase @Inject constructor(
    private val repository: ReportRepository
) {
    suspend operator fun invoke(report: Report): Result<Unit> {
        return repository.submitReport(report)
    }
}
