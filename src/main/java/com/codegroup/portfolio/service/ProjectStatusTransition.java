package com.codegroup.portfolio.service;

import com.codegroup.portfolio.domain.enums.ProjectStatus;
import com.codegroup.portfolio.exception.Response422Exception;
import org.springframework.stereotype.Service;

@Service
public class ProjectStatusTransition {

    public void ensureValidTransition(final ProjectStatus current, final ProjectStatus next) {
        if (!current.canTransitionTo(next)) {
            throw new Response422Exception(
                    "error.project.status.invalid.transition",
                    current.getDescription(),
                    next == null ? "(nulo)" : next.getDescription()
            );
        }
    }

    public void ensureNotBlockingDeletion(final ProjectStatus current) {
        if (current.blocksDeletion()) {
            throw new Response422Exception(
                    "error.project.status.blocks.deletion",
                    current.getDescription()
            );
        }
    }
}
