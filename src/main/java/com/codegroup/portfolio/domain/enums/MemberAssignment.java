package com.codegroup.portfolio.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberAssignment {
    FUNCIONARIO("Funcionário"),
    GERENTE("Gerente"),
    ADMINISTRATIVO("Administrativo"),
    ANALISTA("Analista");

    private final String description;

    public boolean canBeAllocatedToProject() {
        return this == FUNCIONARIO;
    }

    public boolean canManageProject() {
        return this == GERENTE;
    }
}
