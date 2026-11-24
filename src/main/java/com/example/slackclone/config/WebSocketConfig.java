package com.example.slackclone.config;

import com.example.slackclone.domain.UserRepository;
import com.example.slackclone.service.ChannelService;
import com.example.slackclone.service.ChatService;
import com.example.slackclone.web.StompAuthInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.time.Clock;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(ChatProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final ChannelService channelService;

    public WebSocketConfig(JwtDecoder jwtDecoder, ChannelService channelService) {
        this.jwtDecoder = jwtDecoder;
        this.channelService = channelService;
    }

    // 왜: 단일 서버 메모리 브로커면 충분하다(다중 서버 전파는 PRD에서 제외).
    // 어디: 보내는 곳 /app, 받는 곳 /topic, 개인 오류 /user/queue.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }

    // 왜: 모든 STOMP 프레임이 인증·참여자 검사를 거치게 입구에 건다.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthInterceptor(jwtDecoder, channelService));
    }

    // 왜: 길이 한도를 설정 파일에서 받아 service에 숫자로 넘긴다. service는 설정 클래스를 모른다.
    @Bean
    ChatService chatService(ChannelService channelService, UserRepository users, Clock clock, ChatProperties props) {
        return new ChatService(channelService, users, clock, props.maxLength());
    }
}
