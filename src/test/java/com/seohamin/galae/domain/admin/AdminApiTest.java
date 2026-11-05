package com.seohamin.galae.domain.admin;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 테스트 설정의 관리자: admin / admin1234, 오늘은 2026-09-25
 */
class AdminApiTest extends ApiTestSupport {

    @Test
    void 일반_유저는_관리자_API를_쓸_수_없다() throws Exception {
        final String token = signup("일반");
        get("/api/v1/admin/questions", token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void 공개일_전_질문과_공개_안된_질문은_사용자에게_보이지_않는다() throws Exception {
        final String admin = login("admin", "admin1234");
        final String user = signup("사용자");

        final String body = """
                {"id":"t-future","date":"2026-09-26","title":"내일 질문","body":"내일 공개됩니다.","status":"PUBLISHED",
                 "choices":[{"text":"하나","shortText":"하나"},{"text":"둘","shortText":"둘"}]}
                """;
        postJson("/api/v1/admin/questions", admin, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.choices[1].id").value(1));

        //같은 날짜에는 하나만
        postJson("/api/v1/admin/questions", admin, body.replace("t-future", "t-future2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTION_ALREADY_EXIST"));

        get("/api/v1/questions/t-future", user).andExpect(status().isNotFound());
        get("/api/v1/questions", user).andExpect(jsonPath("$[*].id", not(hasItem("t-future"))));

        //오늘 날짜로 옮겨도 DRAFT면 보이지 않는다
        postJson("/api/v1/admin/questions", admin, """
                {"id":"t-draft","date":"2026-08-01","title":"초안","body":"초안입니다.",
                 "choices":[{"text":"하나","shortText":"하나"},{"text":"둘","shortText":"둘"}]}
                """).andExpect(jsonPath("$.status").value("DRAFT"));
        get("/api/v1/questions/t-draft", user).andExpect(status().isNotFound());

        //답변이 없으면 선택지를 바꿀 수 있다
        patchJson("/api/v1/admin/questions/t-draft", admin, """
                {"status":"PUBLISHED","choices":[{"text":"A","shortText":"A"},{"text":"B","shortText":"B"},{"text":"C","shortText":"C"}]}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.choices[2].text").value("C"));
        get("/api/v1/questions/t-draft", user).andExpect(status().isOk());

        //답변이 생기면 선택지는 바꿀 수 없다
        putJson("/api/v1/questions/t-draft/answer", user, "{\"choiceId\":2}").andExpect(status().isOk());
        patchJson("/api/v1/admin/questions/t-draft", admin, """
                {"choices":[{"text":"A","shortText":"A"},{"text":"B","shortText":"B"}]}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("QUESTION_HAS_ANSWERS"));
    }

    @Test
    void 해설은_승인해야_확장하기에_보인다() throws Exception {
        final String admin = login("admin", "admin1234");
        final String user = signup("독자");
        putJson("/api/v1/questions/q032/answer", user, "{\"choiceId\":0}").andExpect(status().isOk());

        final JsonNode draft = json(postJson("/api/v1/admin/questions/q032/perspectives", admin,
                "{\"label\":\"철학\",\"body\":\"약속에 대한 철학의 관점\",\"model\":\"claude-opus-5-5\",\"promptVersion\":\"v1\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT")));
        final long perspectiveId = draft.get("id").asLong();

        get("/api/v1/questions/q032/reading", user).andExpect(jsonPath("$.perspectives[*].label", not(hasItem("철학"))));
        get("/api/v1/admin/perspectives", admin).andExpect(jsonPath("$[*].id", hasItem((int) perspectiveId)));

        patchJson("/api/v1/admin/perspectives/" + perspectiveId + "/review", admin, "{\"status\":\"DRAFT\"}")
                .andExpect(status().isBadRequest());
        patchJson("/api/v1/admin/perspectives/" + perspectiveId + "/review", admin, "{\"status\":\"APPROVED\"}")
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").isNumber());
        get("/api/v1/questions/q032/reading", user).andExpect(jsonPath("$.perspectives[*].label", hasItem("철학")));

        //고치면 다시 검수 대기
        putJson("/api/v1/admin/perspectives/" + perspectiveId, admin, "{\"label\":\"철학\",\"body\":\"고친 해설\"}")
                .andExpect(jsonPath("$.status").value("DRAFT"));
        get("/api/v1/questions/q032/reading", user).andExpect(jsonPath("$.perspectives[*].label", not(hasItem("철학"))));
    }

    @Test
    void 주제_검증일을_지정한다() throws Exception {
        final String admin = login("admin", "admin1234");
        patchJson("/api/v1/admin/topics/p005/checked", admin, "{\"checkedOn\":\"2026-11-05\"}")
                .andExpect(jsonPath("$.checkedOn").value("2026-11-05"));
        patchJson("/api/v1/admin/topics/p005/checked", admin, "{\"checkedOn\":null}")
                .andExpect(jsonPath("$.checkedOn").doesNotExist());
    }
}
