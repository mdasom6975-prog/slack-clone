package com.example.slackclone.domain;

/** channel_members의 복합 키 (채널, 사용자). */
public record ChannelMemberId(Long channelId, Long userId) {
}
