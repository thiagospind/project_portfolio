package com.codegroup.portfolio.dto.project;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectRequestDTO(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull(message = "Data de inicio é obrigatória!")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate initDate,

        @NotNull(message = "A data de previsão do termino no projeto é obrigatória")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endPreviewDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endRealDate,

        @NotNull(message = "O orçamento do projeto é obrigatório!")
        @DecimalMin(value = "0.00", inclusive = false)
        BigDecimal totalBudget,

        String description,

        @NotNull
        UUID managerId
) {
}
