package com.seohamin.galae.domain.comment.controller.like;

import com.seohamin.galae.domain.comment.dto.like.CommentLikeResponseDto;
import com.seohamin.galae.domain.comment.service.like.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    //공감 API
    @PutMapping("/comments/{commentId}/like")
    public ResponseEntity<CommentLikeResponseDto> like(
            @PathVariable("commentId") final Long commentId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(commentLikeService.like(commentId, userId));
    }

    //공감 취소 API
    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<CommentLikeResponseDto> unlike(
            @PathVariable("commentId") final Long commentId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(commentLikeService.unlike(commentId, userId));
    }
}
