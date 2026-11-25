package com.example.slackclone.service;

/** 빈 메시지이거나 너무 길어서 규칙을 어겼으므로 예외로 알린다. */
public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException(String reason) {
        super(reason);
    }
}
