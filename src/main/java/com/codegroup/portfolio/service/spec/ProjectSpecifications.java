package com.codegroup.portfolio.service.spec;

import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.domain.enums.ProjectStatus;
import com.codegroup.portfolio.dto.project.ProjectFilterDTO;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
public class ProjectSpecifications {
    public static Specification<Project> fromFilter(final ProjectFilterDTO f) {
        if (f == null) return Specification.unrestricted();
        return Specification.allOf(
                nameContains(f.name()),
                hasStatus(f.actualStatus()),
                hasManager(f.managerId()),
                initDateFrom(f.initDateFrom()),
                initDateTo(f.initDateTo()),
                totalBudgetFrom(f.totalBudgetFrom()),
                totalBudgetTo(f.totalBudgetTo())
        );
    }

    public static Specification<Project> nameContains(final String name) {
        if (StringUtils.isBlank(name)) return null;
        String pattern = "%" + name.toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Project> hasStatus(final ProjectStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("actualStatus"), status);
    }

    public static  Specification<Project> hasManager(final UUID managerId) {
        return managerId == null ? null : (root, q, cb) -> cb.equal((root.get("managerId")), managerId);
    }

    public static  Specification<Project> initDateFrom(final LocalDate from) {
        return from == null ? null : (root, q, cb) -> cb.equal(root.get("initDate"), from);
    }

    public static  Specification<Project> initDateTo(final LocalDate to) {
        return to == null ? null : (root, q, cb) -> cb.equal(root.get("initDate"), to);
    }

    public static  Specification<Project> totalBudgetFrom(final BigDecimal min) {
        return min == null ? null : (root, q, cb) -> cb.equal(root.get("totalBudget"), min);
    }

    public static  Specification<Project> totalBudgetTo(final BigDecimal max) {
        return max == null ? null : (root, q, cb) -> cb.equal(root.get("totalBudget"), max);
    }
}
