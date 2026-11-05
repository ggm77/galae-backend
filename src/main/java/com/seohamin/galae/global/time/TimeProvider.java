package com.seohamin.galae.global.time;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 서비스 기준 '오늘'을 정한다.
 * 답한 날, 오늘의 질문, 연속 일수, 생각 피드 순환이 모두 이 날짜를 따른다.
 * galae.today를 지정하면 그 날짜로 고정된다 (데모·테스트용. 프론트 데모의 오늘은 2026-09-25)
 */
@Component
public class TimeProvider {

    private final ZoneId zoneId;
    private final LocalDate fixedToday;

    public TimeProvider(
            @Value("${galae.zone:Asia/Seoul}") final String zone,
            @Value("${galae.today:}") final String fixedToday
    ) {
        this.zoneId = ZoneId.of(zone);
        this.fixedToday = fixedToday == null || fixedToday.isBlank() ? null : LocalDate.parse(fixedToday);
    }

    public LocalDate today() {
        return fixedToday != null ? fixedToday : LocalDate.now(zoneId);
    }
}
