package com.codegroup.portfolio.dto.member;

import com.codegroup.portfolio.domain.enums.MemberAssignment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemberRequestDTO(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        MemberAssignment assignment
) {
}
