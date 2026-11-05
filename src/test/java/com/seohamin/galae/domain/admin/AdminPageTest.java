package com.seohamin.galae.domain.admin;

import com.seohamin.galae.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminPageTest extends ApiTestSupport {

    @Test
    void 관리자_페이지는_토큰_없이_열리고_API는_막혀_있다() throws Exception {
        get("/admin", null)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/index.html"));

        final String html = get("/admin/index.html", null)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("<title>갈래 관리자</title>");

        get("/api/v1/admin/questions", null)
                .andExpect(status().isUnauthorized());
    }
}
