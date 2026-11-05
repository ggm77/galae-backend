package com.seohamin.galae.domain.answer;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 테스트의 오늘은 2026-09-25 (오늘의 질문 q037)
 */
class AnswerApiTest extends ApiTestSupport {

    @Test
    void 답하기_전에는_결과와_의견과_확장하기가_잠겨_있다() throws Exception {
        final String token = signup("잠금");

        get("/api/v1/questions/today", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("q037"))
                .andExpect(jsonPath("$.choices", hasSize(3)))
                .andExpect(jsonPath("$.myAnswer").doesNotExist());

        get("/api/v1/questions/q037/result", token).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ANSWER_REQUIRED"));
        get("/api/v1/questions/q037/choices/1/opinions", token).andExpect(status().isForbidden());
        get("/api/v1/questions/q037/reading", token).andExpect(status().isForbidden());
    }

    @Test
    void 답한_뒤_결과와_다른_사람의_이유를_본다() throws Exception {
        final String me = signup("나");
        final String other = signup("다른");

        putJson("/api/v1/questions/q036/answer", other, "{\"choiceId\":0,\"reason\":\"먼저 사과하고 싶어요\"}")
                .andExpect(status().isOk());
        putJson("/api/v1/questions/q036/answer", me, "{\"choiceId\":0,\"reason\":\"내 이유\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answeredOn").value("2026-09-25"))
                .andExpect(jsonPath("$.completed").value(false));

        get("/api/v1/questions/q036/result", me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myChoiceId").value(0))
                .andExpect(jsonPath("$.choices[0].pct").value(100))
                .andExpect(jsonPath("$.choices[1].pct").value(0));

        //내 이유는 맨 위(myReason), 다른 사람의 이유는 익명 목록
        get("/api/v1/questions/q036/choices/0/opinions", me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myReason").value("내 이유"))
                .andExpect(jsonPath("$.opinions[0]").value("먼저 사과하고 싶어요"));

        get("/api/v1/questions/q036/reading", me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perspectives[0].label").value("성경"));
    }

    @Test
    void 다시_하기는_선택과_이유만_바꾸고_답한_날과_완료는_그대로다() throws Exception {
        final String token = signup("다시");

        putJson("/api/v1/questions/q037/answer", token, "{\"choiceId\":1}").andExpect(status().isOk());
        postJson("/api/v1/questions/q037/answer/complete", token, null)
                .andExpect(jsonPath("$.completed").value(true));

        putJson("/api/v1/questions/q037/answer", token, "{\"choiceId\":2,\"reason\":\"바꿨어요\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choiceId").value(2))
                .andExpect(jsonPath("$.reason").value("바꿨어요"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.answeredOn").value("2026-09-25"));
    }

    @Test
    void 없는_선택지와_공개_전_질문은_거절한다() throws Exception {
        final String token = signup("거절");

        putJson("/api/v1/questions/q037/answer", token, "{\"choiceId\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHOICE_NOT_EXIST"));
        putJson("/api/v1/questions/q999/answer", token, "{\"choiceId\":0}")
                .andExpect(status().isNotFound());
        putJson("/api/v1/questions/q037/answer", token, "{\"choiceId\":0,\"reason\":\"" + "가".repeat(501) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void 질문_목록을_답변_상태로_거르고_달력에서_답한_날을_본다() throws Exception {
        final String token = signup("목록");
        putJson("/api/v1/questions/q033/answer", token, "{\"choiceId\":1}").andExpect(status().isOk());

        //다른 테스트가 질문을 더할 수 있어서 전체 개수는 고정하지 않는다
        final int total = json(get("/api/v1/questions", token)).size();
        get("/api/v1/questions?filter=answered", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("q033"))
                .andExpect(jsonPath("$[0].myAnswer.choiceId").value(1));
        get("/api/v1/questions?filter=unanswered", token).andExpect(jsonPath("$", hasSize(total - 1)));
        get("/api/v1/questions?query=다툰", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("q036"));

        get("/api/v1/users/me/answers?from=2026-09-01&to=2026-09-30", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].answeredOn").value("2026-09-25"))
                .andExpect(jsonPath("$[0].questionDate").value("2026-09-21"));
    }
}
