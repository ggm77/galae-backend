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
}
