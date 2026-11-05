package com.seohamin.galae.domain.admin.ai.controller;

import com.seohamin.galae.domain.admin.ai.dto.AiConfigResponseDto;
import com.seohamin.galae.domain.admin.ai.dto.PerspectiveGenerateRequestDto;
import com.seohamin.galae.domain.admin.ai.service.AiCommentaryService;
import com.seohamin.galae.domain.admin.question.dto.AdminPerspectiveResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 해설 생성 API (ROLE_ADMIN만, /api/v1/admin/**)
 * 관리자 페이지에서만 부른다. API 키는 서버에만 있다
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiCommentaryService aiCommentaryService;

    //AI 설정 조회 API (키 설정 여부, 기본 모델, 프롬프트 버전)
    @GetMapping("/ai")
    public ResponseEntity<AiConfigResponseDto> getConfig() {
        return ResponseEntity.ok(aiCommentaryService.getConfig());
    }

    //AI 해설 생성 API. 결과는 DRAFT(검증 통과) 또는 FAILED(검증 실패)로 저장된다
    @PostMapping("/questions/{questionId}/perspectives/generate")
    public ResponseEntity<AdminPerspectiveResponseDto> generate(
            @PathVariable("questionId") final String questionId,
            @Valid @RequestBody final PerspectiveGenerateRequestDto requestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiCommentaryService.generate(questionId, requestDto));
    }
}
