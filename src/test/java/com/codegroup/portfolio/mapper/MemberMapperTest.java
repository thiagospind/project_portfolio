package com.codegroup.portfolio.mapper;

import com.codegroup.portfolio.domain.entity.Member;
import com.codegroup.portfolio.domain.enums.MemberAssignment;
import com.codegroup.portfolio.dto.member.MemberRequestDTO;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemberMapperTest {

    private final MemberMapper mapper = Mappers.getMapper(MemberMapper.class);

    @Nested
    @DisplayName("mapToEntityInsert")
    class MapToEntityInsert {

        @Test
        @DisplayName("retorna null quando request é null")
        void returnsNullForNullRequest() {
            assertThat(mapper.mapToEntityInsert(null)).isNull();
        }

        @Test
        @DisplayName("copia name e assignment do request")
        void mapsScalarFields() {
            MemberRequestDTO req = new MemberRequestDTO("Carla Souza", MemberAssignment.GERENTE);

            Member entity = mapper.mapToEntityInsert(req);

            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("Carla Souza");
            assertThat(entity.getAssignment()).isEqualTo(MemberAssignment.GERENTE);
        }

        @Test
        @DisplayName("ignora id (deve ser atribuído pelo serviço) e updatedAt")
        void ignoresIdAndUpdatedAt() {
            MemberRequestDTO req = new MemberRequestDTO("João", MemberAssignment.FUNCIONARIO);

            Member entity = mapper.mapToEntityInsert(req);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("preenche createdAt com LocalDateTime.now()")
        void setsCreatedAtToNow() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            MemberRequestDTO req = new MemberRequestDTO("João", MemberAssignment.FUNCIONARIO);

            Member entity = mapper.mapToEntityInsert(req);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(entity.getCreatedAt())
                    .isNotNull()
                    .isAfter(before)
                    .isBefore(after);
        }
    }

    @Nested
    @DisplayName("mapToResponseDTO")
    class MapToResponseDTO {

        @Test
        @DisplayName("retorna null quando entity é null")
        void returnsNullForNullEntity() {
            assertThat(mapper.mapToResponseDTO(null)).isNull();
        }

        @Test
        @DisplayName("copia id, name e assignment para o DTO")
        void mapsAllFields() {
            UUID id = UUID.randomUUID();
            Member entity = new Member();
            entity.setId(id);
            entity.setName("Carla Souza");
            entity.setAssignment(MemberAssignment.GERENTE);

            MemberResponseDTO dto = mapper.mapToResponseDTO(entity);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.name()).isEqualTo("Carla Souza");
            assertThat(dto.assignment()).isEqualTo(MemberAssignment.GERENTE);
        }
    }
}
