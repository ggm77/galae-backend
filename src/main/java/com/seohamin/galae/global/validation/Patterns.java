package com.seohamin.galae.global.validation;

/**
 * 프론트(docs/APP_FLOW.md)와 맞춘 입력 규칙
 */
public final class Patterns {

    private Patterns() {}

    //아이디: 영문 소문자·숫자·마침표·밑줄, 1~20자
    public static final String HANDLE = "^[a-z0-9._]{1,20}$";

    //프로필·그룹 색
    public static final String COLOR = "^(sage|blue|peach|lilac)$";

    //질문 ID (예: q037)
    public static final String QUESTION_ID = "^[a-z0-9_-]{1,20}$";

    //생각 피드 주제 ID (예: p001)
    public static final String TOPIC_ID = "^[a-z0-9_-]{1,20}$";
}
