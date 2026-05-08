package com.codegroup.portfolio.service;

import com.codegroup.portfolio.client.MemberClient;
import com.codegroup.portfolio.client.MemberView;
import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.domain.enums.ProjectStatus;
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
import com.codegroup.portfolio.service.spec.ProjectSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectMapper mapper;
    private final ProjectRepository repository;
    private final ProjectAllocationRepository allocationRepository;
    private final MemberClient memberClient;
    private final ProjectStatusTransition statusTransition;

    @Transactional
    public ProjectResponseDTO create(final ProjectRequestDTO requestDTO) {
        validateDates(requestDTO);
        MemberView managerView = ensureManagerIsGerente(requestDTO.managerId());

        Project project = mapper.mapToEntityInsert(requestDTO);
        project.setId(UUID.randomUUID());
        project.setActualStatus(ProjectStatus.EM_ANALISE);
        Project saved = repository.save(project);

        return buildResponse(saved, managerView);
    }

    public ProjectResponseDTO findById(final UUID id) {
        Project project = findByIdEntity(id);
        MemberView managerView = fetchManagerView(project.getManagerId());
        return buildResponse(project, managerView);
    }

    public Project findByIdEntity(final UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new Response404Exception("error.project.not.found", id.toString()));
    }

    public Page<ProjectResponseDTO> findAll(final ProjectFilterDTO filter, final Pageable pageable) {
        Page<Project> page = repository.findAll(ProjectSpecifications.fromFilter(filter), pageable);
        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<UUID> managerIds = page.stream().map(Project::getManagerId).collect(Collectors.toSet());
        Map<UUID, MemberView> byId = memberClient.findAllByIds(managerIds).stream()
                .collect(Collectors.toMap(MemberView::id, Function.identity()));

        return page.map(p -> {
            MemberView view = byId.get(p.getManagerId());
            if (view == null) {
                throw new Response500Exception("error.project.manager.gone", p.getManagerId().toString());
            }
            return mapper.mapToResponseDTO(p, view);
        });
    }

    @Transactional
    public ProjectResponseDTO update(final UUID id, final ProjectRequestDTO requestDTO) {
        Project project = findByIdEntity(id);
        MemberView memberView = ensureManagerIsGerente(requestDTO.managerId());
        mapper.updateEntity(project, requestDTO);
        Project saved = repository.save(project);
        return buildResponse(saved, memberView);

    }

    @Transactional
    public ProjectResponseDTO updateStatus(final UUID id, final ProjectStatusUpdateDTO statusDTO) {
        Project project = findByIdEntity(id);
        statusTransition.ensureValidTransition(project.getActualStatus(), statusDTO.status());
        if (statusDTO.status() == ProjectStatus.INICIADO) {
            ensureProjectHasAtLeastOneAllocation(id);
        }
        project.setActualStatus(statusDTO.status());
        if (statusDTO.status() == ProjectStatus.ENCERRADO) {
            project.setEndRealDate(LocalDate.now());
        }
        Project saved = repository.save(project);
        return buildResponse(saved, fetchManagerView(project.getManagerId()));
    }

    @Transactional
    public void delete(final UUID id) {
        Project project = findByIdEntity(id);
        ProjectStatus current = project.getActualStatus();
        statusTransition.ensureNotBlockingDeletion(current);
        repository.delete(project);
    }

    private ProjectResponseDTO buildResponse(final Project project, final MemberView managerView) {
        return mapper.mapToResponseDTO(project, managerView);
    }

    private void validateDates(final ProjectRequestDTO requestDTO) {
        LocalDate initDate = requestDTO.initDate();
        LocalDate endPreviewDate = requestDTO.endPreviewDate();
        LocalDate endRealDate = requestDTO.endRealDate();

        if (endPreviewDate != null && endPreviewDate.isBefore(initDate)) {
            throw new Response422Exception("error.project.dates.endPreview.before.init",
                    endPreviewDate.toString(),
                    initDate.toString());
        }

        if (endRealDate != null && endRealDate.isBefore(initDate)) {
            throw new Response422Exception("error.project.dates.endReal.before.init",
                    endRealDate.toString(),
                    initDate.toString());
        }
    }

    private MemberView ensureManagerIsGerente(final UUID managerId) {
        MemberView memberView = memberClient.findById(managerId)
                .orElseThrow(() -> new Response404Exception(
                        "error.member.not.found", managerId.toString()));
        if (!memberView.isGerente()) {
            throw new Response422Exception("error.project.manager.invalid.assignment",
                    memberView.assignment().getDescription());
        }
        return memberView;
    }

    private void ensureProjectHasAtLeastOneAllocation(final UUID projectId) {
        if (allocationRepository.countByProjectId(projectId) < 1) {
            throw new Response422Exception("error.project.start.no.allocations", projectId.toString());
        }
    }

    private MemberView fetchManagerView(UUID managerId) {
        return memberClient.findById(managerId)
                .orElseThrow(() -> new Response500Exception(
                        "error.project.manager.gone", managerId.toString()));
    }
}
