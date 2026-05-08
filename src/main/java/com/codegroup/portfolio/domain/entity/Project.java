package com.codegroup.portfolio.domain.entity;

import com.codegroup.portfolio.common.persistence.AbstractCrudEntity;
import com.codegroup.portfolio.domain.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "project")
public class Project extends AbstractCrudEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "init_date", nullable = false)
    private LocalDate initDate;

    @Column(name = "end_preview_date", nullable = false)
    private LocalDate endPreviewDate;

    @Column(name = "end_real_date")
    private LocalDate endRealDate;

    @Column(name = "total_budget", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_status", nullable = false, length = 50)
    private ProjectStatus actualStatus;

    @Column(name = "manager_id", nullable = false)
    private UUID managerId;
}
