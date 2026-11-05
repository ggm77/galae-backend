package com.seohamin.galae.global.auth.filter;

import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.auth.jwt.JwtProvider;
import com.seohamin.galae.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 필터에 JWT 검증 과정 추가
 * 토큰이 없거나 검증에 실패하면 익명으로 진행되고,
 * 인증이 필요한 API라면 시큐리티의 AuthenticationEntryPoint가 401을 준다
 * principal은 Long 타입 유저 ID라서 컨트롤러에서 @AuthenticationPrincipal Long userId로 받는다
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String authorizationHeader = httpServletRequest.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                final Claims claims = jwtProvider.parseAccessToken(authorizationHeader.substring(7));
                final Long userId = Long.parseLong(claims.getSubject());

                //탈퇴한 유저의 토큰은 만료 전이라도 쓰지 못하게 한다 (JWT는 서버에 저장하지 않아서 따로 확인)
                if (userRepository.existsById(userId)) {
                    final UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, jwtProvider.getAuthorities(claims));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (CustomException | NumberFormatException ex) {
                //검증 실패시 익명으로 진행
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
