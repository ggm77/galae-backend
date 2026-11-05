package com.seohamin.galae.domain.group.controller;

import com.seohamin.galae.domain.group.dto.GroupJoinRequestDto;
import com.seohamin.galae.domain.group.dto.GroupRequestDto;
import com.seohamin.galae.domain.group.dto.GroupResponseDto;
import com.seohamin.galae.domain.group.dto.GroupSummaryResponseDto;
import com.seohamin.galae.domain.group.service.GroupService;
import com.seohamin.galae.global.validation.Create;
import com.seohamin.galae.global.validation.Update;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    //가입한 그룹 목록 API
    @GetMapping("/groups")
    public ResponseEntity<List<GroupSummaryResponseDto>> getMyGroups(
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(groupService.getMyGroups(userId));
    }

    //그룹 만들기 API
    @PostMapping("/groups")
    public ResponseEntity<GroupResponseDto> createGroup(
            @AuthenticationPrincipal final Long userId,
            @Validated(Create.class) @RequestBody final GroupRequestDto groupRequestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(groupRequestDto, userId));
    }

    //초대 코드로 참여 API
    @PostMapping("/groups/join")
    public ResponseEntity<GroupResponseDto> joinGroup(
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final GroupJoinRequestDto groupJoinRequestDto
    ) {
        return ResponseEntity.ok(groupService.joinGroup(groupJoinRequestDto, userId));
    }

    //그룹 상세 API
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<GroupResponseDto> getGroup(
            @PathVariable("groupId") final Long groupId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(groupService.getGroup(groupId, userId));
    }

    //그룹 설정 API (그룹장만)
    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<GroupResponseDto> updateGroup(
            @PathVariable("groupId") final Long groupId,
            @AuthenticationPrincipal final Long userId,
            @Validated(Update.class) @RequestBody final GroupRequestDto groupRequestDto
    ) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, groupRequestDto, userId));
    }

    //그룹 나가기 API (그룹장이 나가면 그룹 삭제)
    @DeleteMapping("/groups/{groupId}/members/me")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable("groupId") final Long groupId,
            @AuthenticationPrincipal final Long userId
    ) {
        groupService.leaveGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    //멤버 내보내기 API (그룹장만)
    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable("groupId") final Long groupId,
            @PathVariable("memberId") final Long memberId,
            @AuthenticationPrincipal final Long userId
    ) {
        groupService.kickMember(groupId, memberId, userId);
        return ResponseEntity.noContent().build();
    }

    //그룹 사진 API (인증 없이)
    @GetMapping("/groups/{groupId}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable("groupId") final Long groupId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(groupService.getPhoto(groupId));
    }
}
