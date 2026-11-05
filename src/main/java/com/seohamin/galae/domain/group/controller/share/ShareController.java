package com.seohamin.galae.domain.group.controller.share;

import com.seohamin.galae.domain.group.dto.share.ShareRequestDto;
import com.seohamin.galae.domain.group.dto.share.ShareResponseDto;
import com.seohamin.galae.domain.group.service.share.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    //나누기 상태 조회 API (보낸 그룹, 초안)
    @GetMapping("/questions/{questionId}/share")
    public ResponseEntity<ShareResponseDto> getShareStatus(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(shareService.getShareStatus(questionId, userId));
    }

    //나누기 API
    @PostMapping("/questions/{questionId}/share")
    public ResponseEntity<ShareResponseDto> share(
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final ShareRequestDto shareRequestDto
    ) {
        return ResponseEntity.ok(shareService.share(questionId, shareRequestDto, userId));
    }
}
