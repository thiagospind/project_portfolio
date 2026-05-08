package com.codegroup.portfolio.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioReportDTO(
        List<StatusSummaryDTO> projectsByStatus,
        BigDecimal averageClosedDurationDays,
        long uniqueAllocatedMembers
) {
}
