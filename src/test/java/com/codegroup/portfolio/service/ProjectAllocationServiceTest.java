package com.codegroup.portfolio.service;

import com.codegroup.portfolio.client.MemberClient;
import com.codegroup.portfolio.client.MemberView;
import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.domain.entity.ProjectAllocation;
import com.codegroup.portfolio.domain.enums.MemberAssignment;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.exception.Response404Exception;
import com.codegroup.portfolio.exception.Response422Exception;
import com.codegroup.portfolio.repository.ProjectAllocationRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectAllocationServiceTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private MemberClient memberClient;

    @Mock
    private ProjectAllocationRepository repository;

    @InjectMocks
    private ProjectAllocationService service;

    private UUID projectId;
    private UUID memberId;
    private Project project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        project = new Project();
        project.setId(projectId);
    }

    @Nested
    @DisplayName("allocate")
    class Allocate {

        @Test
        @DisplayName("propaga 404 quando projeto não existe")
        void propagatesProjectNotFound() {
            when(projectService.findByIdEntity(projectId))
                    .thenThrow(new Response404Exception("error.project.not.found", projectId.toString()));

            assertThatThrownBy(() -> service.allocate(projectId, memberId))
                    .isInstanceOf(Response404Exception.class);

            verifyNoInteractions(memberClient);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("retorna 404 quando membro não existe")
        void throwsWhenMemberNotFound() {
            when(projectService.findByIdEntity(projectId)).thenReturn(project);
            when(memberClient.findById(memberId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.allocate(projectId, memberId))
                    .isInstanceOf(Response404Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.member.not.found");

            verify(repository, never()).save(any());
        }

        @ParameterizedTest(name = "retorna 422 quando membro tem assignment {0} (não é FUNCIONARIO)")
        @EnumSource(value = MemberAssignment.class, mode = EnumSource.Mode.EXCLUDE, names = "FUNCIONARIO")
        void throwsWhenMemberNotFuncionario(MemberAssignment assignment) {
            when(projectService.findByIdEntity(projectId)).thenReturn(project);
            when(memberClient.findById(memberId))
                    .thenReturn(Optional.of(new MemberView(memberId, "Membro", assignment)));

            assertThatThrownBy(() -> service.allocate(projectId, memberId))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.allocation.member.invalid.assignment");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("retorna 422 quando alocação já existe (duplicata)")
        void throwsWhenAlreadyAllocated() {
            mockValidMember();
            when(repository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(true);

            assertThatThrownBy(() -> service.allocate(projectId, memberId))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.allocation.duplicate");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("aceita alocar 10º membro (boundary inferior)")
        void allocatesTenthMember() {
            mockValidMember();
            when(repository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(false);
            when(repository.countByProjectId(projectId)).thenReturn(9L);
            when(repository.countActiveByMemberId(memberId)).thenReturn(0L);

            service.allocate(projectId, memberId);

            verify(repository).save(any(ProjectAllocation.class));
        }

        @Test
        @DisplayName("retorna 422 quando projeto já tem 10 membros")
        void throwsWhenProjectAtMaxCapacity() {
            mockValidMember();
            when(repository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(false);
            when(repository.countByProjectId(projectId)).thenReturn(10L);

            assertThatThrownBy(() -> service.allocate(projectId, memberId))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.allocation.project.max.reached");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("aceita alocar quando membro tem 2 projetos ativos (boundary inferior)")
        void allocatesWhenMemberHasTwoActiveProjects() {
            mockValidMember();
            when(repository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(false);
            when(repository.countByProjectId(projectId)).thenReturn(0L);
            when(repository.countActiveByMemberId(memberId)).thenReturn(2L);

            service.allocate(projectId, memberId);

            verify(repository).save(any(ProjectAllocation.class));
        }

        @Test
        @DisplayName("retorna 422 quando membro já tem 3 projetos ativos")
        void throwsWhenMemberAtMaxActive() {
            mockValidMember();
            when(repository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(false);
            when(repository.countByProjectId(projectId)).thenReturn(0L);
            when(repository.countActiveByMemberId(memberId)).thenReturn(3L);

            assertThatThrownBy(() -> service.allocate(projectId, memberId))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.allocation.member.max.active.reached");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("salva ProjectAllocation completa quando todas validações passam")
        void savesAllocationOnHappyPath() {
            mockValidMember();
            when(repository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(false);
            when(repository.countByProjectId(projectId)).thenReturn(0L);
            when(repository.countActiveByMemberId(memberId)).thenReturn(0L);

            service.allocate(projectId, memberId);

            ArgumentCaptor<ProjectAllocation> captor = ArgumentCaptor.forClass(ProjectAllocation.class);
            verify(repository).save(captor.capture());
            ProjectAllocation saved = captor.getValue();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getProject()).isSameAs(project);
            assertThat(saved.getMemberId()).isEqualTo(memberId);
        }

        private void mockValidMember() {
            when(projectService.findByIdEntity(projectId)).thenReturn(project);
            when(memberClient.findById(memberId))
                    .thenReturn(Optional.of(new MemberView(memberId, "João", MemberAssignment.FUNCIONARIO)));
        }
    }

    @Nested
    @DisplayName("deallocate")
    class Deallocate {

        @Test
        @DisplayName("retorna 404 quando alocação não encontrada")
        void throwsWhenAllocationNotFound() {
            when(repository.findByProjectIdAndMemberId(projectId, memberId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deallocate(projectId, memberId))
                    .isInstanceOf(Response404Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.allocation.not.found");

            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("remove a alocação quando encontrada")
        void deletesWhenFound() {
            ProjectAllocation allocation = newAllocation(memberId);
            when(repository.findByProjectIdAndMemberId(projectId, memberId))
                    .thenReturn(Optional.of(allocation));

            service.deallocate(projectId, memberId);

            verify(repository).delete(allocation);
        }
    }

    @Nested
    @DisplayName("listAllocations")
    class ListAllocations {

        @Test
        @DisplayName("propaga 404 quando projeto não existe")
        void throwsWhenProjectNotFound() {
            when(projectService.findByIdEntity(projectId))
                    .thenThrow(new Response404Exception("error.project.not.found", projectId.toString()));

            assertThatThrownBy(() -> service.listAllocations(projectId))
                    .isInstanceOf(Response404Exception.class);

            verifyNoInteractions(memberClient);
        }

        @Test
        @DisplayName("retorna lista vazia sem chamar MemberClient quando não há alocações")
        void returnsEmptyWithoutCallingMemberClient() {
            when(projectService.findByIdEntity(projectId)).thenReturn(project);
            when(repository.findAllByProjectId(projectId)).thenReturn(List.of());

            List<MemberResponseDTO> result = service.listAllocations(projectId);

            assertThat(result).isEmpty();
            verifyNoInteractions(memberClient);
        }

        @Test
        @DisplayName("retorna DTOs dos membros alocados")
        void returnsMemberDtos() {
            UUID memberA = UUID.randomUUID();
            UUID memberB = UUID.randomUUID();
            when(projectService.findByIdEntity(projectId)).thenReturn(project);
            when(repository.findAllByProjectId(projectId))
                    .thenReturn(List.of(newAllocation(memberA), newAllocation(memberB)));
            when(memberClient.findAllByIds(any())).thenReturn(List.of(
                    new MemberView(memberA, "Ana", MemberAssignment.FUNCIONARIO),
                    new MemberView(memberB, "Bruno", MemberAssignment.FUNCIONARIO)
            ));

            List<MemberResponseDTO> result = service.listAllocations(projectId);

            assertThat(result)
                    .hasSize(2)
                    .extracting(MemberResponseDTO::id)
                    .containsExactlyInAnyOrder(memberA, memberB);
        }

        @Test
        @DisplayName("filtra órfãos quando membro não retorna do MemberClient")
        void filtersOrphans() {
            UUID memberA = UUID.randomUUID();
            UUID memberB = UUID.randomUUID();
            when(projectService.findByIdEntity(projectId)).thenReturn(project);
            when(repository.findAllByProjectId(projectId))
                    .thenReturn(List.of(newAllocation(memberA), newAllocation(memberB)));
            when(memberClient.findAllByIds(any())).thenReturn(List.of(
                    new MemberView(memberA, "Ana", MemberAssignment.FUNCIONARIO)
            ));

            List<MemberResponseDTO> result = service.listAllocations(projectId);

            assertThat(result)
                    .hasSize(1)
                    .extracting(MemberResponseDTO::id)
                    .containsExactly(memberA);
        }
    }

    private ProjectAllocation newAllocation(UUID memberId) {
        ProjectAllocation allocation = new ProjectAllocation();
        allocation.setId(UUID.randomUUID());
        allocation.setProject(project);
        allocation.setMemberId(memberId);
        return allocation;
    }
}
