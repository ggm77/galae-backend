package com.seohamin.galae.domain.question.controller;

import com.seohamin.galae.domain.question.dto.*;
import com.seohamin.galae.domain.question.entity.image.QuestionImage;
import com.seohamin.galae.domain.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    //질문 목록 API (검색, 상태 필터: all, unanswered, answered)
    @GetMapping("/questions")
    public ResponseEntity<List<QuestionSummaryResponseDto>> getQuestions(
            @AuthenticationPrincipal final Long userId,
            @RequestParam(value = "query", required = false) final String query,
            @RequestParam(value = "filter", defaultValue = "all") final String filter
    ) {
        return ResponseEntity.ok(questionService.getQuestions(userId, query, filter));
    }

    //오늘의 질문 API
    @GetMapping("/questions/today")
    public ResponseEntity<QuestionResponseDto> getTodayQuestion(
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(questionService.getTodayQuestion(userId));
    }

    //질문 상세 API
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponseDto> getQuestion(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(questionService.getQuestion(questionId, userId));
    }

    //결과 API (내 답을 남긴 뒤에만)
    @GetMapping("/questions/{questionId}/result")
    public ResponseEntity<QuestionResultResponseDto> getResult(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(questionService.getResult(questionId, userId));
    }

    //선택지 코멘트 API (내 답을 남긴 뒤에만)
    @GetMapping("/questions/{questionId}/choices/{choiceId}/opinions")
    public ResponseEntity<OpinionListResponseDto> getOpinions(
            @PathVariable("questionId") final String questionId,
            @PathVariable("choiceId") final Integer choiceId,
            @AuthenticationPrincipal final Long userId,
            @RequestParam(value = "page", defaultValue = "0") final int page,
            @RequestParam(value = "size", defaultValue = "20") final int size
    ) {
        return ResponseEntity.ok(questionService.getOpinions(questionId, choiceId, userId, page, size));
    }

    //질문 이미지 API (공개된 질문만, 인증 없이 - img 태그용)
    @GetMapping("/questions/{questionId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("questionId") final String questionId) {
        final QuestionImage image = questionService.getImage(questionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(image.getData());
    }

    //확장하기 API (내 답을 남긴 뒤에만)
    @GetMapping("/questions/{questionId}/reading")
    public ResponseEntity<ReadingResponseDto> getReading(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(questionService.getReading(questionId, userId));
    }
}
