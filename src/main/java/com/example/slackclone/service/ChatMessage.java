package com.example.slackclone.service;

import java.time.Instant;

/** 채널에 실시간으로 전달되는 메시지. 보낸 사람은 인증된 사용자로 정한다. */
public record ChatMessage(long channelId, long senderId, String senderName, String content, Instant sentAt) {
}
