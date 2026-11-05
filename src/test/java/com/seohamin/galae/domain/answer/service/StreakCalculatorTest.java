package com.seohamin.galae.domain.answer.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    @Test
    void 오늘_답했으면_오늘부터_센다() {
        assertThat(StreakCalculator.current(List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(3);
    }

    @Test
    void 오늘_아직_안_답했으면_어제부터_센다() {
        assertThat(StreakCalculator.current(List.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(2);
    }

    @Test
    void 어제도_안_답했으면_0() {
        assertThat(StreakCalculator.current(List.of(TODAY.minusDays(2)), TODAY)).isZero();
    }

    @Test
    void 가장_길게_이어진_날을_센다() {
        final List<LocalDate> days = List.of(
                TODAY.minusDays(10), TODAY.minusDays(9), TODAY.minusDays(8), TODAY.minusDays(7),
                TODAY.minusDays(3), TODAY.minusDays(2),
                TODAY.minusDays(2)
        );
        assertThat(StreakCalculator.longest(days)).isEqualTo(4);
        assertThat(StreakCalculator.longest(List.of())).isZero();
    }
}
