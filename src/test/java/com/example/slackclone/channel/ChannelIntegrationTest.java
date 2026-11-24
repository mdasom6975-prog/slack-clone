package com.example.slackclone.channel;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChannelIntegrationTest {

    @Autowired MockMvc mvc;

    // 가입 후 로그인해 Authorization 헤더 값을 돌려준다.
    private String tokenOf(String username) throws Exception {
        String body = "{\"username\":\"%s\",\"password\":\"password123\"}".formatted(username);
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");
    }

    private long createChannel(String token, String name) throws Exception {
        MvcResult res = mvc.perform(post("/api/channels").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
    }

    // 증거: 채널 생성 → 목록에 나옴, 만든 사람은 자동 참여
    @Test
    void 채널을_만들면_목록에_나오고_만든_사람은_참여_상태다() throws Exception {
        String alice = tokenOf("ch_alice");
        long id = createChannel(alice, "general-a");

        mvc.perform(get("/api/channels").header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==%d)].joined".formatted(id)).value(true));
        mvc.perform(get("/api/channels/" + id).header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("general-a"));
    }

    // 증거: 참여 전에는 거부, 참여 후에는 허용, 목록의 joined 표시가 바뀐다
    @Test
    void 미참여_채널_접근은_403이고_참여하면_허용된다() throws Exception {
        String alice = tokenOf("ch_alice2");
        String bob = tokenOf("ch_bob2");
        long id = createChannel(alice, "general-b");

        mvc.perform(get("/api/channels").header("Authorization", bob))
                .andExpect(jsonPath("$[?(@.id==%d)].joined".formatted(id)).value(false));
        mvc.perform(get("/api/channels/" + id).header("Authorization", bob))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/channels/" + id + "/join").header("Authorization", bob))
                .andExpect(status().isOk());

        mvc.perform(get("/api/channels/" + id).header("Authorization", bob))
                .andExpect(status().isOk());
        mvc.perform(get("/api/channels").header("Authorization", bob))
                .andExpect(jsonPath("$[?(@.id==%d)].joined".formatted(id)).value(true));
    }

    @Test
    void 이미_참여한_채널에_다시_참여해도_200이다() throws Exception {
        String alice = tokenOf("ch_alice3");
        long id = createChannel(alice, "general-c");
        mvc.perform(post("/api/channels/" + id + "/join").header("Authorization", alice))
                .andExpect(status().isOk());
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mvc.perform(get("/api/channels")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/channels").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/channels/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void 채널_이름이_중복되면_409() throws Exception {
        String alice = tokenOf("ch_alice4");
        createChannel(alice, "dup-name");
        mvc.perform(post("/api/channels").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"dup-name\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void 없는_채널은_404이고_형식이_틀린_이름은_400() throws Exception {
        String alice = tokenOf("ch_alice5");
        mvc.perform(post("/api/channels/999999/join").header("Authorization", alice))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/channels").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bad Name!\"}"))
                .andExpect(status().isBadRequest());
    }
}
