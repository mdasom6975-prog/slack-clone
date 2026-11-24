package com.example.slackclone.web;

import com.example.slackclone.domain.Channel;
import com.example.slackclone.service.ChannelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    public record CreateChannelRequest(
            @NotBlank @Size(min = 1, max = 30) @Pattern(regexp = "[a-z0-9_-]+") String name) {
    }

    public record ChannelResponse(long id, String name) {
    }

    public record ChannelListItem(long id, String name, boolean joined) {
    }

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    // 왜: 사용자 id는 요청 본문이 아니라 검증된 JWT에서만 얻는다. 남의 이름으로 행동할 수 없다.
    // 어디: 토큰이 없으면 SecurityConfig가 401로 막는다.
    private static Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private static ChannelResponse toResponse(Channel c) {
        return new ChannelResponse(c.getId(), c.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateChannelRequest req) {
        return toResponse(channelService.create(req.name(), userId(jwt)));
    }

    @GetMapping
    public List<ChannelListItem> list(@AuthenticationPrincipal Jwt jwt) {
        return channelService.list(userId(jwt)).stream()
                .map(v -> new ChannelListItem(v.channel().getId(), v.channel().getName(), v.joined()))
                .toList();
    }

    @PostMapping("/{id}/join")
    public ChannelResponse join(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return toResponse(channelService.join(id, userId(jwt)));
    }

    @GetMapping("/{id}")
    public ChannelResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return toResponse(channelService.requireMember(id, userId(jwt)));
    }
}
