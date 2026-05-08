package com.codegroup.portfolio.dto.project;

import com.codegroup.portfolio.domain.enums.ProjectStatus;
import com.codegroup.portfolio.domain.enums.RiskLevel;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponseDTO(
        UUID id,
        String name,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate initDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endPreviewDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endRealDate,

        BigDecimal totalBudget,
        String description,
        ProjectStatus actualStatus,
        RiskLevel riskLevel,
        MemberResponseDTO manager,

        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        LocalDateTime updatedAt
) {
}
