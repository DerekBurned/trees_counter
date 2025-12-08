package com.example.trees_houses_counter.domain.usecase

import com.example.trees_houses_counter.domain.model.Report
import com.example.trees_houses_counter.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserReportsUseCase @Inject constructor(
    private val repository: ReportRepository
) {
    operator fun invoke(userId: String): Flow<List<Report>> {
        return repository.getUserReports(userId)
    }
}
