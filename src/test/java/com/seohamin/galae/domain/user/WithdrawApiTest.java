package com.seohamin.galae.domain.user;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WithdrawApiTest extends ApiTestSupport {

    private long createGroup(final String token, final String name) throws Exception {
        return json(postJson("/api/v1/groups", token, "{\"name\":\"" + name + "\",\"color\":\"sage\"}")
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private void join(final String token, final String ownerToken, final long groupId) throws Exception {
        final String code = json(get("/api/v1/groups/" + groupId, ownerToken)).get("inviteCode").asString();
        postJson("/api/v1/groups/join", token, "{\"code\":\"" + code + "\"}").andExpect(status().isOk());
    }

    private JsonNode findComment(final JsonNode comments, final long id) {
        for (final JsonNode c : comments) {
            if (c.get("id").asLong() == id) return c;
            for (final JsonNode r : c.get("replies")) {
                if (r.get("id").asLong() == id) return r;
            }
        }
        return null;
    }

    @Test
    void 탈퇴하면_내_데이터는_지워지고_다른_사람의_글과_공감_수는_맞게_남는다() throws Exception {
        final TestUser me = signupUser("탈퇴");
        final TestUser other = signupUser("남는");

        // 그룹: 남이 만든 그룹에 가입, 내가 만든 그룹에 남이 가입
        final long othersGroup = createGroup(other.token(), "남의 모임");
        join(me.token(), other.token(), othersGroup);
        final long myGroup = createGroup(me.token(), "나의 모임");
        join(other.token(), me.token(), myGroup);

        // 답하고 두 그룹에 나눔
        final long participantsBefore = json(get("/api/v1/questions/q036", other.token())).get("participantCount").asLong();
        putJson("/api/v1/questions/q036/answer", me.token(), "{\"choiceId\":0,\"reason\":\"탈퇴할 사람의 이유\"}").andExpect(status().isOk());
        postJson("/api/v1/questions/q036/share", me.token(), "{\"groupIds\":[" + othersGroup + "," + myGroup + "]}").andExpect(status().isOk());

        // 댓글: 내 댓글에 남이 답글·공감, 남의 댓글에 내가 답글·공감
        final long myComment = json(postJson("/api/v1/feed/topics/p004/comments", me.token(), "{\"text\":\"내 댓글\"}")).get("id").asLong();
        final long othersReply = json(postJson("/api/v1/feed/topics/p004/comments", other.token(),
                "{\"text\":\"남의 답글\",\"parentId\":" + myComment + "}")).get("id").asLong();
        final long othersComment = json(postJson("/api/v1/feed/topics/p004/comments", other.token(), "{\"text\":\"남의 댓글\"}")).get("id").asLong();
        final long myReply = json(postJson("/api/v1/feed/topics/p004/comments", me.token(),
                "{\"text\":\"내 답글\",\"parentId\":" + othersComment + "}")).get("id").asLong();
        putJson("/api/v1/comments/" + myComment + "/like", other.token(), null).andExpect(status().isOk());
        putJson("/api/v1/comments/" + othersComment + "/like", me.token(), null)
                .andExpect(jsonPath("$.likeCount").value(1));

        // 비밀번호가 틀리면 탈퇴되지 않는다
        delete("/api/v1/users/me", me.token()).andExpect(status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + me.token())
                        .contentType("application/json").content("{\"password\":\"wrong-pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));

        // 탈퇴
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + me.token())
                        .contentType("application/json").content("{\"password\":\"password1\"}"))
                .andExpect(status().isNoContent());

        // 토큰은 바로 쓸 수 없고, 같은 아이디로 로그인할 수 없다
        get("/api/v1/users/me", me.token()).andExpect(status().isUnauthorized());
        postJson("/api/v1/auth/login", null, "{\"handle\":\"" + me.handle() + "\",\"password\":\"password1\"}")
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        get("/api/v1/users/" + me.id(), other.token()).andExpect(status().isNotFound());

        // 내가 그룹장인 그룹은 사라지고, 남의 그룹에서는 나와 내 메시지가 빠진다
        get("/api/v1/groups/" + myGroup, other.token()).andExpect(status().isNotFound());
        get("/api/v1/groups/" + othersGroup, other.token()).andExpect(jsonPath("$.members", hasSize(1)));
        putJson("/api/v1/questions/q036/answer", other.token(), "{\"choiceId\":1}").andExpect(status().isOk());
        get("/api/v1/groups/" + othersGroup + "/questions/q036/messages", other.token())
                .andExpect(jsonPath("$.messages", hasSize(0)))
                .andExpect(jsonPath("$.sharedMemberCount").value(0));

        // 답변이 빠져 참여 수에 나만큼 줄고, 의견에서도 사라진다 (남이 방금 답해서 +1)
        get("/api/v1/questions/q036", other.token()).andExpect(jsonPath("$.participantCount").value(participantsBefore + 1));
        get("/api/v1/questions/q036/choices/0/opinions?size=50", other.token())
                .andExpect(jsonPath("$.opinions", not(hasItem("탈퇴할 사람의 이유"))));

        // 댓글: 내 댓글·답글은 지워지고, 남의 답글은 원 댓글로 올라가 남는다. 내가 누른 공감은 빠진다
        final JsonNode comments = json(get("/api/v1/feed/topics/p004/comments", other.token()));
        assertThat(findComment(comments, myComment)).isNull();
        assertThat(findComment(comments, myReply)).isNull();
        final JsonNode promoted = findComment(comments, othersReply);
        assertThat(promoted).isNotNull();
        assertThat(promoted.get("parentId").isNull()).isTrue();
        assertThat(findComment(comments, othersComment).get("likeCount").asInt()).isZero();
    }

    @Test
    void 관리자_계정은_탈퇴할_수_없다() throws Exception {
        final String admin = login("admin", "admin1234");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + admin)
                        .contentType("application/json").content("{\"password\":\"admin1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADMIN_CANNOT_WITHDRAW"));
        get("/api/v1/users/me", admin).andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void 탈퇴한_사람의_그룹_멤버_정보는_남지_않는다() throws Exception {
        final TestUser me = signupUser("나간");
        final String owner = signup("그룹장");
        final long groupId = createGroup(owner, "남는 모임");
        join(me.token(), owner, groupId);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + me.token())
                        .contentType("application/json").content("{\"password\":\"password1\"}"))
                .andExpect(status().isNoContent());

        get("/api/v1/groups/" + groupId, owner)
                .andExpect(jsonPath("$.members[*].user.id", not(hasItem(me.id().intValue()))))
                .andExpect(jsonPath("$.members[0].owner").value(true))
                .andExpect(jsonPath("$.photoUrl").value(nullValue()));
    }
}
