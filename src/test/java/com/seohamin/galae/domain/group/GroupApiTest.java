package com.seohamin.galae.domain.group;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 테스트의 오늘은 2026-09-25 (오늘의 질문 q037)
 */
class GroupApiTest extends ApiTestSupport {

    private long createGroup(final String token, final String name) throws Exception {
        final JsonNode group = json(postJson("/api/v1/groups", token,
                "{\"name\":\"" + name + "\",\"description\":\"소개\",\"color\":\"sage\"}")
                .andExpect(status().isCreated()));
        return group.get("id").asLong();
    }

    private String inviteCode(final String token, final long groupId) throws Exception {
        return json(get("/api/v1/groups/" + groupId, token)).get("inviteCode").asString();
    }

    @Test
    void 초대_코드로_참여하고_중복_가입과_잘못된_코드를_막는다() throws Exception {
        final String owner = signup("그룹장");
        final String member = signup("멤버");
        final long groupId = createGroup(owner, "목요일 모임");
        final String code = inviteCode(owner, groupId);

        //가입 전에는 볼 수 없다
        get("/api/v1/groups/" + groupId, member).andExpect(status().isForbidden());

        postJson("/api/v1/groups/join", member, "{\"code\":\" " + code.toLowerCase() + " \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.owner").value(false));
        postJson("/api/v1/groups/join", member, "{\"code\":\"" + code + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GROUP_ALREADY_JOINED"));
        postJson("/api/v1/groups/join", member, "{\"code\":\"ZZZZZZ\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GROUP_CODE_INVALID"));

        //그룹장만 설정을 바꿀 수 있다
        patchJson("/api/v1/groups/" + groupId, member, "{\"name\":\"바꿈\"}").andExpect(status().isForbidden());
        patchJson("/api/v1/groups/" + groupId, owner, "{\"name\":\"금요일 모임\",\"color\":\"peach\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("금요일 모임"))
                .andExpect(jsonPath("$.color").value("peach"));
    }

