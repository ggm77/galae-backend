package com.seohamin.galae.domain.group.repository;

import com.seohamin.galae.domain.group.entity.GalaeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<GalaeGroup, Long> {

    Optional<GalaeGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    //업적 '개척자' 판정용
    boolean existsByOwnerId(Long ownerId);
}
