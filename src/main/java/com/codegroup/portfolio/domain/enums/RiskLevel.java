package com.codegroup.portfolio.domain.enums;

import com.codegroup.portfolio.exception.Response422Exception;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public enum RiskLevel {
    RISCO_BAIXO("risk.level.low"),
    RISCO_MEDIO("risk.level.medium"),
    RISCO_ALTO("risk.level.high");

    private final String descriptionKey;

    private static final BigDecimal LOW_BUDGET_CEIL = new BigDecimal("100000");
    private static final BigDecimal HIGH_BUDGET_CEIL = new BigDecimal("500000");
    private static final long LOW_DURATION_CEIL_MONTHS = 3L;
    private static final long HIGH_DURATION_CEIL_MONTHS = 6L;

    public static RiskLevel from(BigDecimal totalBudget, long durationMonths) {
        if (totalBudget == null) {
            throw new Response422Exception("error.risk.budget.null");
        }
        if (durationMonths < 0) {
            throw new Response422Exception("error.risk.duration.negative", String.valueOf(durationMonths));
        }
        if (totalBudget.compareTo(HIGH_BUDGET_CEIL) > 0 || durationMonths > HIGH_DURATION_CEIL_MONTHS) {
            return RISCO_ALTO;
        }
        if (totalBudget.compareTo(LOW_BUDGET_CEIL) > 0 || durationMonths > LOW_DURATION_CEIL_MONTHS) {
            return RISCO_MEDIO;
        }
        return RISCO_BAIXO;
    }
}
