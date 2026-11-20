package com.example.slackclone.service;

/** username 없음/비밀번호 불일치를 구분하지 않고 하나로 알린다(계정 존재 여부 노출 방지). */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("username 또는 비밀번호가 올바르지 않습니다.");
    }
}
