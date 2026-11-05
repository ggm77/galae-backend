package com.seohamin.galae.domain.admin;

import com.seohamin.galae.global.infra.ai.AiCompletion;
import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 해설 생성. AiClient는 목이라 실제 비즈라우터를 부르지 않는다
 * q035 선택지는 3개 (A, B, C)
 */
class AiCommentaryApiTest extends ApiTestSupport {

    private static final String REQUEST = """
            {"label":"성경","valueTags":["책임","배려"],
             "sources":[{"ref":"갈라디아서 6:2","text":"너희가 짐을 서로 지라 그리하여 그리스도의 법을 성취하라","note":"공동체의 짐 나누기"},
                        {"ref":"갈라디아서 6:5","text":"각각 자기의 짐을 질 것이라"}]}
            """;

    private static final String GOOD = """
            {"tradition":"성경","values_in_tension":["책임","배려"],
             "for_each_option":[{"option":"A","strongest_case":"함께 애쓴 사람들의 시간을 존중한다"},
                                {"option":"B","strongest_case":"사정을 듣고 함께 마무리할 길을 찾는다"},
                                {"option":"C","strongest_case":"어려운 때 서로의 짐을 나누어 진다"}],
             "body":"성경은 서로의 짐을 나누는 일과 각자 자기 몫을 감당하는 일을 함께 이야기합니다.",
             "sources":["갈라디아서 6:2"],
             "open_question":"내가 나누어 질 수 있는 짐은 어디까지일까요?"}
            """;

    @Test
    void 검증을_통과하면_DRAFT로_저장되고_구절은_입력_원문_그대로_붙는다() throws Exception {
        final String admin = login("admin", "admin1234");
        when(aiClient.isConfigured()).thenReturn(true);
        when(aiClient.defaultModel()).thenReturn("test/model");
        //초안은 코드 블록에 감싸 오고, 자기비평 결과는 JSON만 온다
        when(aiClient.complete(any(), anyString(), anyString(), anyDouble()))
                .thenReturn(new AiCompletion("```json\n" + GOOD + "\n```", "test/model", 0.1))
                .thenReturn(new AiCompletion(GOOD, "test/model", 0.2));

        final JsonNode created = json(postJson("/api/v1/admin/questions/q035/perspectives/generate", admin, REQUEST)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.label").value("성경"))
                .andExpect(jsonPath("$.ref").value("갈라디아서 6:2"))
                .andExpect(jsonPath("$.verse").value("너희가 짐을 서로 지라 그리하여 그리스도의 법을 성취하라"))
                .andExpect(jsonPath("$.openQuestion").value("내가 나누어 질 수 있는 짐은 어디까지일까요?"))
                .andExpect(jsonPath("$.model").value("test/model"))
                .andExpect(jsonPath("$.promptVersion").value("commentary-v1")));

        final JsonNode detail = jsonMapper.readTree(created.get("generationDetail").asString());
        assertThat(detail.get("errors").size()).isZero();
        assertThat(detail.get("forEachOption").size()).isEqualTo(3);
        assertThat(detail.get("cost").asDouble()).isEqualTo(0.3);

        //생성(1) + 자기비평(1) = 2번
        verify(aiClient, times(2)).complete(any(), anyString(), anyString(), anyDouble());

        //승인 전에는 확장하기에 보이지 않는다
        final String reader = signup("독자");
        putJson("/api/v1/questions/q035/answer", reader, "{\"choiceId\":1}").andExpect(status().isOk());
        get("/api/v1/questions/q035/reading", reader)
                .andExpect(jsonPath("$.perspectives[*].openQuestion", not(hasItem("내가 나누어 질 수 있는 짐은 어디까지일까요?"))));

        patchJson("/api/v1/admin/perspectives/" + created.get("id").asLong() + "/review", admin, "{\"status\":\"APPROVED\"}")
                .andExpect(status().isOk());
        get("/api/v1/questions/q035/reading", reader)
                .andExpect(jsonPath("$.perspectives[*].openQuestion", hasItem("내가 나누어 질 수 있는 짐은 어디까지일까요?")));
    }

    @Test
    void 두_번_모두_검증에_실패하면_FAILED로_사유와_함께_저장된다() throws Exception {
        final String admin = login("admin", "admin1234");
        //금지 표현, 빠진 선택지, 목록 밖 출처, 필수 가치 누락
        final String bad = """
                {"values_in_tension":["책임"],
                 "for_each_option":[{"option":"A","strongest_case":"이것이 정답이다"}],
                 "body":"모른 척하는 것은 잘못된 선택입니다.",
                 "sources":["요한복음 3:16"],
                 "open_question":"어떻게 할까요?"}
                """;
        when(aiClient.isConfigured()).thenReturn(true);
        when(aiClient.defaultModel()).thenReturn("test/model");
        when(aiClient.complete(any(), anyString(), anyString(), anyDouble()))
                .thenReturn(new AiCompletion(bad, "test/model", 0.1));

        final JsonNode created = json(postJson("/api/v1/admin/questions/q035/perspectives/generate", admin, REQUEST)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.verse").value(nullValue())));

        final String errors = jsonMapper.readTree(created.get("generationDetail").asString()).get("errors").toString();
        assertThat(errors)
                .contains("필수 가치 누락: 배려")
                .contains("for_each_option에 선택지 B 없음")
                .contains("출처 목록 밖 인용: 요한복음 3:16")
                .contains("금지 표현 (body): 잘못된 선택")
                .contains("금지 표현 (for_each_option A): 정답");

        //(생성 + 자기비평) × 2번
        verify(aiClient, times(4)).complete(any(), anyString(), anyString(), anyDouble());
        //두 번째 생성 요청에는 실패 사유가 붙는다
        verify(aiClient, atLeastOnce()).complete(any(), anyString(), contains("자동 검증에서 다음 이유로 실패했다"), anyDouble());

        //FAILED는 검수 큐에서 볼 수 있다
        get("/api/v1/admin/perspectives?status=FAILED", admin)
                .andExpect(jsonPath("$[*].id", hasItem(created.get("id").asInt())));
    }

    @Test
    void 키가_없거나_관리자가_아니면_생성할_수_없다() throws Exception {
        final String admin = login("admin", "admin1234");
        when(aiClient.isConfigured()).thenReturn(false);

        get("/api/v1/admin/ai", admin).andExpect(jsonPath("$.configured").value(false));
        postJson("/api/v1/admin/questions/q035/perspectives/generate", admin, REQUEST)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_NOT_CONFIGURED"));

        final String user = signup("일반");
        postJson("/api/v1/admin/questions/q035/perspectives/generate", user, REQUEST)
                .andExpect(status().isForbidden());

        //필수 가치는 2개 이상
        when(aiClient.isConfigured()).thenReturn(true);
        postJson("/api/v1/admin/questions/q035/perspectives/generate", admin, "{\"label\":\"성경\",\"valueTags\":[\"책임\"]}")
                .andExpect(status().isBadRequest());

        verify(aiClient, never()).complete(any(), anyString(), anyString(), anyDouble());
    }
}
