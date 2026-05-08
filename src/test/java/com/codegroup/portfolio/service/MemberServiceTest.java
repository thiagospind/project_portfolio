package com.codegroup.portfolio.service;

import com.codegroup.portfolio.domain.entity.Member;
import com.codegroup.portfolio.domain.enums.MemberAssignment;
import com.codegroup.portfolio.dto.member.MemberRequestDTO;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.exception.Response404Exception;
import com.codegroup.portfolio.mapper.MemberMapper;
import com.codegroup.portfolio.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberMapper mapper;

    @Mock
    private MemberRepository repository;

    @InjectMocks
    private MemberService service;

    private UUID memberId;
    private Member member;
    private MemberResponseDTO sentinelResponse;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        member = new Member();
        member.setId(memberId);
        member.setName("João");
        member.setAssignment(MemberAssignment.FUNCIONARIO);
        sentinelResponse = new MemberResponseDTO(memberId, "João", MemberAssignment.FUNCIONARIO);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happy path: gera UUID, persiste e retorna DTO")
        void createsMember() {
            MemberRequestDTO req = new MemberRequestDTO("João", MemberAssignment.FUNCIONARIO);
            Member mapped = new Member();
            when(mapper.mapToEntityInsert(req)).thenReturn(mapped);
            when(repository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.mapToResponseDTO(any(Member.class))).thenReturn(sentinelResponse);

            MemberResponseDTO result = service.create(req);

            assertThat(result).isSameAs(sentinelResponse);
            ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("retorna 404 quando membro não encontrado")
        void throwsWhenNotFound() {
            when(repository.findById(memberId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(memberId))
                    .isInstanceOf(Response404Exception.class)
                    .hasFieldOrPropertyWithValue("messageKey", "error.member.not.found");
        }

        @Test
        @DisplayName("happy path: retorna DTO")
        void returnsDto() {
            when(repository.findById(memberId)).thenReturn(Optional.of(member));
            when(mapper.mapToResponseDTO(member)).thenReturn(sentinelResponse);

            MemberResponseDTO result = service.findById(memberId);

            assertThat(result).isSameAs(sentinelResponse);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("retorna page mapeada para DTOs")
        void returnsMappedPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Member> page = new PageImpl<>(List.of(member), pageable, 1);
            when(repository.findAll(pageable)).thenReturn(page);
            when(mapper.mapToResponseDTO(member)).thenReturn(sentinelResponse);

            Page<MemberResponseDTO> result = service.findAll(pageable);

            assertThat(result.getContent()).containsExactly(sentinelResponse);
        }

        @Test
        @DisplayName("retorna page vazia quando repository não retorna nada")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(Page.empty(pageable));

            Page<MemberResponseDTO> result = service.findAll(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }
}
