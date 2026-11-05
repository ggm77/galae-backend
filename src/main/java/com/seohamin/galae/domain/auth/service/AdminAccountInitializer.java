package com.seohamin.galae.domain.auth.service;

import com.seohamin.galae.domain.user.entity.Role;
import com.seohamin.galae.domain.user.entity.User;
import com.seohamin.galae.domain.user.repository.UserRepository;
import com.seohamin.galae.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * galae.admin.handle / password가 있으면 시작할 때 관리자 계정을 준비한다
 * 관리자는 질문 등록과 해설 검수(승인·반려)를 한다 (docs/PLAN.md 5.3, 9.1)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    @Value("${galae.admin.handle:}")
    private String handle;

    @Value("${galae.admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        if (handle.isBlank() || password.isBlank()) {
            return;
        }

        userRepository.findByHandle(handle).ifPresentOrElse(
                user -> user.updateRole(Role.ADMIN),
                () -> {
                    userRepository.save(User.builder()
                            .handle(handle)
                            .passwordHash(passwordEncoder.encode(password))
                            .name("관리자")
                            .role(Role.ADMIN)
                            .joinedOn(timeProvider.today())
                            .build());
                    log.info("관리자 계정을 만들었습니다: {}", handle);
                }
        );
    }
}
