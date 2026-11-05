package com.seohamin.galae.domain.group.repository.message;

import com.seohamin.galae.domain.group.entity.message.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    //그룹 대화: 질문별 메시지 (오래된 순)
    @Query("""
            select m from GroupMessage m join fetch m.author left join fetch m.choice
            where m.group.id = :groupId and m.question.id = :questionId
            order by m.createdAt asc, m.id asc
            """)
    List<GroupMessage> findAllByGroupIdAndQuestionId(@Param("groupId") Long groupId, @Param("questionId") String questionId);

    //내가 이 질문으로 보낸 공유 메시지
    @Query("""
            select m from GroupMessage m join fetch m.choice
            where m.author.id = :userId and m.question.id = :questionId and m.choice is not null
            """)
    List<GroupMessage> findMyShares(@Param("userId") Long userId, @Param("questionId") String questionId);

    //오늘 나눈 멤버: 그룹별 공유 메시지 작성자 [groupId, authorId]
    @Query("""
            select distinct m.group.id, m.author.id from GroupMessage m
            where m.group.id in :groupIds and m.question.id = :questionId and m.choice is not null
            """)
    List<Object[]> findSharedAuthors(@Param("groupIds") Collection<Long> groupIds, @Param("questionId") String questionId);

    //업적 '길동무'·'동반자' 판정용: 공유한 질문 수
    @Query("select count(distinct m.question.id) from GroupMessage m where m.author.id = :userId and m.choice is not null")
    long countSharedQuestions(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GroupMessage m where m.group.id = :groupId")
    void deleteAllByGroupId(@Param("groupId") Long groupId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GroupMessage m where m.group.id = :groupId and m.author.id = :userId")
    void deleteAllByGroupIdAndAuthorId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
