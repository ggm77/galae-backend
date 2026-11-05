package com.seohamin.galae.global.auth.jwt;

import com.seohamin.galae.domain.user.entity.Role;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT를 생성하고 검증한다.
 * 시크릿키와 만료 시간은 application.yaml에서 가져옴
 * 액세스 토큰과 리프레시 토큰은 token_type 클레임으로 구분해서
 * 리프레시 토큰을 액세스 토큰처럼 쓰지 못하게 한다
 */
@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    //HS512, BASE64로 인코딩된 64바이트 이상 키
    public JwtProvider(
            @Value("${jwt.secret}") final String secret,
            @Value("${jwt.access-token.expr-time}") final long accessTokenExpirationSeconds,
            @Value("${jwt.refresh-token.expr-time}") final long refreshTokenExpirationSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String getTokenType() {
        return "Bearer";
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    /**
     * 액세스 토큰을 생성하는 메서드
     * @param userId 유저의 고유 아이디 번호
     * @param role 유저 권한
     * @return JWT
     */
    public String createAccessToken(final Long userId, final Role role) {
        final Instant now = Instant.now();

        return Jwts.builder()
                .header().type("JWT").and()
                .subject(userId.toString())
                .claim(TOKEN_TYPE_CLAIM, ACCESS)
                .claim(AUTHORITIES_CLAIM, List.of(role.getKey()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpirationSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 리프레시 토큰을 생성하는 메서드
     * @param userId 유저의 고유 아이디 번호
     * @return JWT
     */
    public String createRefreshToken(final Long userId) {
        final Instant now = Instant.now();

        return Jwts.builder()
                .header().type("JWT").and()
                .subject(userId.toString())
                .claim(TOKEN_TYPE_CLAIM, REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenExpirationSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 액세스 토큰을 검증하고 Claims를 리턴하는 메서드
     * 검증 실패시 INVALID_TOKEN 예외
     */
    public Claims parseAccessToken(final String jwt) {
        return parse(jwt, ACCESS);
    }

    /**
     * 리프레시 토큰을 검증하고 유저 ID를 리턴하는 메서드
     * 검증 실패시 INVALID_TOKEN 예외
     */
    public Long parseRefreshToken(final String jwt) {
        try {
            return Long.parseLong(parse(jwt, REFRESH).getSubject());
        } catch (NumberFormatException ex) {
            throw new CustomException(ExceptionCode.INVALID_TOKEN);
        }
    }

    /**
     * JWT에서 Authorities를 얻는 메서드
     * @param claims JWT의 claims
     * @return JWT의 Authorities
     */
    public List<SimpleGrantedAuthority> getAuthorities(final Claims claims) {
        final List<?> roles = claims.get(AUTHORITIES_CLAIM, List.class);
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(Object::toString)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private Claims parse(final String jwt, final String expectedType) {
        final Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new CustomException(ExceptionCode.INVALID_TOKEN);
        }

        if (!expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class)) || claims.getSubject() == null) {
            throw new CustomException(ExceptionCode.INVALID_TOKEN);
        }
        return claims;
    }
}
