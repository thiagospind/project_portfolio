package com.codegroup.portfolio.domain.entity;

import com.codegroup.portfolio.common.persistence.AbstractCrudEntity;
import com.codegroup.portfolio.domain.enums.MemberAssignment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "member")
public class Member extends AbstractCrudEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment", nullable = false, length = 50)
    private MemberAssignment assignment;
}
