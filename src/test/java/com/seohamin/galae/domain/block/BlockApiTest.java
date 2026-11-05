package com.seohamin.galae.domain.block;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 차단은 한쪽 방향: 나(me)의 응답에서만 차단한 사람(blocked)의 글이 빠지고, 제3자(other)에게는 그대로 보인다
 * 테스트의 오늘은 2026-09-25 (오늘의 질문 q037)
 */
class BlockApiTest extends ApiTestSupport {

    private boolean containsId(final JsonNode comments, final long id) {
        for (final JsonNode c : comments) {
            if (c.get("id").asLong() == id) return true;
            for (final JsonNode r : c.get("replies")) {
                if (r.get("id").asLong() == id) return true;
            }
        }
        return false;
    }

    @Test
    void 차단한_사람의_댓글_그룹_메시지_익명_의견이_내_응답에서만_빠진다() throws Exception {
        final TestUser me = signupUser("나");
        final TestUser blocked = signupUser("차단될");
        final TestUser other = signupUser("제3자");

        // 같은 그룹
        final long groupId = json(postJson("/api/v1/groups", me.token(), "{\"name\":\"차단 모임\",\"color\":\"blue\"}")).get("id").asLong();
        final String code = json(get("/api/v1/groups/" + groupId, me.token())).get("inviteCode").asString();
        postJson("/api/v1/groups/join", blocked.token(), "{\"code\":\"" + code + "\"}").andExpect(status().isOk());
        postJson("/api/v1/groups/join", other.token(), "{\"code\":\"" + code + "\"}").andExpect(status().isOk());

        // 셋 다 q037에 같은 선택지로 답하고, 차단될 사람이 나눔·메시지·댓글을 남긴다
        putJson("/api/v1/questions/q037/answer", me.token(), "{\"choiceId\":2,\"reason\":\"내 이유\"}").andExpect(status().isOk());
        putJson("/api/v1/questions/q037/answer", other.token(), "{\"choiceId\":2,\"reason\":\"제3자의 이유\"}").andExpect(status().isOk());
        putJson("/api/v1/questions/q037/answer", blocked.token(), "{\"choiceId\":2,\"reason\":\"차단될 사람의 이유\"}").andExpect(status().isOk());
        postJson("/api/v1/questions/q037/share", blocked.token(), "{\"groupIds\":[" + groupId + "]}").andExpect(status().isOk());
        final String messages = "/api/v1/groups/" + groupId + "/questions/q037/messages";
        postJson(messages, other.token(), "{\"text\":\"제3자 메시지\"}").andExpect(status().isCreated());
        postJson(messages, blocked.token(), "{\"text\":\"차단될 사람의 메시지\"}").andExpect(status().isCreated());

        final long blockedComment = json(postJson("/api/v1/feed/topics/p005/comments", blocked.token(), "{\"text\":\"차단될 댓글\"}")).get("id").asLong();
        final long replyUnderBlocked = json(postJson("/api/v1/feed/topics/p005/comments", other.token(),
                "{\"text\":\"차단될 댓글에 단 답글\",\"parentId\":" + blockedComment + "}")).get("id").asLong();
        final long otherComment = json(postJson("/api/v1/feed/topics/p005/comments", other.token(), "{\"text\":\"제3자 댓글\"}")).get("id").asLong();
        final long blockedReply = json(postJson("/api/v1/feed/topics/p005/comments", blocked.token(),
                "{\"text\":\"차단될 사람의 답글\",\"parentId\":" + otherComment + "}")).get("id").asLong();

        // 차단
        putJson("/api/v1/users/me/blocks/" + blocked.id(), me.token(), null).andExpect(status().isNoContent());
        get("/api/v1/users/me/blocks", me.token())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user.id").value(blocked.id()));

        // 댓글: 차단한 사람의 원 댓글은 그 아래 답글과 함께, 답글은 그것만 빠진다
        final JsonNode myView = json(get("/api/v1/feed/topics/p005/comments", me.token()));
        assertThat(containsId(myView, blockedComment)).isFalse();
        assertThat(containsId(myView, replyUnderBlocked)).isFalse();
        assertThat(containsId(myView, blockedReply)).isFalse();
        assertThat(containsId(myView, otherComment)).isTrue();
        final JsonNode otherView = json(get("/api/v1/feed/topics/p005/comments", other.token()));
        assertThat(containsId(otherView, blockedComment)).isTrue();
        assertThat(containsId(otherView, blockedReply)).isTrue();

        // 그룹 대화: 차단한 사람의 나눔·메시지가 빠지고, 커서는 걸러진 메시지까지 넘어간다
        final JsonNode myMessages = json(get(messages, me.token()));
        assertThat(myMessages.get("messages").toString()).doesNotContain("차단될 사람");
        assertThat(myMessages.get("messages").size()).isEqualTo(1);
        final JsonNode otherMessages = json(get(messages, other.token()));
        assertThat(otherMessages.get("messages").size()).isEqualTo(3);
        assertThat(myMessages.get("lastMessageId").asLong()).isEqualTo(otherMessages.get("lastMessageId").asLong());
        get(messages + "?after=" + myMessages.get("lastMessageId").asLong(), me.token())
                .andExpect(jsonPath("$.messages", hasSize(0)));

        // 그룹: 멤버 목록에는 남되 blocked 표시, 오늘 나눈 멤버에서는 빠진다
        get("/api/v1/groups/" + groupId, me.token())
                .andExpect(jsonPath("$.members[?(@.user.id == " + blocked.id() + ")].blocked").value(contains(true)))
                .andExpect(jsonPath("$.members[?(@.user.id == " + other.id() + ")].blocked").value(contains(false)));
        get("/api/v1/groups", me.token())
                .andExpect(jsonPath("$[?(@.id == " + groupId + ")].todaySharedMembers[*]", hasSize(0)));
        get("/api/v1/groups", other.token())
                .andExpect(jsonPath("$[?(@.id == " + groupId + ")].todaySharedMembers[*]", hasSize(1)));

        // 익명 의견: 차단한 사람의 이유가 빠진다
        get("/api/v1/questions/q037/choices/2/opinions?size=50", me.token())
                .andExpect(jsonPath("$.opinions", hasItem("제3자의 이유")))
                .andExpect(jsonPath("$.opinions", not(hasItem("차단될 사람의 이유"))));
        get("/api/v1/questions/q037/choices/2/opinions?size=50", other.token())
                .andExpect(jsonPath("$.opinions", hasItem("차단될 사람의 이유")));

        // 프로필에 차단 여부
        get("/api/v1/users/" + blocked.id(), me.token()).andExpect(jsonPath("$.blocked").value(true));
        get("/api/v1/users/" + blocked.id(), other.token()).andExpect(jsonPath("$.blocked").value(false));
        get("/api/v1/users/me", me.token()).andExpect(jsonPath("$.blocked").value(nullValue()));

        // 해제하면 다시 보인다
        delete("/api/v1/users/me/blocks/" + blocked.id(), me.token()).andExpect(status().isNoContent());
        assertThat(containsId(json(get("/api/v1/feed/topics/p005/comments", me.token())), blockedComment)).isTrue();
        assertThat(json(get(messages, me.token())).get("messages").size()).isEqualTo(3);
    }

