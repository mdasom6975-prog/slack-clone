package com.example.slackclone.domain;

import java.io.Serializable;

/** channel_members의 복합 키 (채널, 사용자). JPA 명세상 Serializable이 필요하다. */
public record ChannelMemberId(Long channelId, Long userId) implements Serializable {
}
