package com.seohamin.galae.domain.answer.controller;

import com.seohamin.galae.domain.answer.dto.AnswerProgressRequestDto;
import com.seohamin.galae.domain.answer.dto.AnswerRequestDto;
import com.seohamin.galae.domain.answer.dto.AnswerResponseDto;
import com.seohamin.galae.domain.answer.dto.MyAnswerItemResponseDto;
import com.seohamin.galae.domain.answer.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    //내 답변 조회 API
    @GetMapping("/questions/{questionId}/answer")
    public ResponseEntity<AnswerResponseDto> getAnswer(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(answerService.getAnswer(questionId, userId));
    }

    //선택하기 API (처음 답하기, 다시 하기 모두)
    @PutMapping("/questions/{questionId}/answer")
    public ResponseEntity<AnswerResponseDto> submitAnswer(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final AnswerRequestDto answerRequestDto
    ) {
        return ResponseEntity.ok(answerService.submitAnswer(questionId, answerRequestDto, userId));
    }

    //확장하기 완료 API
    @PostMapping("/questions/{questionId}/answer/complete")
    public ResponseEntity<AnswerResponseDto> completeAnswer(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(answerService.completeAnswer(questionId, userId));
    }

    //진행 상태(마지막 단계, 공유 초안) 저장 API
    @PatchMapping("/questions/{questionId}/answer/progress")
    public ResponseEntity<AnswerResponseDto> updateProgress(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final AnswerProgressRequestDto answerProgressRequestDto
    ) {
        return ResponseEntity.ok(answerService.updateProgress(questionId, answerProgressRequestDto, userId));
    }

    //달력용 내 답변 목록 API
    @GetMapping("/users/me/answers")
    public ResponseEntity<List<MyAnswerItemResponseDto>> getMyAnswers(
            @AuthenticationPrincipal final Long userId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to
    ) {
        return ResponseEntity.ok(answerService.getMyAnswers(userId, from, to));
    }
}
