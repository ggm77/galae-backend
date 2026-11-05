package com.seohamin.galae.domain.admin.topic.dto;

import com.seohamin.galae.global.validation.Create;
import com.seohamin.galae.global.validation.Patterns;
import com.seohamin.galae.global.validation.Update;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

/**
 * 생각 탭 주제 추가·수정 (관리자)
 * 수정할 때는 null인 필드를 바꾸지 않는다
 * 검증을 마친 주제의 내용(분류·제목·카드·출처)을 고치면 다시 보류가 된다 (docs/VERIFICATION.md)
 */
@Getter
public class AdminTopicRequestDto {

    //주제 ID (예: p006). 추가할 때만
    @NotNull(groups = Create.class)
    @Pattern(regexp = Patterns.TOPIC_ID, groups = Create.class)
    private String id;

    //동일성, 윤리, 논리 등
    @NotBlank(groups = Create.class)
    @Size(max = 20, groups = {Create.class, Update.class})
    private String category;

    @NotBlank(groups = Create.class)
    @Size(max = 100, groups = {Create.class, Update.class})
    private String title;

    //한 화면에 넘겨 보는 짧은 글 (보통 4~5장)
    @NotNull(groups = Create.class)
    @Size(min = 1, max = 10, groups = {Create.class, Update.class})
    private List<@NotBlank(groups = {Create.class, Update.class}) @Size(max = 500, groups = {Create.class, Update.class}) String> cards;

    //출처 (원전, 참고 자료)
    @Size(max = 10, groups = {Create.class, Update.class})
    @Valid
    private List<SourceItem> sources;

    //주제 이미지 data URL (data:image/jpeg|png|webp;base64,...), 1MB 이하. 수정할 때 빈 문자열이면 삭제
    private String image;

    @Getter
    public static class SourceItem {

        @NotBlank(groups = {Create.class, Update.class})
        @Size(max = 200, groups = {Create.class, Update.class})
        private String label;

        //없으면 null. 있으면 http(s) 주소
        @Size(max = 500, groups = {Create.class, Update.class})
        @Pattern(regexp = "^https?://\\S+$", groups = {Create.class, Update.class})
        private String url;
    }

    //내용(분류·제목·카드·출처) 중 하나라도 바꾸려는지
    public boolean changesContent() {
        return category != null || title != null || cards != null || sources != null;
    }
}
