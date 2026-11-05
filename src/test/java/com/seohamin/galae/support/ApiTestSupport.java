package com.seohamin.galae.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 통합 테스트 공통 도구
 * 테스트마다 새 아이디로 가입해서 서로 데이터가 섞이지 않게 한다
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper jsonMapper;

    /** 새 유저로 가입하고 액세스 토큰을 돌려준다 */
    protected String signup(final String name) throws Exception {
        return signupUser(name).token();
    }

    /** 새 유저로 가입하고 토큰·ID·아이디를 돌려준다 */
    protected TestUser signupUser(final String name) throws Exception {
        final String handle = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        final JsonNode body = json(perform(post("/api/v1/auth/signup"), null,
                "{\"handle\":\"" + handle + "\",\"password\":\"password1\",\"name\":\"" + name + "\"}")
                .andExpect(status().isCreated()));
        return new TestUser(body.get("accessToken").asString(), body.get("userId").asLong(), handle);
    }

    protected record TestUser(String token, Long id, String handle) {}

    protected String login(final String handle, final String password) throws Exception {
        final JsonNode body = json(perform(post("/api/v1/auth/login"), null,
                "{\"handle\":\"" + handle + "\",\"password\":\"" + password + "\"}")
                .andExpect(status().isOk()));
        return body.get("accessToken").asString();
    }

    protected ResultActions get(final String url, final String token) throws Exception {
        return perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), token, null);
    }

    protected ResultActions postJson(final String url, final String token, final String body) throws Exception {
        return perform(post(url), token, body);
    }

    protected ResultActions putJson(final String url, final String token, final String body) throws Exception {
        return perform(put(url), token, body);
    }

    protected ResultActions patchJson(final String url, final String token, final String body) throws Exception {
        return perform(patch(url), token, body);
    }

    protected ResultActions delete(final String url, final String token) throws Exception {
        return perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url), token, null);
    }

    protected JsonNode json(final ResultActions actions) throws Exception {
        return jsonMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private ResultActions perform(
            final MockHttpServletRequestBuilder builder,
            final String token,
            final String body
    ) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }
}
