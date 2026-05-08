package com.codegroup.portfolio.dto.report;

import com.codegroup.portfolio.domain.enums.ProjectStatus;

import java.math.BigDecimal;

public record StatusSummaryDTO(
        ProjectStatus status,
        long count,
        BigDecimal totalBudget
) {
}
