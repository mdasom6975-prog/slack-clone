package com.example.slackclone.realtime;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeIntegrationTest {

    @LocalServerPort int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private String post(String path, String token, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) b.header("Authorization", "Bearer " + token);
        HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isBetween(200, 201);
        return res.body();
    }

    private String tokenOf(String username) throws Exception {
        String body = "{\"username\":\"%s\",\"password\":\"password123\"}".formatted(username);
        post("/api/auth/signup", null, body);
        return JsonPath.read(post("/api/auth/login", null, body), "$.accessToken");
    }

    private long createChannel(String token, String name) throws Exception {
        return ((Number) JsonPath.read(post("/api/channels", token, "{\"name\":\"%s\"}".formatted(name)), "$.id")).longValue();
    }

    private WebSocketStompClient newClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        return client;
    }

    /** 토큰으로 연결한다. 연결 중 ERROR 프레임이 오면 errors에 담긴다. */
    private StompSession connect(String token, CompletableFuture<String> errors) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) connectHeaders.add("Authorization", "Bearer " + token);
        return newClient().connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        errors.complete(headers.getFirst("message"));
                    }

                    @Override
                    public void handleTransportError(StompSession s, Throwable ex) {
                        errors.complete("transport: " + ex.getMessage());
                    }
                }).get(5, TimeUnit.SECONDS);
    }

    private void subscribe(StompSession session, String dest, CompletableFuture<Map<String, Object>> into) {
        session.subscribe(dest, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                into.complete((Map<String, Object>) payload);
            }
        });
    }

    // 증거: 클라이언트 2개가 같은 채널에서 송수신한다
    @Test
    void 클라이언트_둘이_같은_채널에서_메시지를_주고받는다() throws Exception {
        String aliceToken = tokenOf("rt_alice");
        String bobToken = tokenOf("rt_bob");
        long channelId = createChannel(aliceToken, "rt-general");
        post("/api/channels/" + channelId + "/join", bobToken, "{}");

        StompSession alice = connect(aliceToken, new CompletableFuture<>());
        StompSession bob = connect(bobToken, new CompletableFuture<>());
        CompletableFuture<Map<String, Object>> aliceGot = new CompletableFuture<>();
        CompletableFuture<Map<String, Object>> bobGot = new CompletableFuture<>();
        subscribe(alice, "/topic/channels/" + channelId, aliceGot);
        subscribe(bob, "/topic/channels/" + channelId, bobGot);
        Thread.sleep(300); // 구독이 브로커에 등록될 시간

        alice.send("/app/channels/" + channelId + "/send", Map.of("content", "안녕하세요"));

        Map<String, Object> toBob = bobGot.get(5, TimeUnit.SECONDS);
        assertThat(toBob.get("content")).isEqualTo("안녕하세요");
        assertThat(toBob.get("senderName")).isEqualTo("rt_alice");
        assertThat(aliceGot.get(5, TimeUnit.SECONDS).get("content")).isEqualTo("안녕하세요");
        alice.disconnect();
        bob.disconnect();
    }

    // 증거: 비참여자는 수신하지 못한다 (구독이 거부되고 메시지도 오지 않는다)
    @Test
    void 비참여자는_구독할_수_없고_메시지를_받지_못한다() throws Exception {
        String aliceToken = tokenOf("rt_alice2");
        String eveToken = tokenOf("rt_eve2");
        long channelId = createChannel(aliceToken, "rt-private");

        StompSession alice = connect(aliceToken, new CompletableFuture<>());
        CompletableFuture<String> eveErrors = new CompletableFuture<>();
        StompSession eve = connect(eveToken, eveErrors);
        CompletableFuture<Map<String, Object>> eveGot = new CompletableFuture<>();
        subscribe(eve, "/topic/channels/" + channelId, eveGot);

        assertThat(eveErrors.get(5, TimeUnit.SECONDS)).contains("참여하지 않은 채널");

        alice.send("/app/channels/" + channelId + "/send", Map.of("content", "비밀 이야기"));
        assertThatThrownBy(() -> eveGot.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
        alice.disconnect();
    }

    @Test
    void 비참여자의_전송은_거부된다() throws Exception {
        String aliceToken = tokenOf("rt_alice3");
        String eveToken = tokenOf("rt_eve3");
        long channelId = createChannel(aliceToken, "rt-send-deny");

        StompSession alice = connect(aliceToken, new CompletableFuture<>());
        CompletableFuture<Map<String, Object>> aliceGot = new CompletableFuture<>();
        subscribe(alice, "/topic/channels/" + channelId, aliceGot);
        Thread.sleep(300);

        CompletableFuture<String> eveErrors = new CompletableFuture<>();
        StompSession eve = connect(eveToken, eveErrors);
        eve.send("/app/channels/" + channelId + "/send", Map.of("content", "끼어들기"));

        assertThat(eveErrors.get(5, TimeUnit.SECONDS)).contains("참여하지 않은 채널");
        assertThatThrownBy(() -> aliceGot.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
        alice.disconnect();
    }

    @Test
    void 토큰이_없거나_틀리면_연결이_거부된다() throws Exception {
        CompletableFuture<String> noToken = new CompletableFuture<>();
        try {
            connect(null, noToken);
        } catch (Exception expected) {
            noToken.complete("connect failed");
        }
        assertThat(noToken.get(5, TimeUnit.SECONDS)).isNotNull();

        CompletableFuture<String> badToken = new CompletableFuture<>();
        try {
            connect("not-a-token", badToken);
        } catch (Exception expected) {
            badToken.complete("connect failed");
        }
        assertThat(badToken.get(5, TimeUnit.SECONDS)).isNotNull();
    }
}
