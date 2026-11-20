package com.example.slackclone.service;

/** 도메인 규칙 위반(이미 있는 username)이라 예외로 알린다. */
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("이미 사용 중인 username: " + username);
    }
}
