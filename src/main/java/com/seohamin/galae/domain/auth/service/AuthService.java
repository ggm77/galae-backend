package com.seohamin.galae.domain.auth.service;

import com.seohamin.galae.domain.auth.dto.LoginRequestDto;
import com.seohamin.galae.domain.auth.dto.PasswordChangeRequestDto;
import com.seohamin.galae.domain.auth.dto.RefreshTokenRequestDto;
import com.seohamin.galae.domain.auth.dto.SignupRequestDto;
import com.seohamin.galae.domain.auth.dto.TokenResponseDto;
import com.seohamin.galae.domain.user.entity.Role;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.auth.jwt.JwtProvider;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자체 로그인 (아이디·비밀번호)
 * 가입·로그인에 성공하면 액세스·리프레시 토큰을 발급한다
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TimeProvider timeProvider;

    /**
     * 회원가입하는 메서드
     * @param signupRequestDto 아이디, 비밀번호, 이름
     * @return 토큰
     */
    @Transactional
    public TokenResponseDto signup(final SignupRequestDto signupRequestDto) {

        // 1) 아이디 중복 확인
        if (userRepository.existsByHandle(signupRequestDto.getHandle())) {
            throw new CustomException(ExceptionCode.HANDLE_DUPLICATE);
        }

        // 2) 유저 저장
        final User user = userRepository.save(User.builder()
                .handle(signupRequestDto.getHandle())
                .passwordHash(passwordEncoder.encode(signupRequestDto.getPassword()))
                .name(signupRequestDto.getName().trim())
                .role(Role.USER)
                .joinedOn(timeProvider.today())
                .build());

        // 3) 토큰 발급
        return issueToken(user);
    }

    /**
     * 로그인하는 메서드
     * 아이디가 없을 때와 비밀번호가 틀릴 때 같은 에러를 줘서 아이디 존재 여부를 숨긴다
     */
    public TokenResponseDto login(final LoginRequestDto loginRequestDto) {

        final User user = userRepository.findByHandle(loginRequestDto.getHandle())
                .orElseThrow(() -> new CustomException(ExceptionCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPasswordHash())) {
            throw new CustomException(ExceptionCode.LOGIN_FAILED);
        }

        return issueToken(user);
    }

    /**
     * 리프레시 토큰으로 토큰을 재발급하는 메서드
     */
    public TokenResponseDto refreshToken(final RefreshTokenRequestDto refreshTokenRequestDto) {

        final Long userId = jwtProvider.parseRefreshToken(refreshTokenRequestDto.getRefreshToken());

        //탈퇴 등으로 유저가 없으면 토큰이 무효
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_TOKEN));

        return issueToken(user);
    }

    /**
     * 비밀번호를 바꾸는 메서드
     */
    @Transactional
    public void changePassword(final PasswordChangeRequestDto passwordChangeRequestDto, final Long userId) {

        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_EXIST));

        if (!passwordEncoder.matches(passwordChangeRequestDto.getCurrentPassword(), user.getPasswordHash())) {
            throw new CustomException(ExceptionCode.LOGIN_FAILED);
        }

        user.updatePasswordHash(passwordEncoder.encode(passwordChangeRequestDto.getNewPassword()));
    }

    private TokenResponseDto issueToken(final User user) {
        return TokenResponseDto.builder()
                .userId(user.getId())
                .tokenType(jwtProvider.getTokenType())
                .accessToken(jwtProvider.createAccessToken(user.getId(), user.getRole()))
                .exprTime(jwtProvider.getAccessTokenExpirationSeconds())
                .refreshToken(jwtProvider.createRefreshToken(user.getId()))
                .build();
    }
}
