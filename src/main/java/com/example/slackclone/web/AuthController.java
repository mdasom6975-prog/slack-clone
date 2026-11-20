package com.example.slackclone.web;

import com.example.slackclone.domain.User;
import com.example.slackclone.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 왜: 입력 형식 검사는 web 경계에서 한다. BCrypt는 72바이트까지만 보므로 비밀번호 상한을 72로 둔다.
    // 어디: 비밀번호는 응답과 로그에 절대 싣지 않는다.
    public record SignupRequest(
            @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "[A-Za-z0-9_]+") String username,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record SignupResponse(long id, String username) {
    }

    public record TokenResponse(String accessToken) {
    }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest req) {
        User user = authService.signup(req.username(), req.password());
        return new SignupResponse(user.getId(), user.getUsername());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return new TokenResponse(authService.login(req.username(), req.password()));
    }
}
