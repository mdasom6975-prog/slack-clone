package com.example.slackclone.service;

/** 도메인 규칙 위반(이미 있는 채널 이름)이라 예외로 알린다. */
public class DuplicateChannelNameException extends RuntimeException {
    public DuplicateChannelNameException(String name) {
        super("이미 있는 채널 이름: " + name);
    }
}
