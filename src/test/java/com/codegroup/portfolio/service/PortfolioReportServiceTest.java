package com.codegroup.portfolio.service;

import com.codegroup.portfolio.domain.enums.ProjectStatus;
import com.codegroup.portfolio.dto.report.PortfolioReportDTO;
import com.codegroup.portfolio.dto.report.StatusSummaryDTO;
import com.codegroup.portfolio.repository.ProjectAllocationRepository;
import com.codegroup.portfolio.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioReportServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectAllocationRepository allocationRepository;

    @InjectMocks
    private PortfolioReportService service;

    @Test
    @DisplayName("agrega status, duração média e total de membros únicos")
    void aggregatesAllMetrics() {
        List<StatusSummaryDTO> byStatus = List.of(
                new StatusSummaryDTO(ProjectStatus.EM_ANALISE, 3L, new BigDecimal("150000")),
                new StatusSummaryDTO(ProjectStatus.EM_ANDAMENTO, 2L, new BigDecimal("700000")),
                new StatusSummaryDTO(ProjectStatus.ENCERRADO, 1L, new BigDecimal("50000"))
        );
        when(projectRepository.aggregateByStatus()).thenReturn(byStatus);
        when(projectRepository.averageClosedDurationDays()).thenReturn(45.5);
        when(allocationRepository.countDistinctMembers()).thenReturn(7L);

        PortfolioReportDTO report = service.generate();

        assertThat(report.projectsByStatus()).containsExactlyElementsOf(byStatus);
        assertThat(report.averageClosedDurationDays()).isEqualByComparingTo("45.5");
        assertThat(report.uniqueAllocatedMembers()).isEqualTo(7L);
    }

    @Test
    @DisplayName("retorna duração ZERO quando não há projetos encerrados")
    void returnsZeroDurationWhenNoneClosed() {
        when(projectRepository.aggregateByStatus()).thenReturn(List.of());
        when(projectRepository.averageClosedDurationDays()).thenReturn(null);
        when(allocationRepository.countDistinctMembers()).thenReturn(0L);

        PortfolioReportDTO report = service.generate();

        assertThat(report.averageClosedDurationDays()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("não inclui status zerados (apenas o que o repository retornou)")
    void onlyIncludesStatusesWithProjects() {
        List<StatusSummaryDTO> byStatus = List.of(
                new StatusSummaryDTO(ProjectStatus.EM_ANALISE, 1L, new BigDecimal("10000"))
        );
        when(projectRepository.aggregateByStatus()).thenReturn(byStatus);
        when(projectRepository.averageClosedDurationDays()).thenReturn(null);
        when(allocationRepository.countDistinctMembers()).thenReturn(0L);

        PortfolioReportDTO report = service.generate();

        assertThat(report.projectsByStatus())
                .hasSize(1)
                .extracting(StatusSummaryDTO::status)
                .containsExactly(ProjectStatus.EM_ANALISE);
    }
}
