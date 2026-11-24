package com.example.slackclone.web;

import com.example.slackclone.service.DuplicateUsernameException;
import com.example.slackclone.service.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    // 왜: service는 HTTP 상태를 모르므로, 도메인 예외를 상태 코드로 바꾸는 일은 web이 맡는다.
    // 어디: 가입 중복 409, 로그인 실패 401, 입력 형식 오류 400.
    @ExceptionHandler(DuplicateUsernameException.class)
    ProblemDetail duplicate(DuplicateUsernameException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalid(InvalidCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail badRequest(MethodArgumentNotValidException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "입력 형식이 올바르지 않습니다.");
    }
}
