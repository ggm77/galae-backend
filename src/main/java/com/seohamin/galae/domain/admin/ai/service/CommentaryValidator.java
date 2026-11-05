package com.seohamin.galae.domain.admin.ai.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 자동 검증 (docs/PLAN.md 5.7)
 * - JSON 파싱 성공 여부
 * - sources의 모든 항목이 입력으로 준 목록 안에 있는가
 * - 길이 제한(300자)
 * - 금지어 정규식 (정답|오답|틀렸|해야 합니다|잘못된 선택)
 * - 필수 가치 태그가 누락되지 않았는가
 * - for_each_option이 모든 선택지를 포함하는가
 * 장절 주소와 본문 일치는 원문을 서버가 출처 목록에서 그대로 붙여서 보장한다
 */
public final class CommentaryValidator {

    private CommentaryValidator() {}

    public static final int MAX_BODY = 300;
    public static final Pattern FORBIDDEN = Pattern.compile("(정답|오답|틀렸|해야 합니다|잘못된 선택)");

    /**
     * 모델 출력에서 JSON 객체를 꺼낸다 (코드 블록이나 머리말이 붙어도)
     * @return 파싱 실패시 null
     */
    public static JsonNode parse(final JsonMapper jsonMapper, final String content) {
        if (content == null) {
            return null;
        }
        final int start = content.indexOf('{');
        final int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            final JsonNode node = jsonMapper.readTree(content.substring(start, end + 1));
            return node != null && node.isObject() ? node : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * @return 실패 사유 목록. 비어 있으면 통과
     */
    public static List<String> validate(final JsonNode node, final CommentaryContext ctx) {
        final List<String> errors = new ArrayList<>();
        if (node == null) {
            errors.add("JSON 객체로 읽을 수 없음");
            return errors;
        }

        // 1) 충돌하는 가치와 필수 가치 태그
        final List<String> values = strings(node.path("values_in_tension"));
        if (values.size() < 2) {
            errors.add("values_in_tension이 2개 미만");
        }
        for (final String tag : ctx.valueTags()) {
            if (values.stream().noneMatch(v -> v.equals(tag.trim()))) {
                errors.add("필수 가치 누락: " + tag);
            }
        }

        // 2) 모든 선택지의 가장 강한 논거
        final Map<String, String> cases = new LinkedHashMap<>();
        for (final JsonNode item : node.path("for_each_option")) {
            cases.put(text(item.path("option")).trim().toUpperCase(Locale.ROOT), text(item.path("strongest_case")).trim());
        }
        final Set<String> options = new LinkedHashSet<>();
        for (final CommentaryContext.ChoiceItem choice : ctx.choices()) {
            options.add(choice.option());
            final String strongest = cases.get(choice.option());
            if (strongest == null) {
                errors.add("for_each_option에 선택지 " + choice.option() + " 없음");
            } else if (strongest.isEmpty()) {
                errors.add("선택지 " + choice.option() + "의 논거가 비어 있음");
            } else {
                forbidden("for_each_option " + choice.option(), strongest, errors);
            }
        }
        cases.keySet().stream().filter(o -> !options.contains(o)).forEach(o -> errors.add("for_each_option에 없는 선택지: " + o));

        // 3) 본문
        final String body = text(node.path("body")).trim();
        if (body.isEmpty()) {
            errors.add("body가 비어 있음");
        } else if (body.length() > MAX_BODY) {
            errors.add("body가 " + MAX_BODY + "자 초과 (" + body.length() + "자)");
        }
        forbidden("body", body, errors);

        // 4) 출처는 입력 목록 안에서만
        final Set<String> allowed = ctx.sourcesByRef().keySet();
        for (final String ref : strings(node.path("sources"))) {
            if (!allowed.contains(ref)) {
                errors.add("출처 목록 밖 인용: " + ref);
            }
        }

        // 5) 독자에게 남기는 질문
        final String openQuestion = text(node.path("open_question")).trim();
        if (openQuestion.isEmpty()) {
            errors.add("open_question이 비어 있음");
        }
        forbidden("open_question", openQuestion, errors);

        return errors;
    }

    //문자열·숫자 같은 값 노드만 글자로 바꾼다. 객체·배열·없음은 빈 문자열
    public static String text(final JsonNode node) {
        return node != null && node.isValueNode() && !node.isNull() ? node.asString() : "";
    }

    public static List<String> strings(final JsonNode array) {
        final List<String> result = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(v -> {
                if (v.isString() && !v.asString().isBlank()) {
                    result.add(v.asString().trim());
                }
            });
        }
        return result;
    }

    private static void forbidden(final String field, final String text, final List<String> errors) {
        final Matcher m = FORBIDDEN.matcher(text);
        final Set<String> found = new LinkedHashSet<>();
        while (m.find()) {
            found.add(m.group());
        }
        if (!found.isEmpty()) {
            errors.add("금지 표현 (" + field + "): " + String.join(", ", found));
        }
    }
}