    @Test
    void 답하기_전에는_그룹원의_글이_잠기고_답하면_열린다() throws Exception {
        final String owner = signup("그룹장");
        final String member = signup("멤버");
        final long groupId = createGroup(owner, "청년부");
        postJson("/api/v1/groups/join", member, "{\"code\":\"" + inviteCode(owner, groupId) + "\"}").andExpect(status().isOk());

        //그룹장이 답하고 나눈다
        putJson("/api/v1/questions/q037/answer", owner, "{\"choiceId\":1,\"reason\":\"먼저 이야기할래요\"}").andExpect(status().isOk());
        postJson("/api/v1/questions/q037/share", owner, "{\"groupIds\":[" + groupId + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentGroupIds[0]").value(groupId));

        //오늘 나눈 멤버
        get("/api/v1/groups", member)
                .andExpect(jsonPath("$[0].todaySharedMembers", hasSize(1)))
                .andExpect(jsonPath("$[0].memberCount").value(2));

        //멤버는 아직 답하지 않아서 내용이 보이지 않는다
        get("/api/v1/groups/" + groupId + "/questions/q037/messages", member)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.sharedMemberCount").value(1))
                .andExpect(jsonPath("$.messages[0].share").value(true))
                .andExpect(jsonPath("$.messages[0].text").value(nullValue()))
                .andExpect(jsonPath("$.messages[0].choiceId").value(nullValue()));

        //답하면 열린다
        putJson("/api/v1/questions/q037/answer", member, "{\"choiceId\":0}").andExpect(status().isOk());
        get("/api/v1/groups/" + groupId + "/questions/q037/messages", member)
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.messages[0].text").value("먼저 이야기할래요"))
                .andExpect(jsonPath("$.messages[0].choiceId").value(1));
    }

    @Test
    void 이미_보낸_답은_다시_보내지_않고_답을_바꾸면_새로_보낼_수_있다() throws Exception {
        final String token = signup("나눔");
        final long groupId = createGroup(token, "나눔 모임");
        final String messagesUrl = "/api/v1/groups/" + groupId + "/questions/q036/messages";

        putJson("/api/v1/questions/q036/answer", token, "{\"choiceId\":0,\"reason\":\"첫 답\"}").andExpect(status().isOk());

        //초안은 가입한 그룹만 고를 수 있다
        patchJson("/api/v1/questions/q036/answer/progress", token, "{\"shareDraft\":[999999]}")
                .andExpect(status().isForbidden());
        patchJson("/api/v1/questions/q036/answer/progress", token, "{\"lastStep\":\"share\",\"shareDraft\":[" + groupId + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareDraft[0]").value(groupId))
                .andExpect(jsonPath("$.lastStep").value("share"));

        postJson("/api/v1/questions/q036/share", token, "{\"groupIds\":[" + groupId + "]}")
                .andExpect(jsonPath("$.shareDraft").value(nullValue()));
        postJson("/api/v1/questions/q036/share", token, "{\"groupIds\":[" + groupId + "]}").andExpect(status().isOk());
        get(messagesUrl, token).andExpect(jsonPath("$.messages", hasSize(1)));

        //다시 하기로 답이 바뀌면 아직 아무 그룹에도 보내지 않은 상태
        putJson("/api/v1/questions/q036/answer", token, "{\"choiceId\":1,\"reason\":\"바꾼 답\"}").andExpect(status().isOk());
        get("/api/v1/questions/q036/share", token).andExpect(jsonPath("$.sentGroupIds", hasSize(0)));

        postJson("/api/v1/questions/q036/share", token, "{\"groupIds\":[" + groupId + "]}")
                .andExpect(jsonPath("$.sentGroupIds", hasSize(1)));
        //이미 보낸 메시지는 그대로 남는다
        get(messagesUrl, token)
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].text").value("첫 답"))
                .andExpect(jsonPath("$.messages[1].text").value("바꾼 답"));
    }

    @Test
    void 나가면_내_메시지가_지워지고_그룹장이_나가면_그룹이_사라진다() throws Exception {
        final String owner = signup("그룹장");
        final TestUser member = signupUser("멤버");
        final long groupId = createGroup(owner, "사라질 모임");
        postJson("/api/v1/groups/join", member.token(), "{\"code\":\"" + inviteCode(owner, groupId) + "\"}").andExpect(status().isOk());
        final String messagesUrl = "/api/v1/groups/" + groupId + "/questions/q035/messages";

        postJson(messagesUrl, member.token(), "{\"text\":\"안녕하세요\"}").andExpect(status().isCreated());
        postJson(messagesUrl, owner, "{\"text\":\"반가워요\"}").andExpect(status().isCreated());
        postJson(messagesUrl, owner, "{\"text\":\"   \"}").andExpect(status().isBadRequest());

        //멤버는 내보내기를 할 수 없다
        delete("/api/v1/groups/" + groupId + "/members/" + member.id(), member.token()).andExpect(status().isForbidden());

        delete("/api/v1/groups/" + groupId + "/members/me", member.token()).andExpect(status().isNoContent());
        get(messagesUrl, owner)
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].text").value("반가워요"));
        get("/api/v1/groups", member.token()).andExpect(jsonPath("$", hasSize(0)));

        delete("/api/v1/groups/" + groupId + "/members/me", owner).andExpect(status().isNoContent());
        get("/api/v1/groups/" + groupId, owner).andExpect(status().isNotFound());
    }

    @Test
    void 그룹장은_멤버를_내보낼_수_있다() throws Exception {
        final String owner = signup("그룹장");
        final TestUser member = signupUser("멤버");
        final TestUser other = signupUser("다른");
        final long groupId = createGroup(owner, "내보내기");
        final String code = inviteCode(owner, groupId);
        postJson("/api/v1/groups/join", member.token(), "{\"code\":\"" + code + "\"}").andExpect(status().isOk());
        postJson("/api/v1/groups/join", other.token(), "{\"code\":\"" + code + "\"}").andExpect(status().isOk());

        //멤버끼리는 내보낼 수 없다
        delete("/api/v1/groups/" + groupId + "/members/" + other.id(), member.token()).andExpect(status().isForbidden());

        delete("/api/v1/groups/" + groupId + "/members/" + member.id(), owner).andExpect(status().isNoContent());
        get("/api/v1/groups/" + groupId, member.token()).andExpect(status().isForbidden());
        get("/api/v1/groups/" + groupId, owner).andExpect(jsonPath("$.members", hasSize(2)));
    }
}
