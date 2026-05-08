package com.codegroup.portfolio.mapper;

import com.codegroup.portfolio.domain.entity.Member;
import com.codegroup.portfolio.dto.member.MemberRequestDTO;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class, UUID.class})
public interface MemberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", ignore = true)
    Member mapToEntityInsert(MemberRequestDTO requestDTO);

    MemberResponseDTO mapToResponseDTO(Member entity);
}
