package com.example.slackclone.web;

import com.example.slackclone.service.ChatMessage;
import com.example.slackclone.service.ChatService;
import com.example.slackclone.service.InvalidMessageException;
import com.example.slackclone.service.NotChannelMemberException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {

    public record SendRequest(String content) {
    }

    private final ChatService chatService;
    private final SimpMessagingTemplate template;

    public ChatController(ChatService chatService, SimpMessagingTemplate template) {
        this.chatService = chatService;
        this.template = template;
    }

    // 왜: 규칙 검사는 service가 하고, 같은 채널 구독자에게 뿌리는 일만 web이 한다.
    // 어디: 보낸 사람 id는 CONNECT 때 검증된 Principal에서만 얻는다.
    @MessageMapping("/channels/{channelId}/send")
    public void send(@DestinationVariable long channelId, SendRequest req, Principal principal) {
        ChatMessage message = chatService.send(channelId, Long.parseLong(principal.getName()), req.content());
        template.convertAndSend("/topic/channels/" + channelId, message);
    }

    // 도메인 규칙 위반은 보낸 사람에게만 알려 준다 (/user/queue/errors).
    @MessageExceptionHandler({InvalidMessageException.class, NotChannelMemberException.class})
    @SendToUser("/queue/errors")
    public String handleError(RuntimeException e) {
        return e.getMessage();
    }
}
