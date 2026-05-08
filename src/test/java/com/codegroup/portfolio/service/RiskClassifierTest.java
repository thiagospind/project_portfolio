package com.codegroup.portfolio.service;

import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.domain.enums.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RiskClassifierTest {

    private final RiskClassifier classifier = new RiskClassifier();

    private Project project(BigDecimal budget, LocalDate init, LocalDate end) {
        Project p = new Project();
        p.setTotalBudget(budget);
        p.setInitDate(init);
        p.setEndPreviewDate(end);
        return p;
    }

    @Nested
    @DisplayName("Duração: dias residuais arredondam para cima")
    class DurationRounding {

        @Test
        @DisplayName("exatamente 3 meses (sem dias extras) → BAIXO")
        void exactlyThreeMonths() {
            Project p = project(new BigDecimal("50000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 8, 8));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_BAIXO);
        }

        @Test
        @DisplayName("3 meses + 1 dia → MEDIO (estouro de mês conta)")
        void threeMonthsPlusOneDay() {
            Project p = project(new BigDecimal("50000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 8, 9));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_MEDIO);
        }

        @Test
        @DisplayName("exatamente 6 meses → MEDIO")
        void exactlySixMonths() {
            Project p = project(new BigDecimal("50000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 11, 8));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_MEDIO);
        }

        @Test
        @DisplayName("6 meses + 2 dias (cenário reportado: 2026-05-08 → 2026-11-10) → ALTO")
        void sixMonthsPlusTwoDays() {
            Project p = project(new BigDecimal("50000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 11, 10));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_ALTO);
        }

        @Test
        @DisplayName("6 meses + 1 dia → ALTO")
        void sixMonthsPlusOneDay() {
            Project p = project(new BigDecimal("50000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 11, 9));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_ALTO);
        }

        @Test
        @DisplayName("0 meses (mesmo dia) → BAIXO se orçamento for baixo")
        void sameDay() {
            LocalDate date = LocalDate.of(2026, 5, 8);
            Project p = project(new BigDecimal("50000"), date, date);

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_BAIXO);
        }
    }

    @Nested
    @DisplayName("Datas nulas: duração = 0, classificação só por orçamento")
    class NullDates {

        @Test
        @DisplayName("ambas datas null + budget baixo → BAIXO")
        void bothDatesNullLowBudget() {
            Project p = project(new BigDecimal("50000"), null, null);

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_BAIXO);
        }

        @Test
        @DisplayName("ambas datas null + budget médio → MEDIO")
        void bothDatesNullMediumBudget() {
            Project p = project(new BigDecimal("200000"), null, null);

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_MEDIO);
        }

        @Test
        @DisplayName("ambas datas null + budget alto → ALTO")
        void bothDatesNullHighBudget() {
            Project p = project(new BigDecimal("600000"), null, null);

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_ALTO);
        }

        @Test
        @DisplayName("apenas initDate null → duração = 0")
        void onlyInitNull() {
            Project p = project(new BigDecimal("50000"), null, LocalDate.of(2027, 1, 1));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_BAIXO);
        }

        @Test
        @DisplayName("apenas endPreviewDate null → duração = 0")
        void onlyEndNull() {
            Project p = project(new BigDecimal("50000"), LocalDate.of(2026, 5, 8), null);

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_BAIXO);
        }
    }

    @Nested
    @DisplayName("Classificação combinando orçamento e duração")
    class BudgetAndDuration {

        @Test
        @DisplayName("orçamento ≤100k + duração ≤3 meses → BAIXO")
        void lowBoth() {
            Project p = project(new BigDecimal("100000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 8, 8));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_BAIXO);
        }

        @Test
        @DisplayName("orçamento >100k (≤500k) + duração curta → MEDIO")
        void mediumBudgetShortDuration() {
            Project p = project(new BigDecimal("300000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 7, 8));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_MEDIO);
        }

        @Test
        @DisplayName("orçamento >500k → ALTO mesmo com duração curta")
        void highBudgetTrumpsShortDuration() {
            Project p = project(new BigDecimal("600000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 6, 8));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_ALTO);
        }

        @Test
        @DisplayName("duração >6 meses → ALTO mesmo com orçamento baixo")
        void longDurationTrumpsLowBudget() {
            Project p = project(new BigDecimal("10000"),
                    LocalDate.of(2026, 5, 8),
                    LocalDate.of(2026, 12, 8));

            assertThat(classifier.classify(p)).isEqualTo(RiskLevel.RISCO_ALTO);
        }
    }
}
