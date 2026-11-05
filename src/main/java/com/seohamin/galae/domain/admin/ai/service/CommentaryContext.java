package com.seohamin.galae.domain.admin.ai.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 해설 한 건을 만드는 데 필요한 입력 (DB 트랜잭션 밖에서 AI를 부르기 위해 미리 복사해 둔다)
 *
 * @param questionId 질문 ID
 * @param title      질문 제목
 * @param body       질문 본문
 * @param choices    선택지 (A, B, C 순)
 * @param label      관점
 * @param valueTags  필수 가치 태그
 * @param sources    인용할 수 있는 출처 (ref 순서 유지)
 */
public record CommentaryContext(
        String questionId,
        String title,
        String body,
        List<ChoiceItem> choices,
        String label,
        List<String> valueTags,
        List<SourceItem> sources
) {

    public record ChoiceItem(String option, int choiceId, String text) {}

    public record SourceItem(String ref, String text, String note) {}

    public Map<String, SourceItem> sourcesByRef() {
        final Map<String, SourceItem> map = new LinkedHashMap<>();
        sources.forEach(s -> map.put(s.ref(), s));
        return map;
    }

    //선택지 번호 0, 1, 2 → A, B, C
    public static String optionOf(final int choiceIndex) {
        return String.valueOf((char) ('A' + choiceIndex));
    }
}
