package com.seohamin.galae.domain.feed;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 생각 탭 주제 이미지. 테스트 설정은 include-unchecked=true라 시드 주제가 모두 보인다
 */
class TopicImageApiTest extends ApiTestSupport {

    //WebP 시그니처 (RIFF....WEBP) + 아무 바이트
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 1, 2};

    private static String dataUrl(final String type, final byte[] bytes) {
        return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void 관리자가_주제에_이미지를_넣으면_피드에_imageUrl이_붙고_토큰_없이_받는다() throws Exception {
        final String admin = login("admin", "admin1234");
        final String user = signup("피드 이미지");

        patchJson("/api/v1/admin/topics/p003", admin, "{\"image\":\"" + dataUrl("image/webp", WEBP) + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/v1/admin/topics/p003/image?v=")));

        //관리자 미리보기
        final byte[] preview = get("/api/v1/admin/topics/p003/image", admin)
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(preview).isEqualTo(WEBP);
        get("/api/v1/admin/topics/p003/image", user).andExpect(status().isForbidden());
        get("/api/v1/admin/topics", admin)
                .andExpect(jsonPath("$[?(@.id == 'p003')].imageUrl").value(contains(startsWith("/api/v1/admin/topics/p003/image"))));

        //사용자: 주제·피드에 공개 경로, 토큰 없이 받는다
        final String imageUrl = json(get("/api/v1/feed/topics/p003", user)
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/api/v1/feed/topics/p003/image?v="))))
                .get("imageUrl").asString();
        //다른 테스트가 주제를 더하면 오늘의 5개가 바뀌므로, 피드에 p003이 있을 때만 확인한다
        get("/api/v1/feed", user)
                .andExpect(jsonPath("$.topics[?(@.id == 'p003')].imageUrl").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(imageUrl))));

        final byte[] image = get(imageUrl, null)
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(header().string("Cache-Control", "max-age=2592000, public"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(image).isEqualTo(WEBP);

        //빈 문자열이면 삭제, image를 빼면 그대로
        patchJson("/api/v1/admin/topics/p003", admin, "{}").andExpect(jsonPath("$.imageUrl").value(startsWith("/api/v1/admin/topics/p003/image")));
        patchJson("/api/v1/admin/topics/p003", admin, "{\"image\":\"\"}").andExpect(jsonPath("$.imageUrl").value(nullValue()));
        get("/api/v1/feed/topics/p003", user).andExpect(jsonPath("$.imageUrl").value(nullValue()));
        get("/api/v1/feed/topics/p003/image", null).andExpect(status().isNotFound());
    }

    @Test
    void 없는_주제와_잘못된_이미지는_거절한다() throws Exception {
        final String admin = login("admin", "admin1234");

        get("/api/v1/feed/topics/nope/image", null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TOPIC_NOT_EXIST"));
        patchJson("/api/v1/admin/topics/nope", admin, "{\"image\":\"\"}").andExpect(status().isNotFound());

        patchJson("/api/v1/admin/topics/p004", admin, "{\"image\":\"" + dataUrl("image/webp", new byte[]{1, 2, 3}) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHOTO"));
        get("/api/v1/feed/topics/p004", admin).andExpect(jsonPath("$.imageUrl").value(nullValue()));
    }
}
