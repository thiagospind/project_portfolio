package com.codegroup.portfolio.service;

import com.codegroup.portfolio.domain.enums.ProjectStatus;
import com.codegroup.portfolio.exception.Response422Exception;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectStatusTransitionTest {

    private final ProjectStatusTransition transition = new ProjectStatusTransition();

    @Nested
    @DisplayName("ensureValidTransition")
    class EnsureValidTransition {

        @ParameterizedTest(name = "{0} → {1} é válida (sequência do lifecycle)")
        @CsvSource({
                "EM_ANALISE, ANALISE_REALIZADA",
                "ANALISE_REALIZADA, ANALISE_APROVADA",
                "ANALISE_APROVADA, INICIADO",
                "INICIADO, PLANEJADO",
                "PLANEJADO, EM_ANDAMENTO",
                "EM_ANDAMENTO, ENCERRADO"
        })
        void allowsForwardLifecycleTransitions(ProjectStatus current, ProjectStatus next) {
            assertThatCode(() -> transition.ensureValidTransition(current, next))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0} → CANCELADO é válida (cancelamento permitido em qualquer status não-terminal)")
        @EnumSource(value = ProjectStatus.class, names = {
                "EM_ANALISE", "ANALISE_REALIZADA", "ANALISE_APROVADA",
                "INICIADO", "PLANEJADO", "EM_ANDAMENTO"
        })
        void allowsCancelFromAnyNonTerminalStatus(ProjectStatus current) {
            assertThatCode(() -> transition.ensureValidTransition(current, ProjectStatus.CANCELADO))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "pular etapas ({0} → {1}) lança 422")
        @CsvSource({
                "EM_ANALISE, ANALISE_APROVADA",
                "EM_ANALISE, INICIADO",
                "ANALISE_REALIZADA, INICIADO",
                "ANALISE_APROVADA, EM_ANDAMENTO",
                "INICIADO, EM_ANDAMENTO",
                "PLANEJADO, ENCERRADO"
        })
        void rejectsSkippingStages(ProjectStatus current, ProjectStatus next) {
            assertThatThrownBy(() -> transition.ensureValidTransition(current, next))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.invalid.transition");
        }

        @ParameterizedTest(name = "voltar no lifecycle ({0} → {1}) lança 422")
        @CsvSource({
                "ANALISE_REALIZADA, EM_ANALISE",
                "INICIADO, ANALISE_APROVADA",
                "EM_ANDAMENTO, PLANEJADO"
        })
        void rejectsBackwardTransitions(ProjectStatus current, ProjectStatus next) {
            assertThatThrownBy(() -> transition.ensureValidTransition(current, next))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.invalid.transition");
        }

        @ParameterizedTest(name = "transição para o mesmo status ({0} → {0}) lança 422")
        @EnumSource(ProjectStatus.class)
        void rejectsSameStatusTransition(ProjectStatus current) {
            assertThatThrownBy(() -> transition.ensureValidTransition(current, current))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.invalid.transition");
        }

        @ParameterizedTest(name = "ENCERRADO é terminal: bloqueia transição para {0}")
        @EnumSource(value = ProjectStatus.class, names = {
                "EM_ANALISE", "ANALISE_REALIZADA", "ANALISE_APROVADA",
                "INICIADO", "PLANEJADO", "EM_ANDAMENTO", "CANCELADO"
        })
        void rejectsAnyTransitionFromEncerrado(ProjectStatus next) {
            assertThatThrownBy(() -> transition.ensureValidTransition(ProjectStatus.ENCERRADO, next))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.invalid.transition");
        }

        @ParameterizedTest(name = "CANCELADO é terminal: bloqueia transição para {0}")
        @EnumSource(value = ProjectStatus.class, names = {
                "EM_ANALISE", "ANALISE_REALIZADA", "ANALISE_APROVADA",
                "INICIADO", "PLANEJADO", "EM_ANDAMENTO", "ENCERRADO"
        })
        void rejectsAnyTransitionFromCancelado(ProjectStatus next) {
            assertThatThrownBy(() -> transition.ensureValidTransition(ProjectStatus.CANCELADO, next))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.invalid.transition");
        }

        @Test
        @DisplayName("next null lança 422 com fallback no param (defesa em profundidade)")
        void rejectsNullNext() {
            assertThatThrownBy(() -> transition.ensureValidTransition(ProjectStatus.EM_ANALISE, null))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.invalid.transition")
                    .hasFieldOrPropertyWithValue("params", new String[]{"Em análise", "(nulo)"});
        }

        @Test
        @DisplayName("exception carrega descriptions de current e next como params")
        void exceptionCarriesDescriptionParams() {
            assertThatThrownBy(() -> transition.ensureValidTransition(
                    ProjectStatus.EM_ANALISE, ProjectStatus.INICIADO))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("params",
                            new String[]{"Em análise", "Iniciado"});
        }
    }

    @Nested
    @DisplayName("ensureNotBlockingDeletion")
    class EnsureNotBlockingDeletion {

        @ParameterizedTest(name = "{0} bloqueia deleção (lança 422)")
        @EnumSource(value = ProjectStatus.class, names = {"INICIADO", "EM_ANDAMENTO", "ENCERRADO"})
        void rejectsBlockingStatuses(ProjectStatus current) {
            assertThatThrownBy(() -> transition.ensureNotBlockingDeletion(current))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.project.status.blocks.deletion");
        }

        @ParameterizedTest(name = "{0} permite deleção")
        @EnumSource(value = ProjectStatus.class, names = {
                "EM_ANALISE", "ANALISE_REALIZADA", "ANALISE_APROVADA", "PLANEJADO", "CANCELADO"
        })
        void allowsNonBlockingStatuses(ProjectStatus current) {
            assertThatCode(() -> transition.ensureNotBlockingDeletion(current))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("exception carrega description do status como param")
        void exceptionCarriesDescriptionParam() {
            assertThatThrownBy(() -> transition.ensureNotBlockingDeletion(ProjectStatus.EM_ANDAMENTO))
                    .isInstanceOf(Response422Exception.class)
                    .hasFieldOrPropertyWithValue("params",
                            new String[]{"Em andamento"});
        }
    }
}
