package com.seohamin.galae.domain.answer.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 연속 일수 계산 (docs/APP_FLOW.md 5-12, docs/ACHIEVEMENTS.md 판정 기준)
 */
public final class StreakCalculator {

    private StreakCalculator() {}

    /**
     * 지금 이어지고 있는 연속 일수
     * 오늘(아직 안 답했으면 어제)부터 거슬러 올라가며 하루도 빠짐없이 답한 날 수
     */
    public static int current(final Collection<LocalDate> answeredDays, final LocalDate today) {
        final Set<LocalDate> days = new HashSet<>(answeredDays);
        int streak = 0;
        for (LocalDate d = days.contains(today) ? today : today.minusDays(1); days.contains(d); d = d.minusDays(1)) {
            streak++;
        }
        return streak;
    }

    /**
     * 지금까지 가장 길게 이어진 연속 일수 (업적 판정용)
     */
    public static int longest(final Collection<LocalDate> answeredDays) {
        final List<LocalDate> days = new HashSet<>(answeredDays).stream().sorted().toList();
        int best = 0;
        int run = 0;
        for (int i = 0; i < days.size(); i++) {
            run = i > 0 && days.get(i - 1).plusDays(1).equals(days.get(i)) ? run + 1 : 1;
            best = Math.max(best, run);
        }
        return best;
    }
}
