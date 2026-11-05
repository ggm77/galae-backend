package com.seohamin.galae.domain.question;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 질문 이미지. 테스트의 오늘은 2026-09-25
 */
class QuestionImageApiTest extends ApiTestSupport {

    //PNG 시그니처 + 아무 바이트
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    private static String dataUrl(final String type, final byte[] bytes) {
        return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void 이미지는_공개된_질문만_토큰_없이_볼_수_있고_관리자는_미리볼_수_있다() throws Exception {
        final String admin = login("admin", "admin1234");
        final String user = signup("이미지");

        postJson("/api/v1/admin/questions", admin, """
                {"id":"t-img","date":"2026-07-15","title":"이미지 질문","body":"그림이 있는 질문입니다.",
                 "choices":[{"text":"하나","shortText":"하나"},{"text":"둘","shortText":"둘"}],
                 "image":"%s"}
                """.formatted(dataUrl("image/png", PNG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/v1/admin/questions/t-img/image?v=")));

        //초안이라 사용자에게는 없다
        get("/api/v1/questions/t-img/image", null).andExpect(status().isNotFound());

        //관리자는 공개 전에도 본다
        final byte[] preview = get("/api/v1/admin/questions/t-img/image", admin)
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(preview).isEqualTo(PNG);
        get("/api/v1/admin/questions/t-img/image", user).andExpect(status().isForbidden());

        //공개하면 목록·상세에 imageUrl이 붙고, 토큰 없이 받을 수 있다
        patchJson("/api/v1/admin/questions/t-img", admin, "{\"status\":\"PUBLISHED\"}").andExpect(status().isOk());
        final String imageUrl = json(get("/api/v1/questions/t-img", user)
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/v1/questions/t-img/image?v="))))
                .get("imageUrl").asString();
        get("/api/v1/questions?query=그림이 있는", user).andExpect(jsonPath("$[0].imageUrl").value(imageUrl));

        final byte[] image = get(imageUrl, null)
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "max-age=2592000, public"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(image).isEqualTo(PNG);

        //빈 문자열이면 삭제
        patchJson("/api/v1/admin/questions/t-img", admin, "{\"image\":\"\"}")
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
        get("/api/v1/questions/t-img", user).andExpect(jsonPath("$.imageUrl").value(nullValue()));
        get("/api/v1/questions/t-img/image", null).andExpect(status().isNotFound());
    }

    @Test
    void 형식과_크기가_맞지_않는_이미지는_거절한다() throws Exception {
        final String admin = login("admin", "admin1234");
        final String url = "/api/v1/admin/questions/q033";

        //GIF는 받지 않는다
        patchJson(url, admin, "{\"image\":\"" + dataUrl("image/gif", new byte[]{'G', 'I', 'F', '8'}) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHOTO"));

        //PNG라고 했지만 내용은 JPEG
        patchJson(url, admin, "{\"image\":\"" + dataUrl("image/png", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHOTO"));

        //1MB 초과
        final byte[] big = Arrays.copyOf(PNG, 1024 * 1024 + 1);
        patchJson(url, admin, "{\"image\":\"" + dataUrl("image/png", big) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOO_BIG_PHOTO"));

        //거절되면 질문은 그대로
        get("/api/v1/admin/questions", admin)
                .andExpect(jsonPath("$[?(@.id == 'q033')].imageUrl").value(org.hamcrest.Matchers.contains(nullValue())));
    }
}
