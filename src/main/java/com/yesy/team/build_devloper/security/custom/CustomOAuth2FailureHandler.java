package com.yesy.team.build_devloper.security.custom;

import com.yesy.team.build_devloper.security.exception.EmailAlreadyExistsException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;

@Component
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // 예외 메시지를 추출
        String errorMessage;

        if (exception instanceof EmailAlreadyExistsException) {
            errorMessage = exception.getMessage();  // 이메일 중복 메시지 사용
        } else {
            errorMessage = "알 수 없는 이유로 로그인에 실패했습니다.";
        }

        // 오류 페이지로 리다이렉트 또는 알림
        response.sendRedirect("/api/member/login?error=true&message=" + URLEncoder.encode(errorMessage, "UTF-8"));
    }
}