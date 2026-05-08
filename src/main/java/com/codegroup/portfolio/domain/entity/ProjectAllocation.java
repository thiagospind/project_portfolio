package com.codegroup.portfolio.domain.entity;

import com.codegroup.portfolio.common.persistence.AbstractCrudEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "project_member_allocation")
public class ProjectAllocation extends AbstractCrudEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id",nullable = false)
    private Project project;

    @Column(name = "member_id")
    private UUID memberId;
}
