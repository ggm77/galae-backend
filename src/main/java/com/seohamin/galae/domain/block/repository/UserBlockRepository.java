package com.seohamin.galae.domain.block.repository;

import com.seohamin.galae.domain.block.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    //내 차단 목록 (최근 차단 순)
    @Query("""
            select b from UserBlock b join fetch b.blocked
            where b.blocker.id = :blockerId
            order by b.createdAt desc, b.id desc
            """)
    List<UserBlock> findAllWithBlockedByBlockerId(@Param("blockerId") Long blockerId);

    //응답 필터링용: 내가 차단한 유저 ID
    @Query("select b.blocked.id from UserBlock b where b.blocker.id = :blockerId")
    List<Long> findBlockedIds(@Param("blockerId") Long blockerId);

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserBlock b where b.blocker.id = :blockerId and b.blocked.id not in :keepIds")
    void deleteAllByBlockerIdExcept(@Param("blockerId") Long blockerId, @Param("keepIds") Collection<Long> keepIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserBlock b where b.blocker.id = :blockerId")
    void deleteAllByBlockerId(@Param("blockerId") Long blockerId);

    //회원 탈퇴: 내가 한 차단과 나를 차단한 기록 모두
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserBlock b where b.blocker.id = :userId or b.blocked.id = :userId")
    void deleteAllInvolving(@Param("userId") Long userId);
}