    @Test
    void 차단_목록을_통째로_받아_교체한다() throws Exception {
        final TestUser me = signupUser("목록");
        final TestUser a = signupUser("가");
        final TestUser b = signupUser("나");
        final TestUser c = signupUser("다");

        putJson("/api/v1/users/me/blocks", me.token(), "{\"userIds\":[" + a.id() + "," + b.id() + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].user.id", containsInAnyOrder(a.id().intValue(), b.id().intValue())));

        //b는 남기고 a는 풀고 c를 더한다 (중복은 한 번만)
        putJson("/api/v1/users/me/blocks", me.token(), "{\"userIds\":[" + b.id() + "," + c.id() + "," + c.id() + "]}")
                .andExpect(jsonPath("$[*].user.id", containsInAnyOrder(b.id().intValue(), c.id().intValue())));

        //빈 배열이면 모두 해제
        putJson("/api/v1/users/me/blocks", me.token(), "{\"userIds\":[]}").andExpect(jsonPath("$", hasSize(0)));

        //자기 자신, 없는 유저는 거절하고 목록은 그대로
        putJson("/api/v1/users/me/blocks", me.token(), "{\"userIds\":[" + a.id() + "]}").andExpect(status().isOk());
        putJson("/api/v1/users/me/blocks", me.token(), "{\"userIds\":[" + me.id() + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_BLOCK_SELF"));
        putJson("/api/v1/users/me/blocks", me.token(), "{\"userIds\":[" + a.id() + ",99999999]}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_EXIST"));
        get("/api/v1/users/me/blocks", me.token()).andExpect(jsonPath("$[*].user.id", contains(a.id().intValue())));

        //한 명 차단은 여러 번 해도 한 번
        putJson("/api/v1/users/me/blocks/" + b.id(), me.token(), null).andExpect(status().isNoContent());
        putJson("/api/v1/users/me/blocks/" + b.id(), me.token(), null).andExpect(status().isNoContent());
        get("/api/v1/users/me/blocks", me.token()).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void 탈퇴하면_차단_기록도_사라진다() throws Exception {
        final TestUser me = signupUser("남는");
        final TestUser leaving = signupUser("떠나는");
        putJson("/api/v1/users/me/blocks/" + leaving.id(), me.token(), null).andExpect(status().isNoContent());
        putJson("/api/v1/users/me/blocks/" + me.id(), leaving.token(), null).andExpect(status().isNoContent());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + leaving.token())
                        .contentType("application/json").content("{\"password\":\"password1\"}"))
                .andExpect(status().isNoContent());

        get("/api/v1/users/me/blocks", me.token()).andExpect(jsonPath("$", hasSize(0)));
    }
}
