package com.seohamin.galae.global.health;

import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final EntityManager entityManager;

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/ready")
    public String ready() {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return "ready";
        } catch (Exception ex) {
            throw new CustomException(ExceptionCode.SERVICE_UNAVAILABLE);
        }
    }
}
