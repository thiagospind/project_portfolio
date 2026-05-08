package com.codegroup.portfolio.service;

import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.domain.enums.RiskLevel;
import org.springframework.stereotype.Service;

import java.time.Period;

@Service
public class RiskClassifier {

    public RiskLevel classify(final Project project) {
        long durationMonths = 0L;
        if (project.getInitDate() != null && project.getEndPreviewDate() != null) {
            Period period = Period.between(project.getInitDate(), project.getEndPreviewDate());
            durationMonths = period.toTotalMonths();
            if (period.getDays() > 0) {
                durationMonths++;
            }
        }
        return RiskLevel.from(project.getTotalBudget(), durationMonths);
    }
}
