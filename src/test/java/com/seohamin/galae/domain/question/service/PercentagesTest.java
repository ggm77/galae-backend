package com.seohamin.galae.domain.question.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PercentagesTest {

    @Test
    void 합이_100이_되도록_맞춘다() {
        final int[] pcts = QuestionService.percentages(new long[]{1, 1, 1});
        assertThat(Arrays.stream(pcts).sum()).isEqualTo(100);
        assertThat(pcts).containsExactlyInAnyOrder(34, 33, 33);
    }

    @Test
    void 응답이_없으면_모두_0() {
        assertThat(QuestionService.percentages(new long[]{0, 0, 0})).containsExactly(0, 0, 0);
    }

    @Test
    void 비율을_반올림에_가깝게_나눈다() {
        assertThat(QuestionService.percentages(new long[]{24, 67, 9})).containsExactly(24, 67, 9);
        assertThat(QuestionService.percentages(new long[]{2, 1})).containsExactly(67, 33);
    }
}
