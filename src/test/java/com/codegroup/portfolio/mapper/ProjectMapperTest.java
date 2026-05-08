package com.codegroup.portfolio.mapper;

import com.codegroup.portfolio.client.MemberView;
import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.domain.enums.MemberAssignment;
import com.codegroup.portfolio.domain.enums.ProjectStatus;
import com.codegroup.portfolio.domain.enums.RiskLevel;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.dto.project.ProjectRequestDTO;
import com.codegroup.portfolio.dto.project.ProjectResponseDTO;
import com.codegroup.portfolio.service.RiskClassifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig({ProjectMapperImpl.class, RiskClassifier.class})
class ProjectMapperTest {

    @Autowired
    private ProjectMapper mapper;

    private static final UUID MANAGER_ID = UUID.randomUUID();

    private ProjectRequestDTO sampleRequest() {
        return new ProjectRequestDTO(
                "Migração ERP",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 9, 1),
                null,
                new BigDecimal("80000.00"),
                "Migração para o novo ERP",
                MANAGER_ID);
    }

    private Project sampleEntity() {
        Project p = new Project();
        p.setId(UUID.randomUUID());
        p.setName("Migração ERP");
        p.setInitDate(LocalDate.of(2026, 6, 1));
        p.setEndPreviewDate(LocalDate.of(2026, 9, 1));
        p.setEndRealDate(null);
        p.setTotalBudget(new BigDecimal("80000.00"));
        p.setDescription("Migração para o novo ERP");
        p.setActualStatus(ProjectStatus.EM_ANALISE);
        p.setManagerId(MANAGER_ID);
        return p;
    }

    @Nested
    @DisplayName("mapToEntityInsert")
    class MapToEntityInsert {

        @Test
        @DisplayName("retorna null quando request é null")
        void returnsNullForNullRequest() {
            assertThat(mapper.mapToEntityInsert(null)).isNull();
        }

        @Test
        @DisplayName("copia todos os campos escalares do request")
        void mapsScalarFields() {
            ProjectRequestDTO req = sampleRequest();

            Project entity = mapper.mapToEntityInsert(req);

            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("Migração ERP");
            assertThat(entity.getInitDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(entity.getEndPreviewDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(entity.getEndRealDate()).isNull();
            assertThat(entity.getTotalBudget()).isEqualByComparingTo("80000.00");
            assertThat(entity.getDescription()).isEqualTo("Migração para o novo ERP");
            assertThat(entity.getManagerId()).isEqualTo(MANAGER_ID);
        }

        @Test
        @DisplayName("ignora id, updatedAt e actualStatus (definidos pelo serviço)")
        void ignoresServiceManagedFields() {
            Project entity = mapper.mapToEntityInsert(sampleRequest());

            assertThat(entity.getId()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getActualStatus()).isNull();
        }

        @Test
        @DisplayName("preenche createdAt com LocalDateTime.now()")
        void setsCreatedAtToNow() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            Project entity = mapper.mapToEntityInsert(sampleRequest());
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(entity.getCreatedAt())
                    .isNotNull()
                    .isAfter(before)
                    .isBefore(after);
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("atualiza campos do request preservando id, createdAt e actualStatus")
        void updatesMutableFieldsOnly() {
            Project existing = sampleEntity();
            UUID originalId = existing.getId();
            LocalDateTime originalCreatedAt = LocalDateTime.of(2025, 1, 1, 10, 0);
            existing.setCreatedAt(originalCreatedAt);
            existing.setActualStatus(ProjectStatus.EM_ANDAMENTO);

            UUID newManagerId = UUID.randomUUID();
            ProjectRequestDTO update = new ProjectRequestDTO(
                    "Novo Nome",
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 12, 1),
                    LocalDate.of(2026, 11, 30),
                    new BigDecimal("250000.00"),
                    "Nova descrição",
                    newManagerId);

            mapper.updateEntity(existing, update);

            assertThat(existing.getId()).isEqualTo(originalId);
            assertThat(existing.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(existing.getActualStatus()).isEqualTo(ProjectStatus.EM_ANDAMENTO);

            assertThat(existing.getName()).isEqualTo("Novo Nome");
            assertThat(existing.getInitDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(existing.getEndPreviewDate()).isEqualTo(LocalDate.of(2026, 12, 1));
            assertThat(existing.getEndRealDate()).isEqualTo(LocalDate.of(2026, 11, 30));
            assertThat(existing.getTotalBudget()).isEqualByComparingTo("250000.00");
            assertThat(existing.getDescription()).isEqualTo("Nova descrição");
            assertThat(existing.getManagerId()).isEqualTo(newManagerId);
        }

        @Test
        @DisplayName("define updatedAt com LocalDateTime.now()")
        void setsUpdatedAtToNow() {
            Project existing = sampleEntity();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            mapper.updateEntity(existing, sampleRequest());
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(existing.getUpdatedAt())
                    .isNotNull()
                    .isAfter(before)
                    .isBefore(after);
        }
    }

    @Nested
    @DisplayName("mapToResponseDTO")
    class MapToResponseDTO {

        @Test
        @DisplayName("retorna null quando entity e memberView são null")
        void returnsNullWhenAllSourcesAreNull() {
            assertThat(mapper.mapToResponseDTO(null, null)).isNull();
        }

        @Test
        @DisplayName("copia campos escalares do entity")
        void mapsEntityScalarFields() {
            Project entity = sampleEntity();
            MemberView managerView = new MemberView(MANAGER_ID, "Carla", MemberAssignment.GERENTE);

            ProjectResponseDTO dto = mapper.mapToResponseDTO(entity, managerView);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(entity.getId());
            assertThat(dto.name()).isEqualTo(entity.getName());
            assertThat(dto.initDate()).isEqualTo(entity.getInitDate());
            assertThat(dto.endPreviewDate()).isEqualTo(entity.getEndPreviewDate());
            assertThat(dto.endRealDate()).isEqualTo(entity.getEndRealDate());
            assertThat(dto.totalBudget()).isEqualByComparingTo(entity.getTotalBudget());
            assertThat(dto.description()).isEqualTo(entity.getDescription());
            assertThat(dto.actualStatus()).isEqualTo(entity.getActualStatus());
        }

        @Test
        @DisplayName("popula manager a partir do MemberView")
        void mapsManagerFromMemberView() {
            Project entity = sampleEntity();
            MemberView managerView = new MemberView(MANAGER_ID, "Carla Souza", MemberAssignment.GERENTE);

            ProjectResponseDTO dto = mapper.mapToResponseDTO(entity, managerView);

            assertThat(dto.manager()).isNotNull();
            assertThat(dto.manager().id()).isEqualTo(MANAGER_ID);
            assertThat(dto.manager().name()).isEqualTo("Carla Souza");
            assertThat(dto.manager().assignment()).isEqualTo(MemberAssignment.GERENTE);
        }

        @Test
        @DisplayName("calcula riskLevel via RiskClassifier — RISCO_BAIXO (até 100k e ≤3 meses)")
        void computesLowRisk() {
            Project entity = sampleEntity();
            entity.setTotalBudget(new BigDecimal("80000.00"));
            entity.setInitDate(LocalDate.of(2026, 1, 1));
            entity.setEndPreviewDate(LocalDate.of(2026, 4, 1));

            ProjectResponseDTO dto = mapper.mapToResponseDTO(entity, null);

            assertThat(dto.riskLevel()).isEqualTo(RiskLevel.RISCO_BAIXO);
        }

        @Test
        @DisplayName("calcula riskLevel via RiskClassifier — RISCO_MEDIO (>100k ou >3 meses)")
        void computesMediumRisk() {
            Project entity = sampleEntity();
            entity.setTotalBudget(new BigDecimal("150000.00"));
            entity.setInitDate(LocalDate.of(2026, 1, 1));
            entity.setEndPreviewDate(LocalDate.of(2026, 4, 1));

            ProjectResponseDTO dto = mapper.mapToResponseDTO(entity, null);

            assertThat(dto.riskLevel()).isEqualTo(RiskLevel.RISCO_MEDIO);
        }

        @Test
        @DisplayName("calcula riskLevel via RiskClassifier — RISCO_ALTO (>500k ou >6 meses)")
        void computesHighRisk() {
            Project entity = sampleEntity();
            entity.setTotalBudget(new BigDecimal("600000.00"));
            entity.setInitDate(LocalDate.of(2026, 1, 1));
            entity.setEndPreviewDate(LocalDate.of(2026, 4, 1));

            ProjectResponseDTO dto = mapper.mapToResponseDTO(entity, null);

            assertThat(dto.riskLevel()).isEqualTo(RiskLevel.RISCO_ALTO);
        }

        @Test
        @DisplayName("manager é null quando memberView é null")
        void managerIsNullWhenMemberViewIsNull() {
            ProjectResponseDTO dto = mapper.mapToResponseDTO(sampleEntity(), null);

            assertThat(dto.manager()).isNull();
        }
    }

    @Nested
    @DisplayName("mapMemberViewToResponse")
    class MapMemberViewToResponse {

        @Test
        @DisplayName("retorna null quando view é null")
        void returnsNullForNullView() {
            assertThat(mapper.mapMemberViewToResponse(null)).isNull();
        }

        @Test
        @DisplayName("copia id, name e assignment do MemberView")
        void mapsAllFields() {
            UUID id = UUID.randomUUID();
            MemberView view = new MemberView(id, "João", MemberAssignment.FUNCIONARIO);

            MemberResponseDTO dto = mapper.mapMemberViewToResponse(view);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.name()).isEqualTo("João");
            assertThat(dto.assignment()).isEqualTo(MemberAssignment.FUNCIONARIO);
        }
    }
}
