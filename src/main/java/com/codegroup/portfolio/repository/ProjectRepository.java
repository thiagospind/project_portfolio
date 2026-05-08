package com.codegroup.portfolio.repository;

import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.dto.report.StatusSummaryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    @Query("""
            select new com.codegroup.portfolio.dto.report.StatusSummaryDTO(
                p.actualStatus, count(p), coalesce(sum(p.totalBudget), 0))
            from Project p
            group by p.actualStatus
            """)
    List<StatusSummaryDTO> aggregateByStatus();

    @Query("""
            select avg((p.endRealDate - p.initDate) by day)
            from Project p
            where p.actualStatus = com.codegroup.portfolio.domain.enums.ProjectStatus.ENCERRADO
              and p.endRealDate is not null
            """)
    Double averageClosedDurationDays();
}
