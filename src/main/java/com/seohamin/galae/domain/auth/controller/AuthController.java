package com.seohamin.galae.domain.auth.controller;

import com.seohamin.galae.domain.auth.dto.LoginRequestDto;
import com.seohamin.galae.domain.auth.dto.PasswordChangeRequestDto;
import com.seohamin.galae.domain.auth.dto.RefreshTokenRequestDto;
import com.seohamin.galae.domain.auth.dto.SignupRequestDto;
import com.seohamin.galae.domain.auth.dto.TokenResponseDto;
import com.seohamin.galae.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //회원가입 API
    @PostMapping("/auth/signup")
    public ResponseEntity<TokenResponseDto> signup(
            @Valid @RequestBody final SignupRequestDto signupRequestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(signupRequestDto));
    }

    //로그인 API
    @PostMapping("/auth/login")
    public ResponseEntity<TokenResponseDto> login(
            @Valid @RequestBody final LoginRequestDto loginRequestDto
    ) {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    //토큰 리프레시 API
    @PostMapping("/auth/token/refresh")
    public ResponseEntity<TokenResponseDto> refreshToken(
            @Valid @RequestBody final RefreshTokenRequestDto refreshTokenRequestDto
    ) {
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequestDto));
    }

    //비밀번호 변경 API
    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final PasswordChangeRequestDto passwordChangeRequestDto
    ) {
        authService.changePassword(passwordChangeRequestDto, userId);
        return ResponseEntity.noContent().build();
    }
}
