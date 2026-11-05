package com.seohamin.galae.domain.comment.repository.like;

import com.seohamin.galae.domain.comment.entity.like.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    //댓글 목록 중 내가 공감한 것
    @Query("select l.comment.id from CommentLike l where l.user.id = :userId and l.comment.id in :commentIds")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") Collection<Long> commentIds);

    //업적 '경청자' 판정용
    long countByUserId(Long userId);
}
