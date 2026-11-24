package com.example.slackclone.service;

/** 참여하지 않은 채널에 접근했으므로 예외로 알린다. */
public class NotChannelMemberException extends RuntimeException {
    public NotChannelMemberException(Long channelId) {
        super("참여하지 않은 채널입니다: " + channelId);
    }
}
