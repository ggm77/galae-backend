package com.seohamin.galae.domain.comment.controller;

import com.seohamin.galae.domain.comment.dto.CommentRequestDto;
import com.seohamin.galae.domain.comment.dto.CommentResponseDto;
import com.seohamin.galae.domain.comment.dto.MyCommentResponseDto;
import com.seohamin.galae.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    //주제 댓글 목록 API
    @GetMapping("/feed/topics/{topicId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable("topicId") final String topicId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(commentService.getComments(topicId, userId));
    }

    //댓글·답글 남기기 API
    @PostMapping("/feed/topics/{topicId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable("topicId") final String topicId,
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final CommentRequestDto commentRequestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(topicId, commentRequestDto, userId));
    }

    //내 댓글 목록 API
    @GetMapping("/users/me/comments")
    public ResponseEntity<List<MyCommentResponseDto>> getMyComments(
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(commentService.getMyComments(userId));
    }
}
