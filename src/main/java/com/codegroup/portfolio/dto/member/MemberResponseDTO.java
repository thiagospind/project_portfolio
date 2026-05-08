package com.codegroup.portfolio.dto.member;

import com.codegroup.portfolio.domain.enums.MemberAssignment;

import java.util.UUID;

public record MemberResponseDTO(
        UUID id,
        String name,
        MemberAssignment assignment
) {
}
