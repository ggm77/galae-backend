package com.seohamin.galae.domain.admin;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 생각 탭 주제 추가·수정 (관리자)
 */
class AdminTopicApiTest extends ApiTestSupport {

    private static final String NEW_TOPIC = """
            {"id":"t-river","category":"동일성","title":"강물의 비유",
             "cards":["같은 강물에 두 번 들어갈 수 있을까요?","물은 계속 흘러 바뀝니다.","그래도 우리는 같은 강이라고 부릅니다."],
             "sources":[{"label":"Wikipedia, 'Heraclitus'","url":"https://en.wikipedia.org/wiki/Heraclitus"},{"label":"플라톤, 『크라튈로스』"}]}
            """;

    @Test
    void 주제를_추가하면_보류로_들어가고_검증한_뒤_내용을_고치면_다시_보류가_된다() throws Exception {
        final String admin = login("admin", "admin1234");

        postJson("/api/v1/admin/topics", admin, NEW_TOPIC)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("t-river"))
                .andExpect(jsonPath("$.checkedOn").value(nullValue()))
                .andExpect(jsonPath("$.cards", hasSize(3)))
                .andExpect(jsonPath("$.sources[0].url").value("https://en.wikipedia.org/wiki/Heraclitus"))
                .andExpect(jsonPath("$.sources[1].url").value(nullValue()))
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));

        //같은 ID는 다시 만들 수 없다
        postJson("/api/v1/admin/topics", admin, NEW_TOPIC)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOPIC_ALREADY_EXIST"));

        get("/api/v1/admin/topics", admin).andExpect(jsonPath("$[*].id", hasItem("t-river")));

        //검증 완료
        patchJson("/api/v1/admin/topics/t-river/checked", admin, "{\"checkedOn\":\"2026-11-05\"}")
                .andExpect(jsonPath("$.checkedOn").value("2026-11-05"));

        //이미지만 바꾸면 검증일 유지
        final String png = "data:image/png;base64," + Base64.getEncoder().encodeToString(
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0});
        patchJson("/api/v1/admin/topics/t-river", admin, "{\"image\":\"" + png + "\"}")
                .andExpect(jsonPath("$.checkedOn").value("2026-11-05"))
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/v1/admin/topics/t-river/image")));

        //내용을 고치면 다시 보류. 보내지 않은 필드는 그대로
        patchJson("/api/v1/admin/topics/t-river", admin,
                "{\"title\":\"헤라클레이토스의 강\",\"cards\":[\"고친 첫 장\",\"고친 둘째 장\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("헤라클레이토스의 강"))
                .andExpect(jsonPath("$.cards", contains("고친 첫 장", "고친 둘째 장")))
                .andExpect(jsonPath("$.category").value("동일성"))
                .andExpect(jsonPath("$.sources", hasSize(2)))
                .andExpect(jsonPath("$.checkedOn").value(nullValue()))
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/v1/admin/topics/t-river/image")));

        //출처를 빈 배열로 보내면 모두 지운다
        patchJson("/api/v1/admin/topics/t-river", admin, "{\"sources\":[]}")
                .andExpect(jsonPath("$.sources", hasSize(0)));

        //사용자에게 보이는 주제 조회에도 반영된다 (테스트 설정은 보류 주제도 보임)
        final String user = signup("독자");
        get("/api/v1/feed/topics/t-river", user)
                .andExpect(jsonPath("$.title").value("헤라클레이토스의 강"))
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/v1/feed/topics/t-river/image")));
    }

    @Test
    void 잘못된_주제는_거절하고_일반_유저는_쓸_수_없다() throws Exception {
        final String admin = login("admin", "admin1234");

        //카드가 없음
        postJson("/api/v1/admin/topics", admin, "{\"id\":\"t-bad1\",\"category\":\"윤리\",\"title\":\"제목\",\"cards\":[]}")
                .andExpect(status().isBadRequest());
        //빈 카드
        postJson("/api/v1/admin/topics", admin, "{\"id\":\"t-bad2\",\"category\":\"윤리\",\"title\":\"제목\",\"cards\":[\"  \"]}")
                .andExpect(status().isBadRequest());
        //ID 형식
        postJson("/api/v1/admin/topics", admin, "{\"id\":\"Bad ID\",\"category\":\"윤리\",\"title\":\"제목\",\"cards\":[\"글\"]}")
                .andExpect(status().isBadRequest());
        //출처 주소 형식
        postJson("/api/v1/admin/topics", admin,
                "{\"id\":\"t-bad3\",\"category\":\"윤리\",\"title\":\"제목\",\"cards\":[\"글\"],\"sources\":[{\"label\":\"출처\",\"url\":\"javascript:alert(1)\"}]}")
                .andExpect(status().isBadRequest());
        //수정에서도 카드를 비울 수 없다
        patchJson("/api/v1/admin/topics/p001", admin, "{\"cards\":[]}").andExpect(status().isBadRequest());
        patchJson("/api/v1/admin/topics/nope", admin, "{\"title\":\"x\"}").andExpect(status().isNotFound());

        get("/api/v1/admin/topics", admin).andExpect(jsonPath("$[*].id", not(hasItem(startsWith("t-bad")))));

        final String user = signup("일반");
        postJson("/api/v1/admin/topics", user, "{\"id\":\"t-user\",\"category\":\"윤리\",\"title\":\"제목\",\"cards\":[\"글\"]}")
                .andExpect(status().isForbidden());
    }
}
