package com.seohamin.galae.domain.auth;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    @Test
    void 가입한_아이디로_로그인하고_토큰을_갱신한다() throws Exception {
        postJson("/api/v1/auth/signup", null, "{\"handle\":\"auth.tester\",\"password\":\"password1\",\"name\":\"테스터\"}")
                .andExpect(status().isCreated());

        //같은 아이디는 다시 가입할 수 없다
        postJson("/api/v1/auth/signup", null, "{\"handle\":\"auth.tester\",\"password\":\"password1\",\"name\":\"다른\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HANDLE_DUPLICATE"));

        //비밀번호가 틀리면 실패
        postJson("/api/v1/auth/login", null, "{\"handle\":\"auth.tester\",\"password\":\"wrong-pass\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));

        final JsonNode token = json(postJson("/api/v1/auth/login", null, "{\"handle\":\"auth.tester\",\"password\":\"password1\"}")
                .andExpect(status().isOk()));
        assertThat(token.get("tokenType").asString()).isEqualTo("Bearer");

        //리프레시 토큰은 액세스 토큰으로 쓸 수 없다
        get("/api/v1/questions/today", token.get("refreshToken").asString())
                .andExpect(status().isUnauthorized());

        postJson("/api/v1/auth/token/refresh", null, "{\"refreshToken\":\"" + token.get("refreshToken").asString() + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        //액세스 토큰은 리프레시에 쓸 수 없다
        postJson("/api/v1/auth/token/refresh", null, "{\"refreshToken\":\"" + token.get("accessToken").asString() + "\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 아이디_규칙에_맞지_않으면_가입할_수_없다() throws Exception {
        postJson("/api/v1/auth/signup", null, "{\"handle\":\"Upper-Case\",\"password\":\"password1\",\"name\":\"테스터\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        postJson("/api/v1/auth/signup", null, "{\"handle\":\"short.pw\",\"password\":\"1234567\",\"name\":\"테스터\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        get("/api/v1/users/me", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));
    }
}
