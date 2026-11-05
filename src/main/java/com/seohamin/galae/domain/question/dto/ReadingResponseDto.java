package com.seohamin.galae.domain.question.dto;

import com.seohamin.galae.domain.question.entity.perspective.Perspective;
import lombok.Getter;

import java.util.List;

/**
 * 확장하기: 승인된 관점별 해설
 */
@Getter
public class ReadingResponseDto {

    private final String questionId;
    private final List<PerspectiveItem> perspectives;

    public ReadingResponseDto(final String questionId, final List<Perspective> perspectives) {
        this.questionId = questionId;
        this.perspectives = perspectives.stream().map(PerspectiveItem::new).toList();
    }

    @Getter
    public static class PerspectiveItem {
        private final String label;
        private final String body;
        private final String verse;
        private final String ref;

        public PerspectiveItem(final Perspective perspective) {
            this.label = perspective.getLabel();
            this.body = perspective.getBody();
            this.verse = perspective.getVerse();
            this.ref = perspective.getRef();
        }
    }
}
