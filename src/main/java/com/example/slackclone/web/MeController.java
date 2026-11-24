package com.example.slackclone.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    public record MeResponse(long id, String username) {
    }

    // 왜: 인증이 필요한 첫 API. 토큰이 없거나 틀리면 SecurityConfig가 401로 막는다.
    // 어디: 이후 채널·메시지 API가 같은 방식으로 사용자를 얻는다.
    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(Long.parseLong(jwt.getSubject()), jwt.getClaimAsString("username"));
    }
}
