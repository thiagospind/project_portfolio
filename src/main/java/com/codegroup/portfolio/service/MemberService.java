package com.codegroup.portfolio.service;

import com.codegroup.portfolio.domain.entity.Member;
import com.codegroup.portfolio.dto.member.MemberRequestDTO;
import com.codegroup.portfolio.dto.member.MemberResponseDTO;
import com.codegroup.portfolio.exception.Response404Exception;
import com.codegroup.portfolio.mapper.MemberMapper;
import com.codegroup.portfolio.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberMapper mapper;
    private final MemberRepository repository;

    @Transactional
    public MemberResponseDTO create(final MemberRequestDTO requestDTO) {
        Member member = mapper.mapToEntityInsert(requestDTO);
        member.setId(UUID.randomUUID());
        member = repository.save(member);
        return mapper.mapToResponseDTO(member);
    }

    public MemberResponseDTO findById(final UUID id) {
        return mapper.mapToResponseDTO(repository.findById(id)
                .orElseThrow(() -> new Response404Exception("error.member.not.found", id.toString())));
    }

    public Page<MemberResponseDTO> findAll(final Pageable pageable) {
        return repository.findAll(pageable).map(mapper::mapToResponseDTO);
    }
}
