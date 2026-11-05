package com.seohamin.galae.global.exception.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {
    //400
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 정보가 잘못되어 있습니다."),
    INVALID_PHOTO(HttpStatus.BAD_REQUEST, "사진 형식이 올바르지 않습니다."),
    TOO_BIG_PHOTO(HttpStatus.BAD_REQUEST, "사진이 너무 큽니다."),
    HANDLE_DUPLICATE(HttpStatus.BAD_REQUEST, "이미 사용 중인 아이디입니다."),
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호가 올바르지 않습니다."),
    CHOICE_NOT_EXIST(HttpStatus.BAD_REQUEST, "선택지가 존재하지 않습니다."),
    GROUP_CODE_INVALID(HttpStatus.BAD_REQUEST, "초대 코드가 올바르지 않습니다."),
    GROUP_ALREADY_JOINED(HttpStatus.BAD_REQUEST, "이미 가입한 그룹입니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 내보낼 수 없습니다."),
    EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "내용을 입력해야 합니다."),
    QUESTION_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "같은 ID나 날짜의 질문이 이미 존재합니다."),
    QUESTION_HAS_ANSWERS(HttpStatus.BAD_REQUEST, "이미 답변이 있는 질문은 선택지를 바꿀 수 없습니다."),
    INVALID_REVIEW_STATUS(HttpStatus.BAD_REQUEST, "검수 결과는 APPROVED 또는 REJECTED여야 합니다."),
    ADMIN_CANNOT_WITHDRAW(HttpStatus.BAD_REQUEST, "관리자 계정은 탈퇴할 수 없습니다."),

    //401
    AUTHENTICATION_ERROR(HttpStatus.UNAUTHORIZED, "인증 되지 않았습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    //403
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 기능에 접근할 수 없습니다."),
    GROUP_NOT_JOINED(HttpStatus.FORBIDDEN, "가입하지 않은 그룹입니다."),
    ANSWER_REQUIRED(HttpStatus.FORBIDDEN, "내 답을 먼저 남겨야 볼 수 있습니다."),

    //404
    USER_NOT_EXIST(HttpStatus.NOT_FOUND, "유저가 존재하지 않습니다."),
    QUESTION_NOT_EXIST(HttpStatus.NOT_FOUND, "질문이 존재하지 않습니다."),
    ANSWER_NOT_EXIST(HttpStatus.NOT_FOUND, "답변이 존재하지 않습니다."),
    PERSPECTIVE_NOT_EXIST(HttpStatus.NOT_FOUND, "해설이 존재하지 않습니다."),
    GROUP_NOT_EXIST(HttpStatus.NOT_FOUND, "그룹이 존재하지 않습니다."),
    MEMBER_NOT_EXIST(HttpStatus.NOT_FOUND, "그룹 멤버가 존재하지 않습니다."),
    TOPIC_NOT_EXIST(HttpStatus.NOT_FOUND, "주제가 존재하지 않습니다."),
    COMMENT_NOT_EXIST(HttpStatus.NOT_FOUND, "댓글이 존재하지 않습니다."),
    PHOTO_NOT_EXIST(HttpStatus.NOT_FOUND, "사진이 존재하지 않습니다."),

    //5xx
    AI_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "AI 호출에 실패했습니다."),
    AI_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "AI API 키가 설정되어 있지 않습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "현재 서버를 이용할 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다.")
    ;

    private final HttpStatus status;
    private final String message;
}
