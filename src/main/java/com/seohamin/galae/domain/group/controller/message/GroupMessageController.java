package com.seohamin.galae.domain.group.controller.message;

import com.seohamin.galae.domain.group.dto.message.GroupMessageListResponseDto;
import com.seohamin.galae.domain.group.dto.message.GroupMessageRequestDto;
import com.seohamin.galae.domain.group.service.message.GroupMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GroupMessageController {

    private final GroupMessageService groupMessageService;

    //질문별 그룹 대화 조회 API
    @GetMapping("/groups/{groupId}/questions/{questionId}/messages")
    public ResponseEntity<GroupMessageListResponseDto> getMessages(
            @PathVariable("groupId") final Long groupId,
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(groupMessageService.getMessages(groupId, questionId, userId));
    }

    //일반 메시지 보내기 API
    @PostMapping("/groups/{groupId}/questions/{questionId}/messages")
    public ResponseEntity<GroupMessageListResponseDto.Item> sendMessage(
            @PathVariable("groupId") final Long groupId,
            @PathVariable("questionId") final String questionId,
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final GroupMessageRequestDto groupMessageRequestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupMessageService.sendMessage(groupId, questionId, groupMessageRequestDto, userId));
    }
}
