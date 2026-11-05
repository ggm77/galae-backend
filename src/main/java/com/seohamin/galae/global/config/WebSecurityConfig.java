package com.seohamin.galae.global.config;

import com.seohamin.galae.global.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

import java.time.LocalDateTime;

/**
 * 자체 로그인(아이디·비밀번호) + JWT 기반 보안 설정
 * 세션을 만들지 않고, 매 요청의 Authorization 헤더로 인증한다
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    protected SecurityFilterChain configure(final HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .securityContext(c -> c.securityContextRepository(new NullSecurityContextRepository())) //http에서 세션 생성 방지
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        //CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        //인증 필요 없는 API들
                        .requestMatchers(
                                "/ping",
                                "/ready",
                                "/error",
                                "/api/swagger/**",
                                "/api/v1/auth/**"
                        ).permitAll()

                        //관리자 페이지(정적 HTML). 페이지는 누구나 받지만, 안에서 부르는 /api/v1/admin/**는 ADMIN만 쓸 수 있다
                        .requestMatchers(HttpMethod.GET, "/admin", "/admin/", "/admin/**").permitAll()

                        //사진은 <img> 태그로 불러오므로 토큰 없이 조회 가능
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/photo", "/api/v1/groups/*/photo", "/api/v1/questions/*/image", "/api/v1/feed/topics/*/image").permitAll()

                        //어드민 API는 어드민만 사용가능
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        //그 외 요청은 인증 필요
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        //토큰이 없거나 만료된 경우
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write(
                                    "{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":\"UNAUTHORIZED\",\"code\":\"AUTHENTICATION_ERROR\",\"message\":\"토큰이 만료되었거나 없습니다.\"}"
                            );
                        })
                        //권한이 부족한 경우
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write(
                                    "{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":\"FORBIDDEN\",\"code\":\"PERMISSION_DENIED\",\"message\":\"해당 기능에 접근할 수 없습니다.\"}"
                            );
                        }));

        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }
}
