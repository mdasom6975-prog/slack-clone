package com.example.slackclone.service;

import com.example.slackclone.domain.User;
import com.example.slackclone.domain.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    // 왜: 해시·토큰·시계를 주입받아 테스트에서 바꿀 수 있게 한다.
    // 어디: 실제 구현은 config 패키지에서 빈으로 만든다.
    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       TokenIssuer tokenIssuer, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    public User signup(String username, String rawPassword) {
        if (users.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }
        User user = new User(username, passwordEncoder.encode(rawPassword), clock.instant());
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 username으로 가입한 경우 DB 유니크 제약이 막는다.
            throw new DuplicateUsernameException(username);
        }
    }

    public String login(String username, String rawPassword) {
        User user = users.findByUsername(username)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return tokenIssuer.issue(user.getId(), user.getUsername());
    }
}
