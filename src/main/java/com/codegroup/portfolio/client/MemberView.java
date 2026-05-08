package com.codegroup.portfolio.client;

import com.codegroup.portfolio.domain.enums.MemberAssignment;

import java.util.UUID;

public record MemberView(UUID id, String name, MemberAssignment assignment) {

    public boolean isFuncionario() {
        return assignment != null && assignment.canBeAllocatedToProject();
    }

    public boolean isGerente() {
        return assignment != null && assignment.canManageProject();
    }
}
