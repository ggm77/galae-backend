package com.seohamin.galae.domain.group.repository.member;

import com.seohamin.galae.domain.group.entity.member.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    //내가 가입한 그룹 (가입순)
    @Query("""
            select m from GroupMember m join fetch m.group g join fetch g.owner
            where m.user.id = :userId
            order by m.joinedAt asc, m.id asc
            """)
    List<GroupMember> findAllWithGroupByUserId(@Param("userId") Long userId);

    //그룹 멤버 (가입순)
    @Query("""
            select m from GroupMember m join fetch m.user
            where m.group.id = :groupId
            order by m.joinedAt asc, m.id asc
            """)
    List<GroupMember> findAllWithUserByGroupId(@Param("groupId") Long groupId);

    //그룹별 멤버 수 [groupId, count]
    @Query("""
            select m.group.id, count(m) from GroupMember m
            where m.group.id in :groupIds
            group by m.group.id
            """)
    List<Object[]> countByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    //가입한 그룹 ID 중 주어진 ID에 해당하는 것
    @Query("select m.group.id from GroupMember m where m.user.id = :userId and m.group.id in :groupIds")
    List<Long> findJoinedGroupIds(@Param("userId") Long userId, @Param("groupIds") Collection<Long> groupIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GroupMember m where m.group.id = :groupId")
    void deleteAllByGroupId(@Param("groupId") Long groupId);
}
