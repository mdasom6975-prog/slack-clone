package com.example.slackclone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "channel_members")
@IdClass(ChannelMemberId.class)
public class ChannelMember {

    @Id
    @Column(name = "channel_id")
    private Long channelId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected ChannelMember() {
    }

    public ChannelMember(Long channelId, Long userId, Instant joinedAt) {
        this.channelId = channelId;
        this.userId = userId;
        this.joinedAt = joinedAt;
    }

    public Long getChannelId() {
        return channelId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
