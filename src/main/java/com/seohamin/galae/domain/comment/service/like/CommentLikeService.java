package com.seohamin.galae.domain.comment.service.like;

import com.seohamin.galae.domain.achievement.service.AchievementService;
import com.seohamin.galae.domain.comment.dto.like.CommentLikeResponseDto;
import com.seohamin.galae.domain.comment.entity.Comment;
import com.seohamin.galae.domain.comment.entity.like.CommentLike;
import com.seohamin.galae.domain.comment.repository.CommentRepository;
import com.seohamin.galae.domain.comment.repository.like.CommentLikeRepository;
import com.seohamin.galae.domain.feed.service.FeedService;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 공감
 * 공감·취소 모두 여러 번 눌러도 결과가 같다 (멱등)
 */
@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final FeedService feedService;
    private final AchievementService achievementService;

    @Transactional
    public CommentLikeResponseDto like(final Long commentId, final Long userId) {
        final Comment comment = getComment(commentId);

        if (commentLikeRepository.findByCommentIdAndUserId(commentId, userId).isEmpty()) {
            final User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));
            commentLikeRepository.save(CommentLike.builder().comment(comment).user(user).build());
            commentRepository.addLikeCount(commentId, 1);

            //경청자 업적
            achievementService.evaluate(userId);
        }

        return response(commentId, true);
    }

    @Transactional
    public CommentLikeResponseDto unlike(final Long commentId, final Long userId) {
        getComment(commentId);

        commentLikeRepository.findByCommentIdAndUserId(commentId, userId).ifPresent(like -> {
            commentLikeRepository.delete(like);
            commentRepository.addLikeCount(commentId, -1);
        });

        return response(commentId, false);
    }

    private Comment getComment(final Long commentId) {
        final Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_EXIST));
        feedService.getVisibleTopic(comment.getTopic().getId());
        return comment;
    }

    private CommentLikeResponseDto response(final Long commentId, final boolean liked) {
        final int count = commentRepository.findById(commentId).map(Comment::getLikeCount).orElse(0);
        return new CommentLikeResponseDto(commentId, liked, count);
    }
}
