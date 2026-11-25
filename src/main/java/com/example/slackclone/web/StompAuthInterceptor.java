package com.example.slackclone.web;

import com.example.slackclone.service.ChannelService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** STOMP 프레임 입구. CONNECT에서 JWT를 검증하고, SUBSCRIBE·SEND에서 채널 참여자인지 검사한다. */
public class StompAuthInterceptor implements ChannelInterceptor {

    // 구독: /topic/channels/{id}, 전송: /app/channels/{id}/send
    private static final Pattern CHANNEL_DEST = Pattern.compile("^/(?:topic|app)/channels/(\\d+)(?:/send)?$");

    private final JwtDecoder jwtDecoder;
    private final ChannelService channelService;

    public StompAuthInterceptor(JwtDecoder jwtDecoder, ChannelService channelService) {
        this.jwtDecoder = jwtDecoder;
        this.channelService = channelService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();

        // 왜: 연결 때 한 번 토큰을 검증하고, 사용자 id를 세션에 붙여 둔다. 이후 프레임은 이 사용자로 본다.
        // 어디: 토큰이 없거나 틀리면 예외 → 클라이언트는 ERROR 프레임을 받고 연결이 끊긴다.
        if (StompCommand.CONNECT.equals(command)) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("인증 토큰이 필요합니다.");
            }
            try {
                Jwt jwt = jwtDecoder.decode(header.substring("Bearer ".length()));
                accessor.setUser(new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, List.of()));
            } catch (JwtException e) {
                throw new MessagingException("유효하지 않은 토큰입니다.");
            }
            return message;
        }

        // 왜: 참여자만 채널 메시지를 받고 보낼 수 있다. 비참여자의 구독을 여기서 막는다.
        // 어디: 채널 규칙은 ChannelService.requireMember 한 곳을 쓴다.
        if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            if (accessor.getUser() == null) {
                throw new MessagingException("인증되지 않은 연결입니다.");
            }
            String destination = accessor.getDestination();
            Matcher m = destination == null ? null : CHANNEL_DEST.matcher(destination);
            if (m == null || !m.matches()) {
                // 알 수 없는 경로(다른 사람의 개인 큐 등)는 열어 두지 않는다.
                if (StompCommand.SUBSCRIBE.equals(command) && !"/user/queue/errors".equals(destination)) {
                    throw new MessagingException("구독할 수 없는 경로입니다.");
                }
                return message;
            }
            long channelId = Long.parseLong(m.group(1));
            long userId = Long.parseLong(accessor.getUser().getName());
            try {
                channelService.requireMember(channelId, userId);
            } catch (RuntimeException e) {
                throw new MessagingException(e.getMessage());
            }
        }
        return message;
    }
}
