package com.seohamin.galae.domain.user.controller;

import com.seohamin.galae.domain.achievement.dto.AchievementResponseDto;
import com.seohamin.galae.domain.achievement.service.AchievementService;
import com.seohamin.galae.domain.user.dto.UserProfileResponseDto;
import com.seohamin.galae.domain.user.dto.UserRequestDto;
import com.seohamin.galae.domain.user.dto.WithdrawRequestDto;
import com.seohamin.galae.domain.user.service.UserService;
import com.seohamin.galae.domain.user.service.WithdrawService;
import jakarta.validation.Valid;
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
public class UserController {

    private final UserService userService;
    private final WithdrawService withdrawService;
    private final AchievementService achievementService;

    //내 프로필 API
    @GetMapping("/users/me")
    public ResponseEntity<UserProfileResponseDto> getMe(
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(userService.getProfile(userId, userId));
    }

    //프로필 편집 API
    @PatchMapping("/users/me")
    public ResponseEntity<UserProfileResponseDto> updateMe(
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final UserRequestDto userRequestDto
    ) {
        return ResponseEntity.ok(userService.updateProfile(userRequestDto, userId));
    }

    //회원 탈퇴 API (비밀번호 확인). 그룹 나가기·답변·댓글·공감·업적을 함께 지운다
    @DeleteMapping("/users/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final WithdrawRequestDto withdrawRequestDto
    ) {
        withdrawService.withdraw(withdrawRequestDto, userId);
        return ResponseEntity.noContent().build();
    }

    //내 열매 API
    @GetMapping("/users/me/achievements")
    public ResponseEntity<List<AchievementResponseDto>> getMyAchievements(
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(achievementService.getAchievements(userId));
    }

    //다른 사람 프로필 API
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserProfileResponseDto> getUser(
            @PathVariable("userId") final Long targetUserId,
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(userService.getProfile(targetUserId, userId));
    }

    //프로필 사진 API (인증 없이)
    @GetMapping("/users/{userId}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable("userId") final Long userId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(userService.getPhoto(userId));
    }
}
