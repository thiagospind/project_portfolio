package com.codegroup.portfolio.service;

import com.codegroup.portfolio.client.MemberClient;
import com.codegroup.portfolio.client.MemberView;
import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.domain.enums.MemberAssignment;
import com.codegroup.portfolio.domain.enums.ProjectStatus;
import com.codegroup.portfolio.domain.enums.RiskLevel;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.dto.project.ProjectFilterDTO;
import com.codegroup.portfolio.dto.project.ProjectRequestDTO;
import com.codegroup.portfolio.dto.project.ProjectResponseDTO;
import com.codegroup.portfolio.dto.project.ProjectStatusUpdateDTO;
import com.codegroup.portfolio.exception.Response404Exception;
import com.codegroup.portfolio.exception.Response422Exception;
import com.codegroup.portfolio.exception.Response500Exception;
import com.codegroup.portfolio.mapper.ProjectMapper;
import com.codegroup.portfolio.repository.ProjectAllocationRepository;
import com.codegroup.portfolio.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMapper mapper;

    @Mock
    private ProjectRepository repository;

    @Mock
    private ProjectAllocationRepository allocationRepository;

    @Mock
    private MemberClient memberClient;

    @Mock
    private ProjectStatusTransition statusTransition;

    @InjectMocks
    private ProjectService service;

    private UUID projectId;
    private UUID managerId;
    private LocalDate today;
    private MemberView gerenteView;
    private Project project;
    private ProjectResponseDTO sentinelResponse;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        today = LocalDate.of(2026, 5, 7);
        gerenteView = new MemberView(managerId, "Gerente", MemberAssignment.GERENTE);
        project = newProject(projectId, managerId);
        sentinelResponse = newResponse(projectId, managerId);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("retorna 422 quando endPreviewDate é anterior à initDate")
        void throwsWhenEndPreviewBeforeInit() {
            ProjectRequestDTO req = newRequest(today, today.minusDays(1), null);

            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.dates.endPreview.before.init");

            verifyNoInteractions(memberClient, repository, mapper);
        }

        @Test
        @DisplayName("retorna 422 quando endRealDate é anterior à initDate")
        void throwsWhenEndRealBeforeInit() {
            ProjectRequestDTO req = newRequest(today, today.plusMonths(1), today.minusDays(1));

            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.dates.endReal.before.init");

            verifyNoInteractions(memberClient, repository, mapper);
        }

        @Test
        @DisplayName("retorna 404 quando manager não encontrado")
        void throwsWhenManagerNotFound() {
            ProjectRequestDTO req = newRequest(today, today.plusMonths(1), null);
            when(memberClient.findById(managerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(Response404Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.member.not.found");

            verifyNoInteractions(repository, mapper);
        }

        @ParameterizedTest(name = "retorna 422 quando manager tem assignment {0} (não é GERENTE)")
        @EnumSource(value = MemberAssignment.class, mode = EnumSource.Mode.EXCLUDE, names = "GERENTE")
        void throwsWhenManagerNotGerente(MemberAssignment assignment) {
            ProjectRequestDTO req = newRequest(today, today.plusMonths(1), null);
            when(memberClient.findById(managerId))
                    .thenReturn(Optional.of(new MemberView(managerId, "Membro", assignment)));

            assertThatThrownBy(() -> service.create(req))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.manager.invalid.assignment");

            verifyNoInteractions(repository, mapper);
        }

        @Test
        @DisplayName("happy path: gera UUID, força EM_ANALISE e persiste")
        void createsProject() {
            ProjectRequestDTO req = newRequest(today, today.plusMonths(1), null);
            when(memberClient.findById(managerId)).thenReturn(Optional.of(gerenteView));
            Project mapped = new Project();
            when(mapper.mapToEntityInsert(req)).thenReturn(mapped);
            when(repository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.mapToResponseDTO(any(Project.class), eq(gerenteView))).thenReturn(sentinelResponse);

            ProjectResponseDTO result = service.create(req);

            assertThat(result).isSameAs(sentinelResponse);
            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(repository).save(captor.capture());
            Project saved = captor.getValue();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getActualStatus()).isEqualTo(ProjectStatus.EM_ANALISE);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("retorna 404 quando projeto não encontrado")
        void throwsWhenProjectNotFound() {
            when(repository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(projectId))
                    .isInstanceOf(Response404Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.not.found");
        }

        @Test
        @DisplayName("retorna 500 quando manager sumiu da API externa")
        void throwsWhenManagerGone() {
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(memberClient.findById(managerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(projectId))
                    .isInstanceOf(Response500Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.manager.gone");
        }

        @Test
        @DisplayName("happy path: retorna DTO com dados do manager")
        void returnsDto() {
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(memberClient.findById(managerId)).thenReturn(Optional.of(gerenteView));
            when(mapper.mapToResponseDTO(project, gerenteView)).thenReturn(sentinelResponse);

            ProjectResponseDTO result = service.findById(projectId);

            assertThat(result).isSameAs(sentinelResponse);
        }
    }

    @Nested
    @DisplayName("findByIdEntity")
    class FindByIdEntity {

        @Test
        @DisplayName("retorna 404 quando não encontrado")
        void throwsWhenNotFound() {
            when(repository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByIdEntity(projectId))
                    .isInstanceOf(Response404Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.not.found");
        }

        @Test
        @DisplayName("happy path: retorna entidade")
        void returnsEntity() {
            when(repository.findById(projectId)).thenReturn(Optional.of(project));

            Project result = service.findByIdEntity(projectId);

            assertThat(result).isSameAs(project);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        private final Pageable pageable = PageRequest.of(0, 10);
        private final ProjectFilterDTO filter = new ProjectFilterDTO(
                null, null, null, null, null, null, null, null);

        @Test
        @DisplayName("retorna page vazia sem chamar MemberClient")
        @SuppressWarnings("unchecked")
        void returnsEmpty() {
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty(pageable));

            Page<ProjectResponseDTO> result = service.findAll(filter, pageable);

            assertThat(result.getContent()).isEmpty();
            verifyNoInteractions(memberClient);
        }

        @Test
        @DisplayName("happy path: faz batch fetch e mapeia cada projeto")
        @SuppressWarnings("unchecked")
        void returnsMappedPage() {
            Page<Project> projectPage = new PageImpl<>(List.of(project), pageable, 1);
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(projectPage);
            when(memberClient.findAllByIds(any())).thenReturn(List.of(gerenteView));
            when(mapper.mapToResponseDTO(project, gerenteView)).thenReturn(sentinelResponse);

            Page<ProjectResponseDTO> result = service.findAll(filter, pageable);

            assertThat(result.getContent()).containsExactly(sentinelResponse);
        }

        @Test
        @DisplayName("retorna 500 quando manager de algum projeto sumiu")
        @SuppressWarnings("unchecked")
        void throwsWhenManagerGone() {
            Page<Project> projectPage = new PageImpl<>(List.of(project), pageable, 1);
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(projectPage);
            when(memberClient.findAllByIds(any())).thenReturn(List.of());

            assertThatThrownBy(() -> service.findAll(filter, pageable))
                    .isInstanceOf(Response500Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.manager.gone");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("retorna 404 quando projeto não encontrado")
        void throwsWhenProjectNotFound() {
            when(repository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(projectId, newRequest(today, today.plusMonths(1), null)))
                    .isInstanceOf(Response404Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.not.found");
        }

        @Test
        @DisplayName("retorna 404 quando novo manager não encontrado")
        void throwsWhenNewManagerNotFound() {
            ProjectRequestDTO req = newRequest(today, today.plusMonths(1), null);
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(memberClient.findById(managerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(projectId, req))
                    .isInstanceOf(Response404Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.member.not.found");
        }

        @Test
        @DisplayName("retorna 422 quando novo manager não é GERENTE")
        void throwsWhenManagerNotGerente() {
            ProjectRequestDTO req = newRequest(today, today.plusMonths(1), null);
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(memberClient.findById(managerId))
                    .thenReturn(Optional.of(new MemberView(managerId, "Funcionario", MemberAssignment.FUNCIONARIO)));

            assertThatThrownBy(() -> service.update(projectId, req))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.manager.invalid.assignment");
        }

        @Test
        @DisplayName("happy path: aplica updateEntity, persiste e retorna DTO")
        void updatesProject() {
            ProjectRequestDTO req = newRequest(today, today.plusMonths(1), null);
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(memberClient.findById(managerId)).thenReturn(Optional.of(gerenteView));
            when(repository.save(project)).thenReturn(project);
            when(mapper.mapToResponseDTO(project, gerenteView)).thenReturn(sentinelResponse);

            ProjectResponseDTO result = service.update(projectId, req);

            verify(mapper).updateEntity(project, req);
            verify(repository).save(project);
            assertThat(result).isSameAs(sentinelResponse);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("retorna 404 quando projeto não encontrado")
        void throwsWhenProjectNotFound() {
            when(repository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatus(projectId,
                    new ProjectStatusUpdateDTO(ProjectStatus.ANALISE_REALIZADA)))
                    .isInstanceOf(Response404Exception.class);
        }

        @Test
        @DisplayName("propaga 422 quando ProjectStatusTransition rejeita transição")
        void throwsWhenInvalidTransition() {
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            doThrow(new Response422Exception("error.project.status.invalid.transition", "X", "Y"))
                    .when(statusTransition).ensureValidTransition(any(), any());

            assertThatThrownBy(() -> service.updateStatus(projectId,
                    new ProjectStatusUpdateDTO(ProjectStatus.ENCERRADO)))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.invalid.transition");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("happy path: transição não-ENCERRADO mantém endRealDate nulo")
        void doesNotSetEndRealForNonClosed() {
            project.setActualStatus(ProjectStatus.EM_ANALISE);
            project.setEndRealDate(null);
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(repository.save(project)).thenReturn(project);
            when(memberClient.findById(managerId)).thenReturn(Optional.of(gerenteView));
            when(mapper.mapToResponseDTO(project, gerenteView)).thenReturn(sentinelResponse);

            service.updateStatus(projectId, new ProjectStatusUpdateDTO(ProjectStatus.ANALISE_REALIZADA));

            assertThat(project.getActualStatus()).isEqualTo(ProjectStatus.ANALISE_REALIZADA);
            assertThat(project.getEndRealDate()).isNull();
        }

        @Test
        @DisplayName("retorna 422 ao iniciar projeto sem nenhuma alocação")
        void throwsWhenStartingWithoutAllocations() {
            project.setActualStatus(ProjectStatus.ANALISE_APROVADA);
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(allocationRepository.countByProjectId(projectId)).thenReturn(0L);

            assertThatThrownBy(() -> service.updateStatus(projectId,
                    new ProjectStatusUpdateDTO(ProjectStatus.INICIADO)))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.start.no.allocations");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("happy path: inicia projeto quando há ao menos 1 alocação")
        void startsProjectWithAtLeastOneAllocation() {
            project.setActualStatus(ProjectStatus.ANALISE_APROVADA);
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(allocationRepository.countByProjectId(projectId)).thenReturn(1L);
            when(repository.save(project)).thenReturn(project);
            when(memberClient.findById(managerId)).thenReturn(Optional.of(gerenteView));
            when(mapper.mapToResponseDTO(project, gerenteView)).thenReturn(sentinelResponse);

            service.updateStatus(projectId, new ProjectStatusUpdateDTO(ProjectStatus.INICIADO));

            assertThat(project.getActualStatus()).isEqualTo(ProjectStatus.INICIADO);
        }

        @Test
        @DisplayName("happy path: transição para ENCERRADO seta endRealDate como hoje")
        void setsEndRealForClosed() {
            project.setActualStatus(ProjectStatus.EM_ANDAMENTO);
            project.setEndRealDate(null);
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            when(repository.save(project)).thenReturn(project);
            when(memberClient.findById(managerId)).thenReturn(Optional.of(gerenteView));
            when(mapper.mapToResponseDTO(project, gerenteView)).thenReturn(sentinelResponse);

            service.updateStatus(projectId, new ProjectStatusUpdateDTO(ProjectStatus.ENCERRADO));

            assertThat(project.getActualStatus()).isEqualTo(ProjectStatus.ENCERRADO);
            assertThat(project.getEndRealDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("retorna 404 quando projeto não encontrado")
        void throwsWhenNotFound() {
            when(repository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(projectId))
                    .isInstanceOf(Response404Exception.class);
        }

        @Test
        @DisplayName("propaga 422 quando ProjectStatusTransition rejeita deleção")
        void throwsWhenBlockingStatus() {
            when(repository.findById(projectId)).thenReturn(Optional.of(project));
            doThrow(new Response422Exception("error.project.status.blocks.deletion", "Iniciado"))
                    .when(statusTransition).ensureNotBlockingDeletion(any());

            assertThatThrownBy(() -> service.delete(projectId))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.blocks.deletion");

            verify(repository, never()).delete(any(Project.class));
        }

        @Test
        @DisplayName("happy path: deleta projeto")
        void deletes() {
            when(repository.findById(projectId)).thenReturn(Optional.of(project));

            service.delete(projectId);

            verify(repository).delete(project);
        }
    }

    private Project newProject(UUID id, UUID mgrId) {
        Project p = new Project();
        p.setId(id);
        p.setName("Projeto X");
        p.setInitDate(today);
        p.setEndPreviewDate(today.plusMonths(2));
        p.setTotalBudget(new BigDecimal("50000"));
        p.setActualStatus(ProjectStatus.EM_ANALISE);
        p.setManagerId(mgrId);
        return p;
    }

    private ProjectResponseDTO newResponse(UUID id, UUID mgrId) {
        return new ProjectResponseDTO(
                id, "Projeto X", today, today.plusMonths(2), null,
                new BigDecimal("50000"), null, ProjectStatus.EM_ANALISE,
                RiskLevel.RISCO_BAIXO, new MemberResponseDTO(mgrId, "Gerente", MemberAssignment.GERENTE),
                LocalDateTime.now(), null);
    }

    private ProjectRequestDTO newRequest(LocalDate init, LocalDate endPreview, LocalDate endReal) {
        return new ProjectRequestDTO(
                "Projeto X", init, endPreview, endReal,
                new BigDecimal("50000"), "desc", managerId);
    }
}
