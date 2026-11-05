package com.seohamin.galae.domain.comment;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 테스트 설정은 include-unchecked=true라서 검증 전 주제도 보인다
 */
class FeedCommentApiTest extends ApiTestSupport {

    @Test
    void 오늘의_피드는_다섯_주제까지다() throws Exception {
        final String token = signup("피드");

        get("/api/v1/feed", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-09-25"))
                .andExpect(jsonPath("$.topics.length()").value(lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.topics[0].cards").isArray())
                .andExpect(jsonPath("$.topics[0].sources[0].label").isNotEmpty());

        get("/api/v1/feed/topics/p001", token)
                .andExpect(jsonPath("$.title").value("테세우스의 배"));
        get("/api/v1/feed/topics/nope", token).andExpect(status().isNotFound());
    }

    @Test
    void 댓글에_답글을_달고_공감한다() throws Exception {
        final String me = signup("나");
        final String other = signup("다른");

        final JsonNode comment = json(postJson("/api/v1/feed/topics/p002/comments", other, "{\"text\":\"무거운 질문이에요\"}")
                .andExpect(status().isCreated()));
        final long commentId = comment.get("id").asLong();

        final JsonNode reply = json(postJson("/api/v1/feed/topics/p002/comments", me,
                "{\"text\":\"저도요\",\"parentId\":" + commentId + "}").andExpect(status().isCreated()));

        //답글에 답하면 원 댓글의 답글이 된다
        postJson("/api/v1/feed/topics/p002/comments", other,
                "{\"text\":\"그렇죠\",\"parentId\":" + reply.get("id").asLong() + "}")
                .andExpect(jsonPath("$.parentId").value(commentId));

        //다른 주제의 댓글에는 답글을 달 수 없다
        postJson("/api/v1/feed/topics/p001/comments", me, "{\"text\":\"x\",\"parentId\":" + commentId + "}")
                .andExpect(status().isNotFound());

        //공감은 여러 번 눌러도 한 번
        putJson("/api/v1/comments/" + commentId + "/like", me, null).andExpect(jsonPath("$.likeCount").value(1));
        putJson("/api/v1/comments/" + commentId + "/like", me, null).andExpect(jsonPath("$.likeCount").value(1));

        get("/api/v1/feed/topics/p002/comments", me)
                .andExpect(jsonPath("$[?(@.id == " + commentId + ")].liked").value(true))
                .andExpect(jsonPath("$[?(@.id == " + commentId + ")].replies[*]", hasSize(2)));

        delete("/api/v1/comments/" + commentId + "/like", me)
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));

        get("/api/v1/users/me/comments", me)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].topicTitle").value("트롤리 문제"))
                .andExpect(jsonPath("$[0].parentId").value(commentId));
    }
}
