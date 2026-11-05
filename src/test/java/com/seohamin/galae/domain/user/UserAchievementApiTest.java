package com.seohamin.galae.domain.user;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 테스트의 오늘은 2026-09-25 (오늘의 질문 q037)
 */
class UserAchievementApiTest extends ApiTestSupport {

    @Test
    void 활동하면_열매가_맺히고_한번_맺힌_열매는_남는다() throws Exception {
        final String token = signup("열매");

        //오늘 질문에 답함 -> 첫 가지
        putJson("/api/v1/questions/q037/answer", token, "{\"choiceId\":1}").andExpect(status().isOk());
        get("/api/v1/users/me/achievements", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("first"))
                .andExpect(jsonPath("$[0].name").value("첫 가지"))
                .andExpect(jsonPath("$[0].achievedOn").value("2026-09-25"));

        //지난 질문 -> 여행자, 그룹 만들기 -> 개척자, 나눔 -> 길동무
        putJson("/api/v1/questions/q030/answer", token, "{\"choiceId\":0}").andExpect(status().isNotFound());
        putJson("/api/v1/questions/q034/answer", token, "{\"choiceId\":0}").andExpect(status().isOk());
        final long groupId = json(postJson("/api/v1/groups", token, "{\"name\":\"열매 모임\",\"color\":\"blue\"}"))
                .get("id").asLong();
        postJson("/api/v1/questions/q037/share", token, "{\"groupIds\":[" + groupId + "]}").andExpect(status().isOk());

        //댓글 -> 말동무
        final long commentId = json(postJson("/api/v1/feed/topics/p003/comments", token, "{\"text\":\"신기해요\"}"))
                .get("id").asLong();
        putJson("/api/v1/comments/" + commentId + "/like", token, null).andExpect(status().isOk());
        delete("/api/v1/comments/" + commentId + "/like", token).andExpect(status().isOk());

        get("/api/v1/users/me/achievements", token)
                .andExpect(jsonPath("$[*].id", containsInAnyOrder("first", "past", "group", "share1", "comment1")));

        get("/api/v1/users/me", token)
                .andExpect(jsonPath("$.stats.answerCount").value(2))
                .andExpect(jsonPath("$.stats.currentStreak").value(1))
                .andExpect(jsonPath("$.stats.commentCount").value(1))
                .andExpect(jsonPath("$.joinedOn").value("2026-09-25"));
    }

    @Test
    void 프로필을_편집하고_다른_사람은_가입일_없이_본다() throws Exception {
        final TestUser me = signupUser("나");
        final TestUser taken = signupUser("남");

        patchJson("/api/v1/users/me", me.token(), "{\"handle\":\"" + taken.handle() + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HANDLE_DUPLICATE"));
        patchJson("/api/v1/users/me", me.token(), "{\"color\":\"red\"}").andExpect(status().isBadRequest());
        patchJson("/api/v1/users/me", me.token(), "{\"photo\":\"data:image/png;base64,AAAA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHOTO"));

        //가장 작은 JPEG 머리(FF D8 FF)
        final String jpeg = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10});
        final JsonNode updated = json(patchJson("/api/v1/users/me", me.token(),
                "{\"name\":\"새이름\",\"bio\":\"천천히 생각해요\",\"color\":\"lilac\",\"photo\":\"" + jpeg + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("새이름"))
                .andExpect(jsonPath("$.color").value("lilac")));
        final String photoUrl = updated.get("photoUrl").asString();
        assertThat(photoUrl).startsWith("/api/v1/users/" + me.id() + "/photo?v=");

        //사진은 토큰 없이 볼 수 있다
        final byte[] photo = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(photoUrl))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(photo).hasSize(6);

        get("/api/v1/users/" + me.id(), taken.token())
                .andExpect(jsonPath("$.bio").value("천천히 생각해요"))
                .andExpect(jsonPath("$.joinedOn").value(nullValue()))
                .andExpect(jsonPath("$.role").value(nullValue()));

        //사진 삭제
        patchJson("/api/v1/users/me", me.token(), "{\"photo\":\"\"}")
                .andExpect(jsonPath("$.photoUrl").value(nullValue()));
    }
}
