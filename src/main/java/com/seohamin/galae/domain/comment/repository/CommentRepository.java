package com.seohamin.galae.domain.comment.repository;

import com.seohamin.galae.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    //주제의 모든 댓글과 답글 (오래된 순)
    @Query("""
            select c from Comment c join fetch c.author left join fetch c.parent
            where c.topic.id = :topicId
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findAllByTopicId(@Param("topicId") String topicId);

    //내 댓글 (최신순)
    @Query("""
            select c from Comment c join fetch c.topic left join fetch c.parent
            where c.author.id = :userId
            order by c.createdAt desc, c.id desc
            """)
    List<Comment> findAllByAuthorId(@Param("userId") Long userId);

    //주제별 댓글 수 [topicId, count]
    @Query("select c.topic.id, count(c) from Comment c where c.topic.id in :topicIds group by c.topic.id")
    List<Object[]> countByTopicIds(@Param("topicIds") Collection<String> topicIds);

    long countByAuthorId(Long authorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount + :delta where c.id = :commentId")
    void addLikeCount(@Param("commentId") Long commentId, @Param("delta") int delta);

    /* ---------- 회원 탈퇴 ----------
     * MariaDB는 같은 테이블을 서브쿼리로 참조하는 UPDATE/DELETE를 막으므로(오류 1093)
     * 내 댓글 ID 목록을 먼저 조회해서 넘긴다 */

    @Query("select c.id from Comment c where c.author.id = :userId")
    List<Long> findIdsByAuthorId(@Param("userId") Long userId);

    //내가 공감한 댓글의 공감 수를 하나씩 줄인다 (공감 기록을 지우기 전에)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Comment c set c.likeCount = c.likeCount - 1
            where c.id in (select l.comment.id from CommentLike l where l.user.id = :userId)
            """)
    void decreaseLikeCountLikedBy(@Param("userId") Long userId);

    //다른 사람이 내 댓글에 단 답글은 지우지 않고 원 댓글로 올린다
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.parent = null where c.parent.id in :parentIds and c.author.id <> :userId")
    void detachRepliesOf(@Param("parentIds") Collection<Long> parentIds, @Param("userId") Long userId);

    //답글을 먼저 지우고(부모 FK) 원 댓글을 지운다
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.author.id = :userId and c.parent is not null")
    void deleteRepliesByAuthorId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.author.id = :userId")
    void deleteAllByAuthorId(@Param("userId") Long userId);
}
