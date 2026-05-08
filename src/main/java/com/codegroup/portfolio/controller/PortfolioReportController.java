package com.codegroup.portfolio.controller;

import com.codegroup.portfolio.dto.report.PortfolioReportDTO;
import com.codegroup.portfolio.service.PortfolioReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Relatórios", description = "Relatórios agregados do portfólio")
@RestController
@RequestMapping("/reports/portfolio")
@RequiredArgsConstructor
public class PortfolioReportController {

    private final PortfolioReportService reportService;

    @Operation(
            summary = "Resumo do portfólio",
            description = "Retorna quantidade e total orçado por status, duração média de projetos "
                    + "encerrados (em dias) e total de membros únicos alocados.")
    @GetMapping
    public ResponseEntity<PortfolioReportDTO> generate() {
        return ResponseEntity.ok(reportService.generate());
    }
}
