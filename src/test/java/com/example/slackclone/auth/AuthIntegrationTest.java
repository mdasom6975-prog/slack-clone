package com.example.slackclone.auth;

import com.example.slackclone.domain.User;
import com.example.slackclone.domain.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private void signup(String username, String password, int expected) throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().is(expected));
    }

    private MvcResult login(String username, String password, int expected) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().is(expected)).andReturn();
    }

    // 증거: 가입 → 로그인 → 인증 필요한 API 호출
    @Test
    void 가입_로그인_인증API_호출() throws Exception {
        signup("alice", "password123", 201);
        MvcResult res = login("alice", "password123", 200);
        String token = JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    // 증거: 비밀번호는 해시 저장
    @Test
    void 비밀번호는_평문이_아닌_BCrypt_해시로_저장된다() throws Exception {
        signup("bob", "password123", 201);
        User bob = users.findByUsername("bob").orElseThrow();
        assertThat(bob.getPasswordHash()).isNotEqualTo("password123").startsWith("$2");
    }

    @Test
    void 토큰이_없거나_틀리면_401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 중복_username은_409() throws Exception {
        signup("carol", "password123", 201);
        signup("carol", "otherpass123", 409);
    }

    @Test
    void 틀린_비밀번호와_없는_사용자는_같은_401() throws Exception {
        signup("dave", "password123", 201);
        String wrong = login("dave", "wrong-password", 401).getResponse().getContentAsString();
        String none = login("nobody", "password123", 401).getResponse().getContentAsString();
        assertThat(wrong).isEqualTo(none);
    }

    @Test
    void 형식이_틀린_가입은_400() throws Exception {
        signup("ab", "password123", 400);
        signup("erin", "short", 400);
    }
}
