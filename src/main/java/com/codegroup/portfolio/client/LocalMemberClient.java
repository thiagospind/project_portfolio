package com.codegroup.portfolio.client;

import com.codegroup.portfolio.domain.entity.Member;
import com.codegroup.portfolio.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class LocalMemberClient implements MemberClient {

    private final MemberRepository memberRepository;

    @Override
    public Optional<MemberView> findById(UUID id) {
        return memberRepository.findById(id).map(this::toView);
    }

    @Override
    public List<MemberView> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return memberRepository.findAllById(ids).stream()
                .map(this::toView)
                .toList();
    }

    private MemberView toView(Member member) {
        return new MemberView(member.getId(), member.getName(), member.getAssignment());
    }
}
