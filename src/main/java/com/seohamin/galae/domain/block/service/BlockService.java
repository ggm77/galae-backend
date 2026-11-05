package com.seohamin.galae.domain.block.service;

import com.seohamin.galae.domain.block.dto.BlockResponseDto;
import com.seohamin.galae.domain.block.entity.UserBlock;
import com.seohamin.galae.domain.block.repository.UserBlockRepository;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 사용자 차단
 * 차단 목록은 서버에 저장하고, 댓글·그룹 메시지·익명 의견을 내려줄 때마다 이 목록으로 걸러 낸다
 */
@Service
@RequiredArgsConstructor
public class BlockService {

    //JPQL의 not in에 빈 목록을 넘기면 DB마다 동작이 달라서, 비었을 때 쓸 없는 ID
    private static final List<Long> NONE = List.of(-1L);

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    /**
     * 응답 필터링용: 내가 차단한 유저 ID
     */
    @Transactional(readOnly = true)
    public Set<Long> blockedIds(final Long userId) {
        return new HashSet<>(userBlockRepository.findBlockedIds(userId));
    }

    /**
     * 쿼리의 not in 파라미터용. 차단한 사람이 없으면 [-1]
     */
    public static Collection<Long> forQuery(final Set<Long> blockedIds) {
        return blockedIds.isEmpty() ? NONE : blockedIds;
    }

    @Transactional(readOnly = true)
    public List<BlockResponseDto> getBlocks(final Long userId) {
        return userBlockRepository.findAllWithBlockedByBlockerId(userId).stream()
                .map(BlockResponseDto::new)
                .toList();
    }

    /**
     * 차단 목록 전체 교체
     * 목록에 없는 기존 차단은 풀고, 새로 들어온 사람은 차단한다 (이미 차단한 사람의 차단 시각은 그대로)
     */
    @Transactional
    public List<BlockResponseDto> replaceBlocks(final List<Long> userIds, final Long userId) {
        final Set<Long> requested = new LinkedHashSet<>(userIds);
        validateTargets(requested, userId);

        if (requested.isEmpty()) {
            userBlockRepository.deleteAllByBlockerId(userId);
        } else {
            userBlockRepository.deleteAllByBlockerIdExcept(userId, requested);
        }

        final Set<Long> already = blockedIds(userId);
        final User blocker = userRepository.getReferenceById(userId);
        for (final Long targetId : requested) {
            if (!already.contains(targetId)) {
                userBlockRepository.save(UserBlock.builder()
                        .blocker(blocker)
                        .blocked(userRepository.getReferenceById(targetId))
                        .build());
            }
        }

        return getBlocks(userId);
    }

    /**
     * 한 명 차단 (이미 차단했으면 그대로)
     */
    @Transactional
    public void block(final Long targetId, final Long userId) {
        validateTargets(Set.of(targetId), userId);
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetId)) {
            userBlockRepository.save(UserBlock.builder()
                    .blocker(userRepository.getReferenceById(userId))
                    .blocked(userRepository.getReferenceById(targetId))
                    .build());
        }
    }

    /**
     * 한 명 차단 해제 (차단하지 않았으면 그대로)
     */
    @Transactional
    public void unblock(final Long targetId, final Long userId) {
        userBlockRepository.findByBlockerIdAndBlockedId(userId, targetId).ifPresent(userBlockRepository::delete);
    }

    private void validateTargets(final Set<Long> targetIds, final Long userId) {
        if (targetIds.contains(userId)) {
            throw new CustomException(ExceptionCode.CANNOT_BLOCK_SELF);
        }
        if (!targetIds.isEmpty() && userRepository.countByIdIn(targetIds) != targetIds.size()) {
            throw new CustomException(ExceptionCode.USER_NOT_EXIST, "존재하지 않는 유저가 목록에 있습니다.");
        }
    }
}
