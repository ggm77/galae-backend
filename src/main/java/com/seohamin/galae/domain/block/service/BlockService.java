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
import java.util.stream.Collectors;

/**
 * 사용자 차단
 * 차단할 사람은 아이디(handle)로 지정하고, 저장은 유저 ID로 한다.
 * 그래서 상대가 아이디를 바꿔도 차단은 유지되고, 목록은 늘 현재 아이디로 보인다.
 * 차단 목록으로 댓글·그룹 메시지·익명 의견을 내려줄 때마다 걸러 낸다
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
     * 차단 목록 전체 교체 (아이디 목록)
     * 목록에 없는 기존 차단은 풀고, 새로 들어온 사람은 차단한다 (이미 차단한 사람의 차단 시각은 그대로)
     */
    @Transactional
    public List<BlockResponseDto> replaceBlocks(final List<String> handles, final Long userId) {
        final Set<Long> requested = resolve(handles, userId);

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
    public void block(final String handle, final Long userId) {
        final Long targetId = resolve(List.of(handle), userId).iterator().next();
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
    public void unblock(final String handle, final Long userId) {
        final User target = userRepository.findByHandle(handle)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST, "존재하지 않는 아이디입니다: " + handle));
        userBlockRepository.findByBlockerIdAndBlockedId(userId, target.getId()).ifPresent(userBlockRepository::delete);
    }

    /**
     * 아이디 목록을 유저 ID로 바꾼다. 같은 아이디는 한 번으로 본다
     * 나 자신이 있거나 없는 아이디가 있으면 거절한다
     */
    private Set<Long> resolve(final List<String> handles, final Long userId) {
        final Set<String> requested = handles.stream().map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            return Set.of();
        }

        final Map<String, Long> found = userRepository.findAllByHandleIn(requested).stream()
                .collect(Collectors.toMap(User::getHandle, User::getId));

        final List<String> missing = requested.stream().filter(h -> !found.containsKey(h)).toList();
        if (!missing.isEmpty()) {
            throw new CustomException(ExceptionCode.USER_NOT_EXIST, "존재하지 않는 아이디입니다: " + String.join(", ", missing));
        }
        if (found.containsValue(userId)) {
            throw new CustomException(ExceptionCode.CANNOT_BLOCK_SELF);
        }

        return requested.stream().map(found::get).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
