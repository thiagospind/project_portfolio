package com.codegroup.portfolio.repository;

import com.codegroup.portfolio.domain.entity.ProjectAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface
ProjectAllocationRepository extends JpaRepository<ProjectAllocation, UUID> {

    List<ProjectAllocation> findAllByProjectId(UUID projectId);

    boolean existsByProjectIdAndMemberId(UUID projectId, UUID memberId);

    Optional<ProjectAllocation> findByProjectIdAndMemberId(UUID projectId, UUID memberId);

    long countByProjectId(UUID projectId);

    @Query("""
            select count(a) from ProjectAllocation a
            where a.memberId = :memberId
              and a.project.actualStatus not in
                  (com.codegroup.portfolio.domain.enums.ProjectStatus.ENCERRADO,
                   com.codegroup.portfolio.domain.enums.ProjectStatus.CANCELADO)
            """)
    long countActiveByMemberId(@Param("memberId") UUID memberId);

    @Query("select count(distinct a.memberId) from ProjectAllocation a")
    long countDistinctMembers();
}
