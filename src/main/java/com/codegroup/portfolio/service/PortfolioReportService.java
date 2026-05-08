package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.report.PortfolioReportDTO;
import com.codegroup.portfolio.dto.report.StatusSummaryDTO;
import com.codegroup.portfolio.repository.ProjectAllocationRepository;
import com.codegroup.portfolio.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioReportService {

    private final ProjectRepository projectRepository;
    private final ProjectAllocationRepository allocationRepository;

    public PortfolioReportDTO generate() {
        List<StatusSummaryDTO> projectsByStatus = projectRepository.aggregateByStatus();
        Double avgClosedDays = projectRepository.averageClosedDurationDays();
        long uniqueAllocatedMembers = allocationRepository.countDistinctMembers();

        return new PortfolioReportDTO(
                projectsByStatus,
                avgClosedDays != null ? BigDecimal.valueOf(avgClosedDays) : BigDecimal.ZERO,
                uniqueAllocatedMembers
        );
    }
}
