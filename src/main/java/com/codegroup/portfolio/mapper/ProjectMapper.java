package com.codegroup.portfolio.mapper;

import com.codegroup.portfolio.client.MemberView;
import com.codegroup.portfolio.domain.entity.Project;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.dto.project.ProjectRequestDTO;
import com.codegroup.portfolio.dto.project.ProjectResponseDTO;
import com.codegroup.portfolio.service.RiskClassifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = RiskClassifier.class, imports = {LocalDateTime.class, UUID.class})
public interface ProjectMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "actualStatus", ignore = true)
    Project mapToEntityInsert(ProjectRequestDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "actualStatus", ignore = true)
    void updateEntity(@MappingTarget Project entity, ProjectRequestDTO requestDTO);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "name", source = "entity.name")
    @Mapping(target = "manager", source = "memberView")
    @Mapping(target = "riskLevel", source = "entity")
    ProjectResponseDTO mapToResponseDTO(Project entity, MemberView memberView);

    MemberResponseDTO mapMemberViewToResponse(MemberView view);
}
