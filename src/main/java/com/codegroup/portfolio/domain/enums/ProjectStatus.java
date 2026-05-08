package com.codegroup.portfolio.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum ProjectStatus {
    EM_ANALISE("Em análise"),
    ANALISE_REALIZADA("Análise realizada"),
    ANALISE_APROVADA("Análise aprovada"),
    INICIADO("Iniciado"),
    PLANEJADO("Planejado"),
    EM_ANDAMENTO("Em andamento"),
    ENCERRADO("Encerrado"),
    CANCELADO("Cancelado");

    private final String description;

    private static final List<ProjectStatus> LIFECYCLE = List.of(
            EM_ANALISE,
            ANALISE_REALIZADA,
            ANALISE_APROVADA,
            INICIADO,
            PLANEJADO,
            EM_ANDAMENTO,
            ENCERRADO
    );

    private static final Set<ProjectStatus> DELETION_BLOCKING = Set.of(INICIADO, EM_ANDAMENTO, ENCERRADO);

    private static final Set<ProjectStatus> TERMINAL = Set.of(ENCERRADO, CANCELADO);

    public boolean canTransitionTo(ProjectStatus next) {
        if (next == null || next == this || TERMINAL.contains(this)) {
            return false;
        }
        if (next == CANCELADO) {
            return true;
        }
        int currentIndex = LIFECYCLE.indexOf(this);
        int nextIndex = LIFECYCLE.indexOf(next);
        return currentIndex >= 0 && nextIndex == currentIndex + 1;
    }

    public boolean blocksDeletion() {
        return DELETION_BLOCKING.contains(this);
    }
}
