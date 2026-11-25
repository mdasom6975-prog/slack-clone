package com.example.slackclone.service;

import com.example.slackclone.domain.User;
import com.example.slackclone.domain.UserRepository;

import java.time.Clock;

/** 실시간 메시지 규칙. WebSocket을 모른다. 전달은 web이 맡는다. 저장은 5단계. */
public class ChatService {

    private final ChannelService channelService;
    private final UserRepository users;
    private final Clock clock;
    private final int maxLength;

    public ChatService(ChannelService channelService, UserRepository users, Clock clock, int maxLength) {
        this.channelService = channelService;
        this.users = users;
        this.clock = clock;
        this.maxLength = maxLength;
    }

    // 왜: 참여자만 보낼 수 있고, 보낸 사람은 클라이언트 입력이 아니라 인증된 사용자로 정한다.
    // 어디: 미참여면 NotChannelMemberException, 빈/과도한 길이면 InvalidMessageException.
    public ChatMessage send(long channelId, long userId, String content) {
        channelService.requireMember(channelId, userId);
        if (content == null || content.isBlank()) {
            throw new InvalidMessageException("메시지가 비어 있습니다.");
        }
        if (content.length() > maxLength) {
            throw new InvalidMessageException("메시지는 " + maxLength + "자까지 보낼 수 있습니다.");
        }
        User sender = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        return new ChatMessage(channelId, userId, sender.getUsername(), content, clock.instant());
    }
}
