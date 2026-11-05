package com.seohamin.galae.domain.admin.question.controller;

import com.seohamin.galae.domain.admin.question.dto.*;
import com.seohamin.galae.domain.admin.question.service.AdminQuestionService;
import com.seohamin.galae.domain.question.entity.image.QuestionImage;
import com.seohamin.galae.domain.question.entity.perspective.PerspectiveStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import com.seohamin.galae.global.validation.Create;
import com.seohamin.galae.global.validation.Update;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 API (ROLE_ADMIN만, WebSecurityConfig에서 /api/v1/admin/** 제한)
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    //전체 질문 조회 API (공개 전 포함, 재검토 표시)
    @GetMapping("/questions")
    public ResponseEntity<List<AdminQuestionResponseDto>> getQuestions() {
        return ResponseEntity.ok(adminQuestionService.getQuestions());
    }

    //질문 등록 API
    @PostMapping("/questions")
    public ResponseEntity<AdminQuestionResponseDto> createQuestion(
            @Validated(Create.class) @RequestBody final AdminQuestionRequestDto requestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminQuestionService.createQuestion(requestDto));
    }

    //질문 수정 API
    @PatchMapping("/questions/{questionId}")
    public ResponseEntity<AdminQuestionResponseDto> updateQuestion(
            @PathVariable("questionId") final String questionId,
            @Validated(Update.class) @RequestBody final AdminQuestionRequestDto requestDto
    ) {
        return ResponseEntity.ok(adminQuestionService.updateQuestion(questionId, requestDto));
    }

    //질문 이미지 조회 API (공개 전 질문 포함, 관리자 페이지 미리보기용)
    @GetMapping("/questions/{questionId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("questionId") final String questionId) {
        final QuestionImage image = adminQuestionService.getImage(questionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.noStore())
                .body(image.getData());
    }

    //해설 등록 API (DRAFT)
    @PostMapping("/questions/{questionId}/perspectives")
    public ResponseEntity<AdminPerspectiveResponseDto> createPerspective(
            @PathVariable("questionId") final String questionId,
            @Valid @RequestBody final AdminPerspectiveRequestDto requestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminQuestionService.createPerspective(questionId, requestDto));
    }

    //검수 큐 API
    @GetMapping("/perspectives")
    public ResponseEntity<List<AdminPerspectiveResponseDto>> getPerspectives(
            @RequestParam(value = "status", defaultValue = "DRAFT") final PerspectiveStatus status
    ) {
        return ResponseEntity.ok(adminQuestionService.getPerspectives(status));
    }

    //해설 수정 API (다시 DRAFT)
    @PutMapping("/perspectives/{perspectiveId}")
    public ResponseEntity<AdminPerspectiveResponseDto> updatePerspective(
            @PathVariable("perspectiveId") final Long perspectiveId,
            @Valid @RequestBody final AdminPerspectiveRequestDto requestDto
    ) {
        return ResponseEntity.ok(adminQuestionService.updatePerspective(perspectiveId, requestDto));
    }

    //승인·반려 API
    @PatchMapping("/perspectives/{perspectiveId}/review")
    public ResponseEntity<AdminPerspectiveResponseDto> reviewPerspective(
            @PathVariable("perspectiveId") final Long perspectiveId,
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final PerspectiveReviewRequestDto requestDto
    ) {
        return ResponseEntity.ok(adminQuestionService.reviewPerspective(perspectiveId, requestDto, userId));
    }
}
