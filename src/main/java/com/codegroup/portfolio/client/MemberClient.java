package com.codegroup.portfolio.client;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberClient {

    Optional<MemberView> findById(UUID id);

    List<MemberView> findAllByIds(Collection<UUID> ids);
}
