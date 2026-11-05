package com.seohamin.galae.domain.comment.service;

import com.seohamin.galae.domain.achievement.service.AchievementService;
import com.seohamin.galae.domain.block.service.BlockService;
import com.seohamin.galae.domain.comment.dto.CommentRequestDto;
import com.seohamin.galae.domain.comment.dto.CommentResponseDto;
import com.seohamin.galae.domain.comment.dto.MyCommentResponseDto;
import com.seohamin.galae.domain.comment.entity.Comment;
import com.seohamin.galae.domain.comment.repository.CommentRepository;
import com.seohamin.galae.domain.comment.repository.like.CommentLikeRepository;
import com.seohamin.galae.domain.feed.entity.Topic;
import com.seohamin.galae.domain.feed.service.FeedService;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 생각 탭의 댓글·답글
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final FeedService feedService;
    private final AchievementService achievementService;
    private final BlockService blockService;

    /**
     * 주제의 댓글 목록 (원 댓글마다 답글을 묶어서)
     * 내가 차단한 사람의 댓글·답글은 뺀다. 차단한 사람의 원 댓글은 그 아래 답글과 함께 빠진다
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(final String topicId, final Long userId) {
        feedService.getVisibleTopic(topicId);

        // 1) 모든 댓글(차단한 사람 제외)과 내가 공감한 댓글
        final Set<Long> blocked = blockService.blockedIds(userId);
        final List<Comment> comments = commentRepository.findAllByTopicId(topicId).stream()
                .filter(c -> !blocked.contains(c.getAuthor().getId()))
                .toList();
        if (comments.isEmpty()) {
            return List.of();
        }
        final Set<Long> liked = new HashSet<>(
                commentLikeRepository.findLikedCommentIds(userId, comments.stream().map(Comment::getId).toList())
        );

        // 2) 답글을 원 댓글 아래로 모은다
        final Map<Long, List<CommentResponseDto>> replies = new HashMap<>();
        for (final Comment c : comments) {
            if (c.getParent() != null) {
                replies.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>())
                        .add(new CommentResponseDto(c, userId, liked.contains(c.getId()), List.of()));
            }
        }

        return comments.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> new CommentResponseDto(c, userId, liked.contains(c.getId()), replies.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    /**
     * 댓글·답글 남기기
     * 답글에 답하면 원 댓글의 답글이 된다 (답글은 한 단계)
     */
    @Transactional
    public CommentResponseDto createComment(
            final String topicId,
            final CommentRequestDto commentRequestDto,
            final Long userId
    ) {
        // 1) 주제와 유저 확인
        final Topic topic = feedService.getVisibleTopic(topicId);
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        final String text = commentRequestDto.getText().trim();
        if (text.isEmpty()) {
            throw new CustomException(ExceptionCode.EMPTY_MESSAGE);
        }

        // 2) 답글이면 같은 주제의 원 댓글을 찾는다
        Comment parent = null;
        if (commentRequestDto.getParentId() != null) {
            parent = commentRepository.findById(commentRequestDto.getParentId())
                    .filter(c -> c.getTopic().getId().equals(topicId))
                    .orElseThrow(() -> new CustomException(ExceptionCode.COMMENT_NOT_EXIST));
            if (parent.getParent() != null) {
                parent = parent.getParent();
            }
        }

        // 3) 저장
        final Comment comment = commentRepository.save(Comment.builder()
                .topic(topic)
                .author(user)
                .parent(parent)
                .text(text)
                .build());

        // 4) 말동무, 논객 업적
        achievementService.evaluate(userId);

        return new CommentResponseDto(comment, userId, false, List.of());
    }

    /**
     * 내 댓글 목록
     */
    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getMyComments(final Long userId) {
        return commentRepository.findAllByAuthorId(userId).stream()
                .map(MyCommentResponseDto::new)
                .toList();
    }
}
