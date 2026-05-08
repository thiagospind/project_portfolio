package com.codegroup.portfolio.dto.project;

import com.codegroup.portfolio.domain.enums.ProjectStatus;
import com.codegroup.portfolio.domain.enums.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectFilterDTO(
        String name,
        ProjectStatus actualStatus,
        RiskLevel riskLevel,
        UUID managerId,
        LocalDate initDateFrom,
        LocalDate initDateTo,
        BigDecimal totalBudgetFrom,
        BigDecimal totalBudgetTo
) {
}
