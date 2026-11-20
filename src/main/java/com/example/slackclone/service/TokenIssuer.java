package com.example.slackclone.service;

/** 토큰 발급 인터페이스. 구현(JWT)은 config에 두어 service가 웹 기술을 모르게 한다. */
public interface TokenIssuer {
    String issue(long userId, String username);
}
