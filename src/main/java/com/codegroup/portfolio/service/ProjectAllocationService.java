package com.codegroup.portfolio.service;

import com.codegroup.portfolio.client.MemberClient;
import com.codegroup.portfolio.client.MemberView;
import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.domain.entity.ProjectAllocation;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.exception.Response404Exception;
import com.codegroup.portfolio.exception.Response422Exception;
import com.codegroup.portfolio.repository.ProjectAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAllocationService {

    static final int MAX_MEMBERS_PER_PROJECT = 10;
    static final int MAX_ACTIVE_PROJECTS_PER_MEMBER = 3;

    private final ProjectService projectService;
    private final MemberClient memberClient;
    private final ProjectAllocationRepository repository;

    @Transactional
    public void allocate(final UUID projectId, final UUID memberId) {
        Project project = projectService.findByIdEntity(projectId);
        MemberView memberView = ensureMemberCanBeAllocated(memberId);
        ensureNotDuplicated(projectId, memberId);
        ensureProjectHasCapacity(projectId);
        ensureMemberHasCapacity(memberId);

        ProjectAllocation allocation = new ProjectAllocation();
        allocation.setId(UUID.randomUUID());
        allocation.setProject(project);
        allocation.setMemberId(memberView.id());
        allocation.setCreatedAt(LocalDateTime.now());
        repository.save(allocation);
    }

    @Transactional
    public void deallocate(final UUID projectId, final UUID memberId) {
        ProjectAllocation allocation = repository.findByProjectIdAndMemberId(projectId, memberId)
                .orElseThrow(() -> new Response404Exception(
                        "error.allocation.not.found",
                        memberId.toString(), projectId.toString()));
        repository.delete(allocation);
    }

    public List<MemberResponseDTO> listAllocations(final UUID projectId) {
        projectService.findByIdEntity(projectId);

        List<ProjectAllocation> allocations = repository.findAllByProjectId(projectId);
        if (allocations.isEmpty()) {
            return List.of();
        }

        Set<UUID> memberIds = allocations.stream()
                .map(ProjectAllocation::getMemberId)
                .collect(Collectors.toSet());
        Map<UUID, MemberView> byId = memberClient.findAllByIds(memberIds).stream()
                .collect(Collectors.toMap(MemberView::id, Function.identity()));

        return allocations.stream()
                .map(a -> byId.get(a.getMemberId()))
                .filter(Objects::nonNull)
                .map(v -> new MemberResponseDTO(v.id(), v.name(), v.assignment()))
                .toList();
    }

    private MemberView ensureMemberCanBeAllocated(final UUID memberId) {
        MemberView memberView = memberClient.findById(memberId)
                .orElseThrow(() -> new Response404Exception(
                        "error.member.not.found", memberId.toString()));
        if (!memberView.isFuncionario()) {
            throw new Response422Exception("error.allocation.member.invalid.assignment",
                    memberView.assignment().getDescription());
        }
        return memberView;
    }

    private void ensureNotDuplicated(final UUID projectId, final UUID memberId) {
        if (repository.existsByProjectIdAndMemberId(projectId, memberId)) {
            throw new Response422Exception("error.allocation.duplicate",
                    memberId.toString(), projectId.toString());
        }
    }

    private void ensureProjectHasCapacity(final UUID projectId) {
        if (repository.countByProjectId(projectId) >= MAX_MEMBERS_PER_PROJECT) {
            throw new Response422Exception("error.allocation.project.max.reached",
                    String.valueOf(MAX_MEMBERS_PER_PROJECT));
        }
    }

    private void ensureMemberHasCapacity(final UUID memberId) {
        if (repository.countActiveByMemberId(memberId) >= MAX_ACTIVE_PROJECTS_PER_MEMBER) {
            throw new Response422Exception("error.allocation.member.max.active.reached",
                    memberId.toString(), String.valueOf(MAX_ACTIVE_PROJECTS_PER_MEMBER));
        }
    }
}
