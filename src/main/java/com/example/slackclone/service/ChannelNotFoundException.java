package com.example.slackclone.service;

/** 없는 채널을 가리켰으므로 예외로 알린다. */
public class ChannelNotFoundException extends RuntimeException {
    public ChannelNotFoundException(Long channelId) {
        super("채널을 찾을 수 없습니다: " + channelId);
    }
}
